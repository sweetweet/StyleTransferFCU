import tensorflow as tf
from tensorflow.python import pywrap_tensorflow


def check(input_checkpoint):
    tf.reset_default_graph()
    with tf.Session() as sess:
        saver = tf.train.import_meta_graph(input_checkpoint + '.meta')
        node_list = [n.name for n in tf.get_default_graph().as_graph_def().node]
        print(node_list)


def freeze_graph(input_checkpoint, output_graph):
    output_node_names = "output_image"  # 获取的节点
    saver = tf.train.import_meta_graph(input_checkpoint + '.meta', clear_devices=True)
    graph = tf.get_default_graph()  # 获得默认的图
    input_graph_def = graph.as_graph_def()  # 返回一个序列化的图代表当前的图

    with tf.Session() as sess:
        saver.restore(sess, input_checkpoint)  # 恢复图并得到数据
        output_graph_def = tf.graph_util.convert_variables_to_constants(  # 模型持久化，将变量值固定
            sess=sess,
            input_graph_def=input_graph_def,  # 等于:sess.graph_def
            output_node_names=output_node_names.split(","))  # 如果有多个输出节点，以逗号隔开

        with tf.gfile.GFile(output_graph, "wb") as f:  # 保存模型
            f.write(output_graph_def.SerializeToString())  # 序列化输出
        # print("%d ops in the final graph." % len(output_graph_def.node))  # 得到当前图有几个操作节点


if __name__ == '__main__':
    modelpath = "models/mosaic/fast-style-model.ckpt-done"
    # freeze_graph(modelpath, "frozen.pb")
    check(modelpath)
    print("finish!")
