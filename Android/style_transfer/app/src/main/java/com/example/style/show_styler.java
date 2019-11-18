package com.example.style;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.Toast;

import com.bm.library.Info;
import com.bm.library.PhotoView;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.image.TensorImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static android.widget.ImageView.ScaleType.FIT_CENTER;

public class show_styler extends AppCompatActivity {
    Bitmap content_bitmap;
    int flag = 1;
    public static final int CROP = 3;
    String imgPath;
    ImageView result_imgView;
    //    Bitmap content_bitmap = null;
    Bitmap resized_bitmap_withBorder = null;
    Bitmap result = null;
    Uri croppedUri;
    View mParent;
    //点击放大后的背景
    View mBg;
    View mBtnCrop;
    PhotoView mPhotoView;
    Info mInfo;
    AlphaAnimation in = new AlphaAnimation(0, 1);
    AlphaAnimation out = new AlphaAnimation(1, 0);

    private ByteBuffer imgData;
    private ByteBuffer controlData;


    private TensorFlowInferenceInterface inferenceInterface;
    private Interpreter tflite;

    private static final String MODEL_FILE_PATH = "file:///android_asset/a_frozen.pb";
    private static final String MODEL_FILE = "a_640_quan.tflite";
//    private static final String MODEL_FILE = "a_640.tflite";

    private static final String INPUT_NODE = "input_image";
    private static final String CONTROL_NODE = "style_control";
    private static final String OUTPUT_NODE = "output_image";
    private static final int NUM_STYLES = 15;
    public static final int INPUT_SCALE = 640;

    GpuDelegate gpuDelegate = null;
    NnApiDelegate nnapiDelegate = null;

    private int content_width;
    private int content_height;
    private int resized_width;
    private int resized_height;

    private long totalTime;
    public int style_number;

    private float[] style_control;
    private int[] input_intValues;
    private float[] input_floatValues;
    private float[] output_floatValues;
    private float[][][] output_3d_floatValues;
    private int[] output_intValues;

    DecimalFormat decimalFormat = new DecimalFormat(".00");

    private Bitmap.Config config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_styler);
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();
        result_imgView = findViewById(R.id.result_imgView);
        mBtnCrop = findViewById(R.id.btn_crop);
        Intent getImage = getIntent();
        if (getImage != null) {
            Bundle bd = getImage.getExtras();
            assert bd != null;

            style_number = bd.getInt("style_number");
//            flag = bd.getInt("model_mode");
            imgPath = bd.getString("imgPath");

            Bitmap smallBitmap = getSmallBitmap(imgPath);
            assert smallBitmap != null;
            config = smallBitmap.getConfig();
            Bitmap blurBitmap = FastBlurUtil.toBlur(smallBitmap, 10);
            result_imgView.setScaleType(FIT_CENTER);
            result_imgView.setImageBitmap(blurBitmap);
            result_imgView.setClickable(false);

            new Thread() {
                @Override
                public void run() {
//                    Bitmap smallBitmap = getSmallBitmap(imgPath);
//                    assert smallBitmap != null;
//                    Log.i("113","width:" +smallBitmap.getWidth()+"height: "+ smallBitmap.getHeight());
//                    config = smallBitmap.getConfig();
//
//                    Bitmap blurBitmap = FastBlurUtil.toBlur(smallBitmap, 10);
                    loadModel();
                    show_styler.this.runOnUiThread(() -> {
//                        result_imgView.setImageBitmap(smallBitmap);//显示到ImageView上
//                        result_imgView.setScaleType(FIT_CENTER);
//                        result_imgView.setImageBitmap(blurBitmap);
//                        result_imgView.setClickable(false);

                        Toast.makeText(show_styler.this, "style_number:" + style_number, Toast.LENGTH_SHORT).show();
                    });

                    content_bitmap = BitmapFactory.decodeFile(imgPath);

                    final long startTime = SystemClock.uptimeMillis();
                    try {
                        result = style_image(content_bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    totalTime = SystemClock.uptimeMillis() - startTime;
                    String time = decimalFormat.format((float)totalTime / 1000.0);

                    show_styler.this.runOnUiThread(() -> {
                        result_imgView.setImageBitmap(result);
                        result_imgView.setClickable(true);
                        Toast.makeText(show_styler.this, "totoal time: " + time + "s", Toast.LENGTH_SHORT).show();
                    });
                }
            }.start();
        }

        in.setDuration(300);
        out.setDuration(300);
        out.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBg.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mParent = findViewById(R.id.result_parent);
        mBg = findViewById(R.id.result_bg);
        mPhotoView =  findViewById(R.id.result_img);
        mPhotoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    private void loadModel() {
        try {
            if (flag == 0) {
                Interpreter.Options tfliteOptions = new Interpreter.Options();
                gpuDelegate = new GpuDelegate();
                nnapiDelegate = new NnApiDelegate();
//                tfliteOptions.addDelegate(gpuDelegate);
                tfliteOptions.addDelegate(nnapiDelegate);
                tflite = new Interpreter(loadModelFile(this), tfliteOptions);
                imgData = ByteBuffer.allocateDirect(INPUT_SCALE * INPUT_SCALE * 3 * 4);
                imgData.order(ByteOrder.nativeOrder());
                controlData = ByteBuffer.allocateDirect(NUM_STYLES * 4);
                controlData.order(ByteOrder.nativeOrder());
            } else if (flag == 1) {
                inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE_PATH);
//                final Graph g = inferenceInterface.graph();
//                final Operation inputOp = g.operation(INPUT_NODE);
//                final Operation styleOp = g.operation(CONTROL_NODE);
//                final Operation outputOp = g.operation(OUTPUT_NODE);
//                if (inputOp == null) {
//                    throw new RuntimeException("Failed to find input Node '" + INPUT_NODE + "'");
//                }
//                if (styleOp == null) {
//                    throw new RuntimeException("Failed to find input Node '" + CONTROL_NODE + "'");
//                }
//                if (outputOp == null) {
//                    throw new RuntimeException("Failed to find input Node '" + OUTPUT_NODE + "'");
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("111", "load model");
    }

    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /*缩小图片，并加黑色底填充到 INPUT_SCALE大小*/
    private Bitmap image_preProcessing(Bitmap bitmap) {

        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        content_height = height;
        content_width = width;

        Log.i("image_preProcessing", "content_height: " + content_height + " content_width: " + content_width);

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
        resized_width = new_width;
        resized_height = new_height;
        Log.i("image_preProcessing", "resized_height: " + new_height + " resized_width: " + new_width);
        Bitmap resized_image = Bitmap.createScaledBitmap(bitmap, new_width, new_height, true);

        Bitmap bmpWithBorder = Bitmap.createBitmap(INPUT_SCALE, INPUT_SCALE, config);

        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(resized_image, 0, 0, null);
        return bmpWithBorder;
    }

    /*输出模型的图片转成原大小*/
    private Bitmap image_reverse(Bitmap bitmap) {
        Log.i("image reverse", "intput bitmap Width: " + bitmap.getWidth() + " intput bitmap Height: " + bitmap.getHeight());

        Bitmap cropped_image = Bitmap.createBitmap(bitmap, 0, 0, resized_width, resized_height, null, false);
        Matrix matrix = new Matrix();
        float ratio = (float) content_height / resized_height;
        matrix.preScale(ratio, ratio);
        Bitmap resized_bitmap = Bitmap.createBitmap(cropped_image, 0, 0, resized_width, resized_height, matrix, true);
        Log.i("image reverse", "resized_bitmap_height: " + resized_bitmap.getWidth() + " resized_bitmap_width: " + resized_bitmap.getHeight());
        return resized_bitmap;
    }

    /*设置Style control array*/
    private void set_style_control() {
        style_control = new float[NUM_STYLES];
        Arrays.fill(style_control, 0);
        style_control[style_number - 1] = 1;
        Log.i("set_style_control", Arrays.toString(style_control));
    }

    /*bitmap转成可以输入模型的参数
     * load model*/
    private Bitmap prepare(Bitmap bitmap) throws IOException {

        set_style_control();
        input_intValues = new int[INPUT_SCALE * INPUT_SCALE];
        input_floatValues = new float[INPUT_SCALE * INPUT_SCALE * 3];

        output_intValues = new int[INPUT_SCALE * INPUT_SCALE];
        output_floatValues = new float[INPUT_SCALE * INPUT_SCALE * 3];
        output_3d_floatValues = new float[INPUT_SCALE][INPUT_SCALE][3];
        resized_bitmap_withBorder = image_preProcessing(bitmap);
        return resized_bitmap_withBorder;
    }


    /*输入模型*/
    private Bitmap style_image(Bitmap bitmap) throws IOException {
        Bitmap processed_bitmap = prepare(bitmap);
        processed_bitmap.getPixels(input_intValues, 0, INPUT_SCALE, 0, 0, INPUT_SCALE, processed_bitmap.getHeight());

        for (int i = 0; i < input_intValues.length; ++i) {
            final int val = input_intValues[i];
            input_floatValues[i * 3] = ((val >> 16) & 0xFF) / 255.0f;
            input_floatValues[i * 3 + 1] = ((val >> 8) & 0xFF) / 255.0f;
            input_floatValues[i * 3 + 2] = (val & 0xFF) / 255.0f;
        }

        Log.i("style_image", "Width: " + processed_bitmap.getWidth() + ", Height: " + processed_bitmap.getHeight());

        try {
            if (flag == 1) {
                inferenceInterface.feed(CONTROL_NODE, style_control, NUM_STYLES);
                inferenceInterface.feed(INPUT_NODE, input_floatValues, processed_bitmap.getWidth(), processed_bitmap.getHeight(), 3);

                inferenceInterface.run(new String[]{OUTPUT_NODE});
                inferenceInterface.fetch(OUTPUT_NODE, output_floatValues);
                for (int i = 0; i < output_intValues.length; ++i) {
                    output_intValues[i] =
                            0xFF000000
                                    | (((int) (output_floatValues[i * 3])) << 16)
                                    | (((int) (output_floatValues[i * 3 + 1])) << 8)
                                    | ((int) (output_floatValues[i * 3 + 2]));
                }

                Log.i("111", "RGB: " + output_floatValues[0] + " "
                        + output_floatValues[1] + " "
                        + output_floatValues[2]);
            } else {
                imgData.rewind();
                for (int i = 0; i < INPUT_SCALE; ++i) {
                    for (int j = 0; j < INPUT_SCALE; ++j) {
                        int pixelValue = input_intValues[i * INPUT_SCALE + j];
                        // Quantized model
                        imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                        imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                        imgData.put((byte) (pixelValue & 0xFF));
                    }
                }

                controlData.rewind();
                for (int i = 0; i < NUM_STYLES; i++) {
                    controlData.put((byte) style_control[i]);
                }
//                tflite.run(imgData,output_floatValues);
//                ByteBuffer[] inputs = {imgData, controlData};
                Object[] inputs = {input_floatValues, style_control};

                Log.i("1111", "style_control: " + Arrays.toString(style_control));
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(0, output_3d_floatValues);
                tflite.runForMultipleInputsOutputs(inputs, outputs);

                for (int i = 0; i < output_3d_floatValues.length; ++i) {
                    for (int j = 0; j < output_3d_floatValues[0].length; ++j) {
                        output_intValues[i * output_3d_floatValues.length + j] =
                                0xFF000000
                                        | (((int) (output_3d_floatValues[i][j][0])) << 16)
                                        | (((int) (output_3d_floatValues[i][j][1])) << 8)
                                        | ((int) (output_3d_floatValues[i][j][2]));
                    }
                }
            }
            Log.i("style_image 3d", "output_intValues" + output_intValues[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
        processed_bitmap.setPixels(output_intValues, 0, INPUT_SCALE, 0, 0, INPUT_SCALE, INPUT_SCALE);
//        Toast.makeText(this, "Total time: "+totalTime +"ms", Toast.LENGTH_SHORT).show();
        return image_reverse(processed_bitmap);
    }


    public static Bitmap getSmallBitmap(String filePath) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//只解析图片边沿，获取宽高
        BitmapFactory.decodeFile(filePath, options);

        int originalWidth = options.outWidth;
        int originalHeight = options.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        //图片分辨率以480x800为标准
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (originalWidth > originalHeight && originalWidth > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (originalHeight / hh);
        }
        if (be <= 0)
            be = 1;

        options.inSampleSize = be;
        // 完整解析图片返回bitmap
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(filePath, options);
    }

    public void clickImg(View view) {
        ImageView imgView = (ImageView) view;
        mInfo = PhotoView.getImageViewInfo(imgView);

        mPhotoView.setImageDrawable(imgView.getDrawable());

        mBg.startAnimation(in);
        mBg.setVisibility(View.VISIBLE);
        mParent.setVisibility(View.VISIBLE);
        mBtnCrop.setVisibility(View.VISIBLE);
        mPhotoView.animaFrom(mInfo);

        mPhotoView.enable();
        mPhotoView.setOnClickListener(v -> {
            mBg.startAnimation(out);
            mBtnCrop.setVisibility(View.INVISIBLE);
            mPhotoView.animaTo(mInfo, () -> mParent.setVisibility(View.GONE));
        });
    }
    @Override
    public void onBackPressed() {
        if (mParent.getVisibility() == View.VISIBLE) {
            mBg.startAnimation(out);
            mBtnCrop.setVisibility(View.INVISIBLE);
            mPhotoView.animaTo(mInfo, () -> mParent.setVisibility(View.GONE));
        } else {
            super.onBackPressed();
        }
    }

    public void cropImage(View view) {
        Log.i("111","crop image");
        croppedUri = null;
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        String fff = "file://" + "/" + Environment.getExternalStorageDirectory() + "/Styler/"; //设置图片名称
        File fullFile = new File(fff, imageFileName + ".jpg"); //将图片路径转换成uri
        croppedUri = Uri.parse(fullFile.toString());

        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        cropIntent.setDataAndType(Uri.parse(imgPath), "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 0.1);
        cropIntent.putExtra("aspectY", 0.1);
        cropIntent.putExtra("scale", true);
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, croppedUri);
        cropIntent.putExtra("return-data", false);
        cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        cropIntent.putExtra("noFaceDetection", true); // no face detection

        startActivityForResult(cropIntent, CROP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case CROP:
                if (croppedUri == null) {//如果没有选取照片，则直接返回
                    return;
                }
                mParent.setVisibility(View.GONE);
                Log.i("113", "cropped image uri: " + croppedUri);
                if (resultCode == RESULT_OK) {
                    imgPath = MainActivity.getRealFilePath(this, croppedUri);
                    result =  BitmapFactory.decodeFile(imgPath);
                    result_imgView.setImageBitmap(result);
                    result_imgView.setClickable(true);

                }
                break;
            default:
                break;
        }
    }
}
