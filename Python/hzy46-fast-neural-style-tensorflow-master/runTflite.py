# -*- coding:utf-8 -*-
import cv2
import time
import tensorflow as tf


def make_square(image, desired_size):
    # old_shape (height,width)
    old_shape = image.shape[:2]
    ratio = float(desired_size) / max(old_shape)
    # new_shape (height, width)
    new_shape = [int(i * ratio) for i in old_shape]
    # resize (width,height)
    image = cv2.resize(image, (new_shape[1], new_shape[0]))
    delta_w = desired_size - new_shape[1]
    delta_h = desired_size - new_shape[0]
    top, bottom = delta_h // 2, delta_h - (delta_h // 2)
    left, right = delta_w // 2, delta_w - (delta_w // 2)
    color = [0, 0, 0]
    return cv2.copyMakeBorder(image, top, bottom, left, right, cv2.BORDER_CONSTANT,
                              value=color), top, bottom, left, right, ratio


def main(img_path, model_path, out_path):
    # with tf.device('/cpu:0'):
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()
    input_details = interpreter.get_input_details()
    print(str(input_details))
    # get input_details shape
    input_details_height, input_details_width, _ = input_details[0]['shape']
    print(input_details_height, input_details_width)

    output_details = interpreter.get_output_details()
    print(str(output_details))

    model_interpreter_time = 0
    start_time = time.time()
    print('=========================')

    img = cv2.imread(img_path)
    print('img shape:', img.shape)
    # new_img with input_details shape
    new_image, top, bottom, left, right, ratio = make_square(img, input_details_height)
    print("new img size:", new_image.shape)
    # interpreter.resize_tensor_input(input_details[0]['index'], [img_height, img_width, img_channels])
    # interpreter.allocate_tensors()
    new_image = new_image.astype('float32')
    interpreter.set_tensor(input_details[0]['index'], new_image)

    model_interpreter_start_time = time.time()
    interpreter.invoke()
    # get output img
    output_data = interpreter.get_tensor(output_details[0]['index'])
    output_with_origion_size = cv2.resize(output_data[top:input_details_height - bottom,
                                          left:input_details_height - right],
                                          (img.shape[1], img.shape[0]))
    cv2.imwrite(out_path, output_with_origion_size)

    model_interpreter_time += time.time() - model_interpreter_start_time

    used_time = time.time() - start_time
    print('used_time:{}'.format(used_time))
    print('model_interpreter_time:{}'.format(model_interpreter_time))


if __name__ == '__main__':
    img_path = 'img/test2.jpg'
    model_path = "pb/frozen_graph.tflite"
    # model_path = "pb/frozen_graph_quan.tflite"
    out_path = 'img/generated_' + img_path[4:]
    main(img_path, model_path, out_path)
