import tensorflow as tf

# pb_path = 'pb/first_style.pb'
pb_path = 'pb/candy.pb'
content_file = 'img/test2.jpg'
generated_file = 'generated/pb_test3.jpg'

with tf.gfile.FastGFile(pb_path, 'rb') as f:
    graph_def = tf.GraphDef()
    graph_def.ParseFromString(f.read())
    input_image, input_height, input_width, output_image = tf.import_graph_def(graph_def,
                                                                               return_elements=['input_image:0',
                                                                                                'height:0',
                                                                                                'width:0',
                                                                                                'output_image:0'])

with tf.Session() as sess:
    sess.run(tf.global_variables_initializer())
    image_bytes = tf.read_file(content_file)
    if content_file.lower().endswith('png'):
        input_array, decoded_image = sess.run([
            tf.reshape(tf.image.decode_png(image_bytes, channels=3), [-1]),
            tf.image.decode_png(image_bytes, channels=3)])
    else:
        input_array, decoded_image = sess.run([
            tf.reshape(tf.image.decode_jpeg(image_bytes, channels=3), [-1]),
            tf.image.decode_jpeg(image_bytes, channels=3)])

    sess.run(output_image, feed_dict={
        input_image: input_array,
        input_height: decoded_image.shape[0],
        input_width: decoded_image.shape[1]})
    output_image = tf.cast(tf.convert_to_tensor(output_image), tf.uint8)
    output_image = tf.reshape(output_image, [input_height, input_width, 3])

    with open(generated_file, 'wb') as img:
        if generated_file.lower().endswith('png'):
            img.write(sess.run(tf.image.encode_png(tf.cast(output_image, tf.uint8)), feed_dict={
                input_image: input_array,
                input_height: decoded_image.shape[0],
                input_width: decoded_image.shape[1]}))
        else:
            img.write(sess.run(tf.image.encode_jpeg(tf.cast(output_image, tf.uint8)), feed_dict={
                input_image: input_array,
                input_height: decoded_image.shape[0],
                input_width: decoded_image.shape[1]}))
