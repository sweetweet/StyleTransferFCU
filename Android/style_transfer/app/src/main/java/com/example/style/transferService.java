package com.example.style;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class transferService extends Service {
    public static final String TAG = "transferService";
    public String imagePath;
    File imageFile;
    public Uri resultUri;
    public int styleNumber;
    public float quality;
    String log;
    long startTime;
    long totalTime;
    long a;

    private TensorFlowInferenceInterface inferenceInterface;
    private static final String MODEL_FILE_PATH = "file:///android_asset/a_frozen.pb";
    private static final String INPUT_NODE = "input_image";
    private static final String CONTROL_NODE = "style_control";
    private static final String OUTPUT_NODE = "output_image";
    private static final int NUM_STYLES = 15;
    public int INPUT_SCALE = 640;

    private int content_width;
    private int content_height;
    private int resized_width;
    private int resized_height;

    int new_height;
    int new_width;

    private float[] style_control;
    private int[] input_intValues;
    private float[] input_floatValues;
    private float[] output_floatValues;
    private int[] output_intValues;

    private IBinder mBinder = new MyBinder();
    DecimalFormat decimalFormat = new DecimalFormat(".00");


    @Override
    public void onCreate() {
        Log.i(TAG, "on create");
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "on bind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "on unbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "on destroy");
        super.onDestroy();
    }


    public class MyBinder extends Binder {
        transferService getService() {
            Log.i(TAG, "get service");
            return transferService.this;
        }
    }

    public void setImagePath(String path) {
        Log.i(TAG, path);
        this.imagePath = path;
    }

    public void setStyleNumber(int number) {
        Log.i(TAG, String.valueOf(number));
        this.styleNumber = number;
//        callback.onDataChange(resultUri);
        startTransform();
    }

    public void setQuality(float quality) {
        Log.i(TAG + ":quality", String.valueOf(quality));
        this.quality = quality;
        INPUT_SCALE = (int)(INPUT_SCALE * quality);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        stopSelf();
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Bundle bundle = intent.getExtras();
//        assert bundle != null;
//        imagePath = bundle.getString("imagePath");
//        styleNumber = bundle.getInt("styleNumber");
//        startTransform();
//        return super.onStartCommand(intent, flags, startId);
//    }

    public void startTransform() {
        new Thread(() -> {
            inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_PATH);
            Bitmap content = BitmapFactory.decodeFile(imagePath);
            int degree = MainActivity.readPictureDegree(imagePath);
            if (degree != 0)
                content = MainActivity.rotaingImageView(degree,content);
            set_style_control();
//            initial(INPUT_SCALE, INPUT_SCALE);
            startTime = SystemClock.uptimeMillis();
            Bitmap result = style_image(content);
            totalTime = SystemClock.uptimeMillis() - startTime;
            log += "\n total time: " + totalTime;
            Log.i(TAG, log);

            a = SystemClock.uptimeMillis();
            saveBitmapFile(result);
            log += "\n save time: " + (SystemClock.uptimeMillis() - a);
            Log.i(TAG, log);

            String time = decimalFormat.format((float) totalTime / 1000.0);
            Log.i(TAG, "total time:" + time + "s");
            Intent intent = new Intent();
            intent.putExtra("uri", resultUri.toString());
            intent.putExtra("path", imageFile.toString());
            intent.putExtra("time", time);
            intent.setAction("BROADCAST");//action与接收器相同

            startTime = SystemClock.uptimeMillis();
            sendBroadcast(intent);
            totalTime = SystemClock.uptimeMillis() - startTime;
            Log.i("time", "send broadcast time :" + decimalFormat.format((float) totalTime));
        }).start();

    }

    private void set_style_control() {
        style_control = new float[NUM_STYLES];
        Arrays.fill(style_control, 0);
        style_control[styleNumber - 1] = 1;
        Log.i("set_style_control", Arrays.toString(style_control));
    }

    private Bitmap style_image(Bitmap bitmap) {

        a = SystemClock.uptimeMillis();
        Bitmap processed_bitmap = image_preProcessing(bitmap);
        log += "\n processed_bitmap time: " + (SystemClock.uptimeMillis() - a);
        Log.i(TAG, log);

        a = SystemClock.uptimeMillis();
        processed_bitmap.getPixels(input_intValues, 0, INPUT_SCALE, 0, 0, INPUT_SCALE, processed_bitmap.getHeight());
        log += "\n processed_bitmap.getPixels time: " + (SystemClock.uptimeMillis() - a);
        Log.i(TAG, log);


        a = SystemClock.uptimeMillis();
        for (int i = 0; i < input_intValues.length; ++i) {
            final int val = input_intValues[i];
            input_floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            input_floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            input_floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        log += "\n get input_floatValues time: " + (SystemClock.uptimeMillis() - a);
        Log.i(TAG, log);
//        Log.i("style_image", "Width: " + processed_bitmap.getWidth() + ", Height: " + processed_bitmap.getHeight());

        try {

            a = SystemClock.uptimeMillis();
            inferenceInterface.feed(CONTROL_NODE, style_control, NUM_STYLES);
            inferenceInterface.feed(INPUT_NODE, input_floatValues, processed_bitmap.getWidth(), processed_bitmap.getHeight(), 3);
            log += "\n feed time: " + (SystemClock.uptimeMillis() - a);
            Log.i(TAG, log);

            a = SystemClock.uptimeMillis();
            inferenceInterface.run(new String[]{OUTPUT_NODE});
            log += "\n run time: " + (SystemClock.uptimeMillis() - a);
            Log.i(TAG, log);

            a = SystemClock.uptimeMillis();
            inferenceInterface.fetch(OUTPUT_NODE, output_floatValues);
            log += "\n fetch time: " + (SystemClock.uptimeMillis() - a);
            Log.i(TAG, log);

            a = SystemClock.uptimeMillis();
            for (int i = 0; i < output_intValues.length; ++i) {
                output_intValues[i] =
                        0xFF000000
                                | (((int) (output_floatValues[i * 3])) << 16)
                                | (((int) (output_floatValues[i * 3 + 1])) << 8)
                                | ((int) (output_floatValues[i * 3 + 2]));
            }
            log += "\n output_intValues time: " + (SystemClock.uptimeMillis() - a);
            Log.i(TAG, log);

//            Log.i("111", "RGB: " + output_floatValues[0] + " "
//                    + output_floatValues[1] + " "
//                    + output_floatValues[2]);
//
//            Log.i("style_image 3d", "output_intValues" + output_intValues[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }

        a = SystemClock.uptimeMillis();
        processed_bitmap.setPixels(output_intValues, 0, INPUT_SCALE, 0, 0, INPUT_SCALE, INPUT_SCALE);
        log += "\n processed_bitmap.setPixels time: " + (SystemClock.uptimeMillis() - a);
        Log.i(TAG, log);
//        Toast.makeText(this, "Total time: "+totalTime +"ms", Toast.LENGTH_SHORT).show();
        return image_reverse(processed_bitmap);
    }

    private Bitmap image_preProcessing(Bitmap bitmap) {

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

//        initial(INPUT_SCALE,INPUT_SCALE);

        content_height = height;
        content_width = width;

//        Log.i("image_preProcessing", "content_height: " + content_height + " content_width: " + content_width);
//
//        int new_height;
//        int new_width;

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
        resized_width = new_width;
        resized_height = new_height;

        initial(INPUT_SCALE, INPUT_SCALE);

        Log.i("image_preProcessing", "content_height: " + content_height + " content_width: " + content_width);
        Log.i("image_preProcessing", "resized_height: " + new_height + " resized_width: " + new_width);
        Bitmap resized_image = Bitmap.createScaledBitmap(bitmap, new_width, new_height, true);


        Bitmap bmpWithBorder = Bitmap.createBitmap(INPUT_SCALE, INPUT_SCALE, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(resized_image, 0, 0, null);

//        return resized_image;
        return bmpWithBorder;
    }

    private void initial(int w, int h) {
        input_intValues = new int[w * h];
        input_floatValues = new float[w * h * 3];

        output_intValues = new int[w * h];
        output_floatValues = new float[w * h * 3];
    }

    private Bitmap image_reverse(Bitmap bitmap) {
//        Log.i("image reverse", "intput bitmap Width: " + bitmap.getWidth() + " intput bitmap Height: " + bitmap.getHeight());

        a = SystemClock.uptimeMillis();
        Bitmap cropped_image = Bitmap.createBitmap(bitmap, 0, 0, resized_width, resized_height, null, false);
        Matrix matrix = new Matrix();
        float ratio = (float) content_height / (resized_height);
        matrix.preScale(ratio, ratio);
        Bitmap resized_bitmap = Bitmap.createBitmap(cropped_image, 0, 0, resized_width, resized_height, matrix, true);
//        Log.i("image reverse", "resized_bitmap_height: " + resized_bitmap.getWidth() + " resized_bitmap_width: " + resized_bitmap.getHeight());
        log += "\n image_reverse time: " + (SystemClock.uptimeMillis() - a);
        Log.i(TAG, log);

        return resized_bitmap;
    }

    private void saveBitmapFile(Bitmap bitmap) {
        imageFile = createImageFile();//创建用来保存照片的文件
        if (imageFile != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                /*7.0以上要通过FileProvider将File转化为Uri*/
                resultUri = FileProvider.getUriForFile(this, "com.example.style", imageFile);
            } else {
                /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                resultUri = Uri.fromFile(imageFile);
            }
            Log.i(TAG, "save bitmap result uri: " + resultUri);
        }
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(imageFile));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
            this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, resultUri));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "save successfully path: " + imageFile);
    }

    private File createImageFile() {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "RESULT_" + timeStamp + "_";
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = new File(Environment.getExternalStorageDirectory(), "Styler");
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        File imageFile = null;
        try {
            imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageFile;
    }
}
