package com.example.style;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class show_styler extends AppCompatActivity {
    Uri mImageUri;
    ImageView result_imgView;
    Bitmap content_bitmap = null;
    Bitmap resized_bitmap_withBorder = null;
    //    1 inferenceInterface 0 tflite
    int flag = 0;

    private TensorFlowInferenceInterface inferenceInterface;
    private Interpreter tflite;

    //    private static final String MODEL_FILE_PATH = "file:///android_asset/model.tflite";
//    private static final String MODEL_FILE_PATH = "file:///android_asset/saved_model.pb";
//    private static final String MODEL_FILE_PATH = "file:///android_asset/models_640.tflite";
//    private static final String MODEL_FILE_PATH = "file:///android_asset/freezed_tflite_640_tf13.tflite";
    private static final String MODEL_FILE_PATH = "file:///android_asset/freezed_tflite_640_lll.tflite";

    //    private static final String MODEL_FILE="model.tflite";
//private static final String MODEL_FILE="frozen_graph.tflite";
//    private static final String MODEL_FILE = "models_640.tflite";
    private static final String MODEL_FILE = "models_640_edited_no_sub_quan.tflite";

    private static final String INPUT_NODE = "input_image:0";
    private static final String CONTROL_NODE = "style_control:0";
    private static final String OUTPUT_NODE = "output_image:0";
    private static final int NUM_STYLES = 15;
    public static final int INPUT_SCALE = 640;
    public int style_number;

    private float[] style_control;
    private int[] input_intValues;
    private float[] input_floatValues;
    private int[][][] output_intValues;
    private float[] output_floatValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_styler);
        result_imgView = findViewById(R.id.result_imgView);
        if(flag==1)
            inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_PATH);
        Intent getImage = getIntent();

        if (getImage != null) {
            //获取intent传递过来的uri数据
            Bundle bd = getImage.getExtras();
            assert bd != null;
            style_number = bd.getInt("style_number");
            Log.i("111", "style_number:" + style_number);

            mImageUri = getImage.getData();
            if (mImageUri != null) {
                try {
                    content_bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mImageUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                result_imgView.setImageBitmap(content_bitmap);//显示到ImageView上
            }
        }

        try {
            process_image(content_bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private Bitmap image_preProcessing(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();
        int new_height;
        int new_width;

        if (height > width) {
            new_height = INPUT_SCALE;
            new_width = (width * new_height / height);
        } else if (height < width) {
            new_width = INPUT_SCALE;
            new_height = (height * new_width / width);
        } else {
            new_width = INPUT_SCALE;
            new_height = INPUT_SCALE;
        }
        Log.i("111", "resized_height: " + new_height + " resized_width: " + new_width);
        Bitmap resized_image = Bitmap.createScaledBitmap(bitmap, new_width, new_height, true);

        Bitmap bmpWithBorder = Bitmap.createBitmap(INPUT_SCALE, INPUT_SCALE, resized_image.getConfig());

        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(resized_image, 0, 0, null);
        return bmpWithBorder;
    }

    private void set_style_control() {
        style_control = new float[NUM_STYLES];
        Arrays.fill(style_control, 0);
        style_control[style_number - 1] = 1;
        Log.i("111", Arrays.toString(style_control));
    }

    private void process_image(Bitmap bitmap) throws IOException {
        try {
            if (flag == 0) {
                Interpreter.Options tfliteOptions = new Interpreter.Options();
//                .addDelegate(new GpuDelegate() {
//                    @Override
//                    public long getNativeHandle() {
//                        return 0;
//                    }
//                });
                tflite = new Interpreter(loadModelFile(this), tfliteOptions);
            }
        } catch (Exception e) {

            e.printStackTrace();
        }

        Log.i("111", "load model");

        set_style_control();
        input_intValues = new int[INPUT_SCALE * INPUT_SCALE];
        input_floatValues = new float[INPUT_SCALE * INPUT_SCALE * 3];
//        input_floatValues = new float[INPUT_SCALE * INPUT_SCALE * 3+15];
        output_floatValues = new float[INPUT_SCALE * INPUT_SCALE * 3];

        resized_bitmap_withBorder = image_preProcessing(bitmap);
        Runnable r = () -> {
            try {
                style_image(resized_bitmap_withBorder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        new Thread(r).start();
    }

    private void style_image(Bitmap bitmap) throws IOException {
        bitmap.getPixels(input_intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

//        int bytes = bitmap.getByteCount();
//        ByteBuffer imgData = ByteBuffer.allocate(bytes);
//        bitmap.copyPixelsToBuffer(imgData);

        for (int i = 0; i < input_intValues.length; ++i) {
            final int val = input_intValues[i];
            input_floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            input_floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            input_floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }
//        for (int i = 1; i <= NUM_STYLES; i++){
//            input_floatValues[INPUT_SCALE * INPUT_SCALE * 3+i]=style_control[i-1];
//        }
        Log.i("ddd", "Width: " + bitmap.getWidth() + ", Height: " + bitmap.getHeight());
        try {
            if (flag == 1) {
                inferenceInterface.feed(CONTROL_NODE, style_control,NUM_STYLES);
                inferenceInterface.feed(INPUT_NODE, input_floatValues, bitmap.getWidth(), bitmap.getHeight(), 3);

                inferenceInterface.run(new String[]{OUTPUT_NODE});
                inferenceInterface.fetch(OUTPUT_NODE, output_floatValues);
            } else {
                Object[] inputs = {input_floatValues, style_control};
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, output_floatValues);
                tflite.runForMultipleInputsOutputs(inputs, outputs);
            }
            Log.i("111", "finish" + output_floatValues.length);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
