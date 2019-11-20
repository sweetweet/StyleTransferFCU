package com.example.style;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bm.library.Info;
import com.bm.library.PhotoView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    public static final int CAMERA = 1;
    public static final int ALBUM = 2;
    public static final int CROP = 3;
    private final String TAG = getClass().getSimpleName();
    ImageView content_imgView;
    Bitmap smallBitmap;
    Uri mImageUri;
    Uri croppedUri;
    Uri show;
    String imgPath;
    View mParent;
    //点击放大后的背景
    View mBg;
    View mBtnCrop;
    PhotoView mPhotoView;
    Info mInfo;

    AlphaAnimation in = new AlphaAnimation(0, 1);
    AlphaAnimation out = new AlphaAnimation(1, 0);

    private static int REQUEST_PERMISSION_CODE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();
        /*设定通知*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "result";
            String channelName = "转换完成通知";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            createNotificationChannel(channelId, channelName, importance);
        }

        content_imgView = findViewById(R.id.content_imgView);
        content_imgView.setClickable(false);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }

        Intent getImage = getIntent();
        if (getImage != null) {
            Bundle extras = getImage.getExtras();
            if (extras != null) {
                imgPath = extras.getString("imgPath");

                Bitmap bmp = getSmallBitmap(imgPath);
                if (bmp != null) {
                    Log.i("111", "onCreate width:" + bmp.getWidth() + " height:" + bmp.getHeight() + " size:" + bmp.getByteCount());
                    content_imgView.setImageBitmap(bmp);
                    content_imgView.setAdjustViewBounds(true);
                    content_imgView.setMaxWidth(250);
                    content_imgView.setMaxHeight(250);
                    content_imgView.setClickable(true);
                }
            }
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
                mBtnCrop.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        mParent = findViewById(R.id.parent);
        mBg = findViewById(R.id.bg);
        mBtnCrop = findViewById(R.id.btn_crop);
        mPhotoView =  findViewById(R.id.img);
        mPhotoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }


//        Intent getImage = getIntent();
//        if (getImage != null) {
//            //获取intent传递过来的uri数据
//            mImageUri = getImage.getData();
//            if (mImageUri != null) {
//                new Thread() {
//                    public void run() {
//                        try {
//                            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mImageUri));
//                            runOnUiThread(() -> {
//                                content_imgView.setImageBitmap(bitmap);//显示到ImageView上style_imageScroll = findViewById(selected_id);
//                            });
//                        } catch (FileNotFoundException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }.start();
//            }
//        }


    @TargetApi(Build.VERSION_CODES.O)
    private void createNotificationChannel(String channelId, String channelName, int importance) {
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(channel);
    }

    public void second_activity(View view) {
        Intent intent = new Intent(this, chooseStyle.class);
//        intent.setDataAndType(mImageUri, "image/*");
        intent.putExtra("imgPath", imgPath);

        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this, content_imgView, "content_image");
        // start the new activity
        startActivity(intent, options.toBundle());
    }

    public void open_camera(View view) {

        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//用来打开相机的Intent
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {//这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
            File imageFile = createImageFile();//创建用来保存照片的文件
            if (imageFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    /*7.0以上要通过FileProvider将File转化为Uri*/
                    mImageUri = FileProvider.getUriForFile(this, "com.example.style", imageFile);
                } else {
                    /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                    mImageUri = Uri.fromFile(imageFile);
                }
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);//将用于输出的文件Uri传递给相机
                startActivityForResult(takePhotoIntent, CAMERA);//打开相机
            }
        }
    }

    private File createImageFile() {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
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

    public void open_album(View view) {
        Intent openAlbumIntent = new Intent(Intent.ACTION_GET_CONTENT);

        openAlbumIntent.setType("image/*");
        startActivityForResult(openAlbumIntent, ALBUM);//打开相册
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
        cropIntent.setDataAndType(mImageUri, "image/*");
        cropIntent.putExtra("crop", "true");
        cropIntent.putExtra("aspectX", 0.1);
        cropIntent.putExtra("aspectY", 0.1);
        cropIntent.putExtra("scale", true);
        cropIntent.putExtra(MediaStore.EXTRA_OUTPUT, croppedUri);
        cropIntent.putExtra("return-data", false);
        cropIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        cropIntent.putExtra("noFaceDetection", true); // no face detection

        String a = getRealFilePath(this,croppedUri);
        Log.i("111","cropped path: "+a);

        show = croppedUri;

        startActivityForResult(cropIntent, CROP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAMERA:
                if (resultCode == RESULT_OK) {
                    show = mImageUri;
                    imgPath = Objects.requireNonNull(mImageUri.getPath()).substring(10);
                    smallBitmap = getSmallBitmap(imgPath);
                    content_imgView.setImageBitmap(smallBitmap);//显示到ImageView上
                    content_imgView.setAdjustViewBounds(true);
                    content_imgView.setMaxHeight(250);
                    content_imgView.setMaxWidth(250);
                    content_imgView.setClickable(true);
                }
                break;

            case ALBUM:
                if (data == null) {//如果没有选取照片，则直接返回
                    return;
                }
                Log.i(TAG, "onActivityResult: ImageUriFromAlbum: " + data.getData());
                if (resultCode == RESULT_OK) {
                    mImageUri = data.getData();
                    show = mImageUri;
                    imgPath = getRealFilePath(this, mImageUri);
                    smallBitmap = getSmallBitmap(imgPath);
                    Log.i("111", "width:" + smallBitmap.getWidth() + " height: " + smallBitmap.getHeight());
                    content_imgView.setImageBitmap(smallBitmap);//显示到ImageView上
                    content_imgView.setAdjustViewBounds(true);
                    content_imgView.setMaxHeight(250);
                    content_imgView.setMaxWidth(250);
                    content_imgView.setClickable(true);
                }
                break;
            case CROP:
                if (croppedUri == null) {//如果没有选取照片，则直接返回
                    return;
                }
                mParent.setVisibility(View.GONE);
                Log.i(TAG, "cropped image uri: " + croppedUri);
                if (resultCode == RESULT_OK) {
                    imgPath = getRealFilePath(this, croppedUri);
                    smallBitmap = getSmallBitmap(imgPath);
                    Log.i("111", "cropped width:" + smallBitmap.getWidth() + " height: " + smallBitmap.getHeight());
                    content_imgView.setImageBitmap(smallBitmap);//显示到ImageView上
                    content_imgView.setAdjustViewBounds(true);
                    content_imgView.setMaxHeight(250);
                    content_imgView.setMaxWidth(250);
                    content_imgView.setClickable(true);
                }
            default:
                break;
        }
    }

    public static String getRealFilePath(final Context context, final Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
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
        float ww = 600f;//这里设置宽度为480f
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


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                Log.i("MainActivity", "申请的权限为：" + permissions[i] + ",申请结果：" + grantResults[i]);
            }
        }
    }

    public void clickImg(View view) {
        mInfo = PhotoView.getImageViewInfo((ImageView) view);
        mPhotoView.setImageBitmap(BitmapFactory.decodeFile(imgPath));
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

}
