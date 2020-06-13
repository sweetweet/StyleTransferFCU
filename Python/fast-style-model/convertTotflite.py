import tensorflow as tf

in_path = "pb/second_style.pb"
out_path = "pb/frozen_graph.tflite"
# out_path = "./model/quantize_frozen_graph.tflite"

# 模型输入节点
input_tensor_name = ["input_image"]
input_tensor_shape = {"input_image": [256, 256, 3]}
# 模型输出节点
classes_tensor_name = ["output_image"]
converter = tf.lite.TFLiteConverter.from_frozen_graph(in_path, input_tensor_name, classes_tensor_name,
                                                      input_shapes=input_tensor_shape)
print('==============================')
# converter.post_training_quantize = True
tflite_model = converter.convert()

with open(out_path, "wb") as f:
    f.write(tflite_model)
