import tensorflow as tf


def conv2d(x, input_filters, output_filters, kernel, strides, mode='REFLECT'):
    with tf.variable_scope('conv'):
        shape = [kernel, kernel, input_filters, output_filters]
        weight = tf.Variable(tf.truncated_normal(shape, stddev=0.1), name='weight')
        x_padded = tf.pad(x, [[0, 0], [int(kernel / 2), int(kernel / 2)], [int(kernel / 2), int(kernel / 2)], [0, 0]], mode=mode)
        return tf.nn.conv2d(x_padded, weight, strides=[1, strides, strides, 1], padding='VALID', name='conv')


def conv2d_transpose(x, input_filters, output_filters, kernel, strides):
    with tf.variable_scope('conv_transpose'):
        shape = [kernel, kernel, output_filters, input_filters]
        weight = tf.Variable(tf.truncated_normal(shape, stddev=0.1), name='weight')

        batch_size = tf.shape(x)[0]
        height = tf.shape(x)[1] * strides
        width = tf.shape(x)[2] * strides
        output_shape = tf.stack([batch_size, height, width, output_filters])
        return tf.nn.conv2d_transpose(x, weight, output_shape, strides=[1, strides, strides, 1], name='conv_transpose')


def resize_conv2d(x, input_filters, output_filters, kernel, strides, training):
    '''
    An alternative to transposed convolution where we first resize, then convolve.
    See http://distill.pub/2016/deconv-checkerboard/

    For some reason the shape needs to be statically known for gradient propagation
    through tf.image.resize_images, but we only know that for fixed image size, so we
    plumb through a "training" argument
    '''
    with tf.variable_scope('conv_transpose'):
        height = x.get_shape()[1].value if training else tf.shape(x)[1]
        width = x.get_shape()[2].value if training else tf.shape(x)[2]

        new_height = height * strides * 2
        new_width = width * strides * 2

        x_resized = tf.image.resize_images(x, [new_height, new_width], tf.image.ResizeMethod.NEAREST_NEIGHBOR)

        # shape = [kernel, kernel, input_filters, output_filters]
        # weight = tf.Variable(tf.truncated_normal(shape, stddev=0.1), name='weight')
        return conv2d(x_resized, input_filters, output_filters, kernel, strides)


def instance_norm(x):
    epsilon = 1e-9

    mean, var = tf.nn.moments(x, [1, 2], keep_dims=True)

    return tf.div(tf.subtract(x, mean), tf.sqrt(tf.add(var, epsilon)))


def batch_norm(x, size, training, decay=0.999):
    beta = tf.Variable(tf.zeros([size]), name='beta')
    scale = tf.Variable(tf.ones([size]), name='scale')
    pop_mean = tf.Variable(tf.zeros([size]))
    pop_var = tf.Variable(tf.ones([size]))
    epsilon = 1e-3

    batch_mean, batch_var = tf.nn.moments(x, [0, 1, 2])
    train_mean = tf.assign(pop_mean, pop_mean * decay + batch_mean * (1 - decay))
    train_var = tf.assign(pop_var, pop_var * decay + batch_var * (1 - decay))

    def batch_statistics():
        with tf.control_dependencies([train_mean, train_var]):
            return tf.nn.batch_normalization(x, batch_mean, batch_var, beta, scale, epsilon, name='batch_norm')

    def population_statistics():
        return tf.nn.batch_normalization(x, pop_mean, pop_var, beta, scale, epsilon, name='batch_norm')

    return tf.cond(training, batch_statistics, population_statistics)


def conditional_instance_norm(x, style_control=None):
    with tf.variable_scope('con_instance_norm'):
        batch, rows, cols, channels = [i.value for i in x.get_shape()]
        mean, var = tf.nn.moments(x, [1, 2], keep_dims=True)

        var_shape = [channels]
        # norm * gamma + beta -> z
        gamma = []
        beta = []

        for i in range(style_control.shape[0]):
            with tf.variable_scope('{0}'.format(i) + '_style'):
                beta.append(tf.get_variable('beta', shape=var_shape, initializer=tf.constant_initializer(0.)))
                gamma.append(tf.get_variable('gamma', shape=var_shape, initializer=tf.constant_initializer(1.)))
                tf.summary.histogram('beta', beta)
                tf.summary.histogram('gamma', gamma)
        gamma = tf.convert_to_tensor(gamma)
        beta = tf.convert_to_tensor(beta)
        epsilon = 1e-3
        normalized = tf.div(tf.subtract(x, mean), tf.sqrt(tf.add(var, epsilon)))

        # 得到选择style的index
        index = tf.where(tf.not_equal(style_control, tf.constant(0, dtype=tf.float32)))
        # style_control对映index的值
        style_value = tf.gather(style_control, index)
        # shift,scale 对映index的值
        beta_value = tf.gather(beta, index)
        gamma_value = tf.gather(gamma, index)
        # style_control的value和
        style_sum = tf.reduce_sum(style_control)
        #
        style_beta = tf.reduce_sum(beta_value * style_value, axis=0) / style_sum
        style_gamma = tf.reduce_sum(gamma_value * style_value, axis=0) / style_sum
        output = style_gamma * normalized + style_beta

        # for i, x in enumerate(style_control):
        #     if not x == 0:
        #         index = [i]
        # style_scale = reduce(tf.add, [scale[i]*style_control[i] for i in index]) / sum(style_control)
        # style_shift = reduce(tf.add, [shift[i]*style_control[i] for i in index]) / sum(style_control)
        # output = style_scale * normalized + style_shift
    return output


def relu(input):
    relu = tf.nn.relu(input)
    # convert nan to zero (nan != nan)
    nan_to_zero = tf.where(tf.equal(relu, relu), relu, tf.zeros_like(relu))
    return nan_to_zero


def residual(x, filters, kernel, strides, style_control):
    with tf.variable_scope('residual'):
        conv1 = conditional_instance_norm(conv2d(x, filters, filters, kernel, strides), style_control)
        conv2 = conditional_instance_norm(conv2d(relu(conv1), filters, filters, kernel, strides), style_control)
        residual = x + conv2
        return residual


def net(image, training, style_control):
    # Less border effects when padding a little before passing through ..
    image = tf.pad(image, [[0, 0], [10, 10], [10, 10], [0, 0]], mode='REFLECT')

    with tf.variable_scope('conv1'):
        conv1 = relu(conditional_instance_norm(conv2d(image, 3, 32, 9, 1), style_control))
    with tf.variable_scope('conv2'):
        conv2 = relu(conditional_instance_norm(conv2d(conv1, 32, 64, 3, 2), style_control))
    with tf.variable_scope('conv3'):
        conv3 = relu(conditional_instance_norm(conv2d(conv2, 64, 128, 3, 2), style_control))
    with tf.variable_scope('res1'):
        res1 = residual(conv3, 128, 3, 1, style_control)
    with tf.variable_scope('res2'):
        res2 = residual(res1, 128, 3, 1, style_control)
    with tf.variable_scope('res3'):
        res3 = residual(res2, 128, 3, 1, style_control)
    with tf.variable_scope('res4'):
        res4 = residual(res3, 128, 3, 1, style_control)
    with tf.variable_scope('res5'):
        res5 = residual(res4, 128, 3, 1, style_control)
    # print(res5.get_shape())
    with tf.variable_scope('deconv1'):
        # deconv1 = relu(instance_norm(conv2d_transpose(res5, 128, 64, 3, 2)))
        deconv1 = relu(conditional_instance_norm(resize_conv2d(res5, 128, 64, 3, 2, training), style_control))
    with tf.variable_scope('deconv2'):
        # deconv2 = relu(instance_norm(conv2d_transpose(deconv1, 64, 32, 3, 2)))
        deconv2 = relu(conditional_instance_norm(resize_conv2d(deconv1, 64, 32, 3, 2, training), style_control))
    with tf.variable_scope('deconv3'):
        # deconv_test = relu(instance_norm(conv2d(deconv2, 32, 32, 2, 1)))
        deconv3 = tf.nn.tanh(conditional_instance_norm(conv2d(deconv2, 32, 3, 9, 1), style_control))

    y = (deconv3 + 1) * 127.5

    # Remove border effect reducing padding.
    height = tf.shape(y)[1]
    width = tf.shape(y)[2]
    y = tf.slice(y, [0, 10, 10, 0], tf.stack([-1, height - 20, width - 20, -1]))

    return y
