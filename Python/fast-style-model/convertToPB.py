# coding: utf-8
import utils
import os
import tensorflow as tf
import argparse
import model

os.chdir('.')


def parse_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('-m', '--model_file', help='the path to the model file')
    parser.add_argument('-n', '--model_name', default='transfer', help='the name of the model')
    return parser.parse_args()


def main(args):
    with tf.Graph().as_default():
        with tf.Session() as sess:
            image_data = tf.placeholder(tf.float32, name='input_image')
            processed_image = utils.mean_image_subtraction(image_data, [123.68, 116.779, 103.939])
            # Add batch dimension
            batched_image = tf.expand_dims(processed_image, 0)
            generated_image = model.net(batched_image, training=False)
            output_image = tf.squeeze(generated_image, [0], name='output_image')

            saver = tf.train.Saver(tf.global_variables(), write_version=tf.train.SaverDef.V1)
            sess.run([tf.global_variables_initializer(), tf.local_variables_initializer()])

            # converter = tf.contrib.lite.TFLiteConverter.from_session(sess, image_data.eval(), output_image)
            # tflite_model = converter.convert()
            # open("converted_model.tflite", "wb").write(tflite_model)

            # Use absolute path.
            model_file = os.path.abspath(args.model_file)
            saver.restore(sess, model_file)

            output_graph_def = tf.graph_util.convert_variables_to_constants(
                    sess, sess.graph_def, output_node_names=['output_image'])

            if os.path.exists('pb') is False:
                os.makedirs('pb')
            with tf.gfile.FastGFile('pb/' + args.model_name + '.pb', mode='wb') as f:
                f.write(output_graph_def.SerializeToString())

            tf.summary.FileWriter('log/',  tf.get_default_graph())


if __name__ == '__main__':
    tf.logging.set_verbosity(tf.logging.INFO)
    input_args = parse_args()
    main(input_args)

# graph = tf.get_default_graph()
# graph_def = graph.as_graph_def()
# graph_def.ParseFromString(tf.gfile.FastGFile(model, 'rb').read())
# tf.import_graph_def(graph_def, name='graph')
# summaryWriter = tf.summary.FileWriter('log/', graph)
# with tf.Session() as sess:
#     # with tf.gfile.FastGFile('pb/first_style.pb', 'rb') as f:
#     #     graph_def = tf.GraphDef()
#     #     graph_def.ParseFromString(f.read())
#     #     sess.graph.as_default()
#     #     tf.import_graph_def(graph_def, name='')  # 导入计算图
#     with open(model, 'rb') as model_file:
#         graph_def = tf.GraphDef()
#         graph_def.ParseFromString(model_file.read())
#         print(graph_def)

# 需要有一个初始化的过程
# sess.run(tf.global_variables_initializer())

# 需要先复原变量
# print(sess.run('b:0'))
# # 1
# content_file = 'img/test4.jpg'
# generated_file = 'generated/6_29.jpg'
#
# with open(generated_file, 'wb') as img:
#     image_bytes = tf.read_file(content_file)
#     input_array, decoded_image = sess.run([
#         tf.reshape(tf.image.decode_jpeg(image_bytes, channels=3), [-1]),
#         tf.image.decode_jpeg(image_bytes, channels=3)])
#
#     start_time = time.time()
#     img.write(sess.run(tf.image.encode_jpeg(tf.cast(cropped_image, tf.uint8)), feed_dict={
#         image_data: input_array,
#         height: decoded_image.shape[0],
#         width: decoded_image.shape[1]}))
#     end_time = time.time()
#
#     tf.logging.info('Elapsed time: %fs' % (end_time - start_time))
# # 输入
# input_image = sess.graph.get_tensor_by_name('input_image:0')
# input_height = sess.graph.get_tensor_by_name('height:0')
# input_width = sess.graph.get_tensor_by_name('width:0')
#
# output_image = sess.graph.get_tensor_by_name('output_image:0')
#
# output = sess.run(output_image, feed_dict={input_image: image_path, input_height: 5, input_width: 3})

#
# path = '333.jpg'
# with tf.Session().as_default() as sess:
#     with open(path, 'rb') as img:
#         image = sess.run(tf.image.decode_jpeg(img.read()))
#         output = sess.run(tf.pad(image, [[10, 10], [10, 10], [0, 0]], mode='REFLECT'))
#
# with tf.Session() as sess:
#     height = tf.shape(output)[0]
#     width = tf.shape(output)[1]
#     y = tf.slice(output, [10, 10, 0], tf.stack([height - 20, width - 20, 3]))
#     print(sess.run(y))
#     generated_file = '16.jpg'
#     with open(generated_file, 'wb') as img:
#         img.write(sess.run(tf.image.encode_jpeg(y)))
