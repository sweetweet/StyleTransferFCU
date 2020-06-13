# coding: utf-8
import argparse

import tensorflow as tf


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-pb', '--pb_file', default='pb/second_style.pb', help='the path to the pb model file')
    parser.add_argument('-c', '--content_file', default='img/test2.jpg', help='the path of the content path')
    parser.add_argument('-g', '--generated_file', default='generated/6_233.jpg', help='the path of the generated path')
    return parser.parse_args()


def main(args):
    with tf.Session() as sess:
        sess.run(tf.global_variables_initializer())
        image_bytes = tf.read_file(args.content_file)
        if args.content_file.lower().endswith('png'):
            decoded_image = sess.run(tf.image.decode_png(image_bytes, channels=3))
        else:
            decoded_image = sess.run(tf.image.decode_jpeg(image_bytes, channels=3))

        with tf.gfile.FastGFile(args.pb_file, 'rb') as f:
            graph_def = tf.GraphDef()
            graph_def.ParseFromString(f.read())
            input_image, output_image = tf.import_graph_def(graph_def,
                                                            return_elements=['input_image:0', 'output_image:0'])

        with open(args.generated_file, 'wb') as img:
            if args.generated_file.lower().endswith('png'):
                img.write(sess.run(tf.image.encode_png(tf.cast(output_image, tf.uint8)), feed_dict={
                    input_image: decoded_image}))
            else:
                img.write(sess.run(tf.image.encode_jpeg(tf.cast(output_image, tf.uint8)), feed_dict={
                    input_image: decoded_image}))


if __name__ == '__main__':
    tf.logging.set_verbosity(tf.logging.INFO)
    input_args = parse_args()
    main(input_args)
