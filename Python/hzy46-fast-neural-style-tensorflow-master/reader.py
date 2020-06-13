from os import listdir
from os.path import isfile, join
import tensorflow as tf


def get_image(path, height, width, preprocess_fn):
    png = path.lower().endswith('png')
    img_bytes = tf.read_file(path)
    image = tf.image.decode_png(img_bytes, channels=3) if png else tf.image.decode_jpeg(img_bytes, channels=3)
    return preprocess_fn(image, height, width)


def image(batch_size, height, width, path, preprocess_fn, epochs=2, shuffle=True):
    # filenames把文件夹下的所有图片名称列出来
    # filenames的内容：['train2014/COCO_val2014_000000000042.jpg', 'train2014/COCO_val2014_000000000074.jpg']
    filenames = [join(path, f) for f in listdir(path) if isfile(join(path, f))]
    if not shuffle:
        filenames = sorted(filenames)
    # If first file is a png, assume they all are
    png = filenames[0].lower().endswith('png')

    # 把filenames都加入到filename_queue
    filename_queue = tf.train.string_input_producer(filenames, shuffle=shuffle, num_epochs=epochs)
    reader = tf.WholeFileReader()
    # 从filename_queue读取一张image
    _, img_bytes = reader.read(filename_queue)
    # 得到image的tensor
    image = tf.image.decode_png(img_bytes, channels=3) if png else tf.image.decode_jpeg(img_bytes, channels=3)
    # 处理图片，设置图片的loss_model和大小 preprocess_fn在train.py中定义
    processed_image = preprocess_fn(image, height, width)
    # 利用一个tensor(processed_image) 来返回一个batch的数据
    return tf.train.batch([processed_image], batch_size, dynamic_pad=True)
