package com.example.styler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
//
//import android.support.v4.content.FileProvider;
//import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int CAMERA = 1;
    public static final int ALBUM = 2;
    public static final int INPUT_SCALE = 720;

    private static final String MODEL_FILE_PATH = "file:///android_asset/mobilenet_v1_1.0_224_quant.tflite";
    //private static final String MODEL_FILE = "file:///android_asset/mobilenet_v1_1.0_224_quant.tflite";
//    private static final String MODEL_FILE = "frozen_graph.tflite";
    private static final String MODEL_FILE = "models_640.tflite";
    //    private static final String MODEL_FILE = "mobilenet_v1_1.0_224_quant.tflite";
    //private static final String MODEL_FILE = "file:///android_asset/second_style.pb";
    private static final String INPUT_NODE = "input_image";
    private static final String OUTPUT_NODE = "output_image";
    //
//    private int[] intValues = new int[INPUT_SCALE * INPUT_SCALE];
//    private float[] floatValues = new float[INPUT_SCALE * INPUT_SCALE * 3];
    public Bitmap image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageButton btn_camera = findViewById(R.id.btn_image_camera);
        btn_camera.setOnClickListener(this);
        ImageButton btn_album = findViewById(R.id.btn_image_local);
        btn_album.setOnClickListener(this);
        Button btn_transfer = findViewById(R.id.btn_transfer);
        btn_transfer.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_image_camera:
                openCamera();
                break;
            case R.id.btn_image_local:
                openAlbum();
                break;
            case R.id.btn_transfer:
                try {
                    transform();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    private Bitmap image_preProcessing() {
        int height = image.getHeight();
        int width = image.getWidth();
        int new_height;
        int new_width;

        if (height > width) {
            float ratio = (float) INPUT_SCALE / (float) height;
            new_height = INPUT_SCALE;
            new_width = (int) (width * ratio);
        } else if (height < width) {
            float ratio = (float) INPUT_SCALE / (float) width;
            new_width = INPUT_SCALE;
            new_height = (int) (height * ratio);
        } else {
            new_width = INPUT_SCALE;
            new_height = INPUT_SCALE;
        }
        Bitmap resized_image = Bitmap.createScaledBitmap(image, new_width, new_height, true);

        Bitmap bmpWithBorder = Bitmap.createBitmap(INPUT_SCALE, INPUT_SCALE, resized_image.getConfig());

        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(resized_image, (float) (INPUT_SCALE - new_width) / 2, (float) (INPUT_SCALE - new_height) / 2, null);
        return bmpWithBorder;
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void transform() throws IOException {

//    -------------------------------show image size    ----------------------------------------------
        String message = "height: " + image.getHeight() + " width：" + image.getWidth();
        Log.i("height", message);


        Bitmap resized_image = image_preProcessing();

        int[] intValues = new int[INPUT_SCALE * INPUT_SCALE];
        float[] floatValues = new float[INPUT_SCALE* INPUT_SCALE * 3];
        float[][][] output = new float[INPUT_SCALE][INPUT_SCALE][3];


//      '''''''''''''''''toast show resized image size''''''''''''''''''''''''''''''''''''''''''
        message = "!!!height: " + resized_image.getHeight() + " width：" + resized_image.getWidth();
        Log.i("height", message);


//      ''''''''''''''''''''''''using interpreter load tflite model'''''''''''''''''''''''''
        Interpreter tflite;
        Interpreter.Options tfliteOptions = new Interpreter.Options();
//                .addDelegate(new GpuDelegate());
        tflite = new Interpreter(loadModelFile(this), tfliteOptions);
        Log.i("tflite", "load tflite");


        resized_image.getPixels(intValues, 0, resized_image.getWidth(), 0, 0, resized_image.getWidth(), resized_image.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        tflite.run(floatValues, output);
//      '''''''''''''''''''''''''''tflite run successfully''''''''''''''''''''''''''''''''''''''
        message = "run tflite successfully";
        Log.i("run success", message);

        TensorFlowInferenceInterface inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_PATH);
////        inferenceInterface.feed(INPUT_NODE, floatValues, 1, trans_image.getWidth(), trans_image.getHeight(), 3);
//        inferenceInterface.feed(INPUT_NODE, floatValues, 1, image.getWidth(), image.getHeight(), 3);
//
//        inferenceInterface.run(new String[]{OUTPUT_NODE});
//        inferenceInterface.fetch(OUTPUT_NODE, floatValues);

        for (int i = 0; i < intValues.length; ++i) {
            intValues[i] =
                    0xFF000000
                            | (((int) (floatValues[i * 3] * 255)) << 16)
                            | (((int) (floatValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (floatValues[i * 3 + 2] * 255));
        }

        resized_image.setPixels(intValues, 0, INPUT_SCALE, 0, 0, INPUT_SCALE, INPUT_SCALE);


        ImageView trans_imgView = findViewById(R.id.imageView_transform);
        trans_imgView.setImageBitmap(resized_image);
    }

    String currentPhotoPath;

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",   /* suffix */
                storageDir      /* directory */
        );

        currentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    public void openCamera() {
        Intent openCameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (openCameraIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.styler",
                        photoFile);
                openCameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(openCameraIntent, CAMERA);
            }
        }
    }

    public void openAlbum() {
        Intent openAlbumIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (openAlbumIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(openAlbumIntent, ALBUM);
        }
    }

    private void setPic() {
        ImageView imgView = findViewById(R.id.imageView);
        int targetW = imgView.getWidth();
        int targetH = imgView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;

        image = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        imgView.setImageBitmap(image);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ImageView imageView = findViewById(R.id.imageView);
        switch (requestCode) {
            case CAMERA:
                if (resultCode == RESULT_OK) {
                    setPic();
                }
                break;
            case ALBUM:
                if (resultCode == RESULT_OK) {
                    assert data != null;
                    Uri uri = data.getData();
                    try {
                        image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    imageView.setImageBitmap(image);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
