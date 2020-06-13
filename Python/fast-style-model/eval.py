# coding: utf-8
from __future__ import print_function
import tensorflow as tf
from preprocessing import preprocessing_factory
import reader
import model
import time
import os

tf.app.flags.DEFINE_string('loss_model', 'vgg_16', 'The name of the architecture to evaluate. '
                                                   'You can view all the support models in nets/nets_factory.py')
tf.app.flags.DEFINE_integer('image_size', 256, 'Image size to train.')
tf.app.flags.DEFINE_string("model_file", "models.ckpt", "")
tf.app.flags.DEFINE_string("image_file", "a.jpg", "")

FLAGS = tf.app.flags.FLAGS


def main(_):
    # Get image's height and width.
    with open(FLAGS.image_file, 'rb') as img:
        with tf.Session().as_default() as sess:
            if FLAGS.image_file.lower().endswith('png'):
                image = sess.run(tf.image.decode_png(img.read()))
            else:
                image = sess.run(tf.image.decode_jpeg(img.read()))
            # resize 输入图片的大小
            # image = tf.image.resize_image_with_crop_or_pad(image, 400, 400)
            # height = image.shape[0]
            # width = image.shape[1]
            original_height = image.shape[0]
            original_width = image.shape[1]
            if image.shape[0] < image.shape[1]:
                rate = 460 / image.shape[1]
                width = 460
                height = int(round(image.shape[0] * rate))
            else:
                rate = 450 / image.shape[0]
                height = 450
                width = int(round(image.shape[1] * rate))

    tf.logging.info('Image size: %dx%d' % (width, height))

    with tf.Graph().as_default():
        with tf.Session().as_default() as sess:
            # Read image data.
            # 传入model name 以及是否训练 返回model预处理图片的function

            image_preprocessing_fn, _ = preprocessing_factory.get_preprocessing(
                FLAGS.loss_model,
                is_training=False)
            # get_image呼叫了image_preprocessing_fn预处理图片，高度宽度在上一个with已经处理好
            image = reader.get_image(FLAGS.image_file, height, width, image_preprocessing_fn)

            # Add batch dimension => (batch_size, width, height, color_depth)
            #                image = (1, 400, 400 ,3)
            image = tf.expand_dims(image, 0)
            generated = model.net(image, training=False)
            generated = tf.image.resize_bilinear(generated, [original_height, original_width], align_corners=False)
            generated = tf.cast(generated, tf.uint8)
            # Remove batch dimension
            generated = tf.squeeze(generated, [0])

            #
            saver = tf.train.Saver(tf.global_variables(), write_version=tf.train.SaverDef.V1)
            sess.run([tf.global_variables_initializer(), tf.local_variables_initializer()])
            # Use absolute path
            # FLAGS.model_file = os.path.abspath(FLAGS.model_file)
            saver.restore(sess, FLAGS.model_file)

            # Make sure 'generated' directory exists.\
            generated_file = 'generated/' + FLAGS.model_file[7: -11] + '_' + FLAGS.image_file[4:]

            if os.path.exists('generated') is False:
                os.makedirs('generated')

            # Generate and write image data to file.
            with open(generated_file, 'wb') as img:
                start_time = time.time()
                img.write(sess.run(tf.image.encode_jpeg(generated)))
                end_time = time.time()
                tf.logging.info('Elapsed time: %fs' % (end_time - start_time))

                tf.logging.info('Done. Please check %s' % generated_file)


if __name__ == '__main__':
    tf.logging.set_verbosity(tf.logging.INFO)
    tf.app.run()
