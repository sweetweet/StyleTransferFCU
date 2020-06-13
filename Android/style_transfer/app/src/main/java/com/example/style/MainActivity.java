package com.example.style;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bm.library.Info;
import com.bm.library.PhotoView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

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
    ImageButton mBtnNext;
    PhotoView mPhotoView;
    Info mInfo;
    int degree;
    AlphaAnimation in = new AlphaAnimation(0, 1);
    AlphaAnimation out = new AlphaAnimation(1, 0);


    public static final int CAMERA = 1;
    public static final int ALBUM = 2;
    public static final int CROP = 3;

    private static final int TAKE_PHOTO_PERMISSION_REQUEST_CODE = 0; // 拍照的权限处理返回码
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1; // 读储存卡内容的权限处理返回码
    private static final int ALBUM_PERMISSION_REQUEST_CODE = 2; // 读储存卡内容的权限处理返回码

    private boolean isAndroidQ = Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q;

    String[] cameraPermissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    String[] albumPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};

    List<String> mPermissionList = new ArrayList<>();


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
        /*获取必要权限*/
//        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    WRITE_SDCARD_PERMISSION_REQUEST_CODE);
//        }

//       读取权限
//        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                    READ_SDCARD_PERMISSION_REQUEST_CODE);
//        }

//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
//            }
//        }

        Intent getImage = getIntent();
        if (getImage != null) {
            Bundle extras = getImage.getExtras();

            String action = getImage.getAction();
            String type = getImage.getType();
            Boolean flag = false;
            if (extras != null) {
                String temp = extras.getString("imgPath");
                if (temp != null) {
                    imgPath = temp;
                    flag = true;
                }
            }
            if (action != null && type != null) {
                Uri uri = getImage.getParcelableExtra(Intent.EXTRA_STREAM);
                assert uri != null;
//                imgPath = getFilePathByUri(this,uri);
                imgPath = getRealFilePath(this, uri);
                flag = true;
            }
            if (flag) {
                Bitmap bmp = getSmallBitmap(imgPath);
                if (bmp != null) {
                    Log.i("111", "onCreate width:" + bmp.getWidth() + " height:" + bmp.getHeight() + " size:" + bmp.getByteCount());
                    content_imgView.setImageBitmap(bmp);
                    content_imgView.setAdjustViewBounds(true);
                    content_imgView.setMaxWidth(250);
                    content_imgView.setMaxHeight(250);
                    content_imgView.setClickable(true);
                }
                flag=false;
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
        mBtnNext = findViewById(R.id.btn_next);
        mPhotoView = findViewById(R.id.img);
        mPhotoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    private void requestAlbumPermission() {
        mPermissionList.clear();
        for (String permission : albumPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.i("111", permission + " ");
                mPermissionList.add(permission);//添加还未授予的权限
            }
        }
        if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this, albumPermissions, ALBUM_PERMISSION_REQUEST_CODE);
        } else {
            open_album();
        }
    }

    private void requestCameraPermission() {
        mPermissionList.clear();
        for (String permission : cameraPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionList.add(permission);//添加还未授予的权限
            }
        }
        if (mPermissionList.size() > 0) {//有权限没有通过，需要申请
            ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            open_camera();
        }
    }

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

    public void click(View view) {
        switch (view.getId()) {
            case R.id.btn_camera:
                requestCameraPermission();
                break;
            case R.id.btn_album:
                requestAlbumPermission();
                break;
            default:
                break;
        }
    }

    public void open_camera() {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//用来打开相机的Intent
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {//这句作用是如果没有相机则该应用不会闪退，要是不加这句则当系统没有相机应用的时候该应用会闪退
//            if (isAndroidQ) {
//                mImageUri = createImageUri();
//                Log.i("111","isAndroidQ imageUri: "+mImageUri);
//            } else {
                File imageFile = createImageFile();//创建用来保存照片的文件
                Log.i("111","imageFile "+imageFile);
                if (imageFile != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        /*7.0以上要通过FileProvider将File转化为Uri*/
                        mImageUri = FileProvider.getUriForFile(this, "com.example.style", imageFile);
                    } else {
                        /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                        mImageUri = Uri.fromFile(imageFile);
                    }
                }
            }
            if (mImageUri != null) {
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);//将用于输出的文件Uri传递给相机
                startActivityForResult(takePhotoIntent, CAMERA);//打开相机
            }
//        }

    }

    private Uri createImageUri() {
        String status = Environment.getExternalStorageState();
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new ContentValues());
        } else {
            return getContentResolver().insert(MediaStore.Images.Media.INTERNAL_CONTENT_URI, new ContentValues());
        }
    }

    private File createImageFile() {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = new File(Environment.getExternalStorageDirectory(), "Styler");
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        File imageFile = null;
        try {
            imageFile = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert imageFile != null;
//        Log.i("111", "abs path" + imageFile.getAbsolutePath());
        return imageFile;
    }

    public void open_album() {
        Intent openAlbumIntent = new Intent(Intent.ACTION_GET_CONTENT);

        openAlbumIntent.setType("image/*");
        startActivityForResult(openAlbumIntent, ALBUM);//打开相册
    }

    public void cropImage(View view) {
        Log.i("111", "crop image");
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

        String a = getRealFilePath(this, croppedUri);
        Log.i("111", "cropped path: " + a);

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
                    galleryAddPic(mImageUri);
//                    if(isAndroidQ){
//                        content_imgView.setImageURI(mImageUri);
//                    }else{
                        imgPath = Objects.requireNonNull(mImageUri.getPath()).substring(10);
                        Log.i("111", "uri path: " + mImageUri.getPath());
                        Log.i("111", "imgPath: " +imgPath);
                        smallBitmap = getSmallBitmap(imgPath);
                        content_imgView.setImageURI(mImageUri);
                        content_imgView.setImageBitmap(smallBitmap);//显示到ImageView上
//                    }
                    content_imgView.setAdjustViewBounds(true);
                    content_imgView.setMaxHeight(250);
                    content_imgView.setMaxWidth(250);
                    content_imgView.setClickable(true);
                    mBtnNext.setVisibility(View.VISIBLE);
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
                    Log.i("111", "mImageUri is " + mImageUri);
//                    Toast.makeText(MainActivity.this, "imageuri is "+ mImageUri,Toast.LENGTH_SHORT).show();
                    if(isAndroidQ) {
                        imgPath = getFilePathByUri(this, mImageUri);
                        Log.i("111", "AndroidQ img path is " + imgPath);
                    }else {
                        imgPath = getRealFilePath(this, mImageUri);
                        Log.i("111", "img path is " + imgPath);
//                    imgPath = getFilePathByUri(this,mImageUri);
                    }
                    smallBitmap = getSmallBitmap(imgPath);
                    Log.i("111", "width:" + smallBitmap.getWidth() + " height: " + smallBitmap.getHeight());
                    content_imgView.setImageBitmap(smallBitmap);//显示到ImageView上
                    content_imgView.setAdjustViewBounds(true);
                    content_imgView.setMaxHeight(250);
                    content_imgView.setMaxWidth(250);
                    content_imgView.setClickable(true);
                    mBtnNext.setVisibility(View.VISIBLE);
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

    /**
     * 将拍的照片添加到相册
     * @param uri 拍的照片的Uri
     */
    private void galleryAddPic(Uri uri){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uri);
        sendBroadcast(mediaScanIntent);
    }

    public static String getFilePathByUri(Context context, Uri uri) {
        String path = null;
        // 以 file:// 开头的
        if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            path = uri.getPath();
            return path;
        }
        // 以 content:// 开头的，比如 content://media/extenral/images/media/17766
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.Media.DATA}, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (columnIndex > -1) {
                        path = cursor.getString(columnIndex);
                    }
                }
                cursor.close();
            }
            return path;
        }
        // 4.4及之后的 是以 content:// 开头的，比如 content://com.android.providers.media.documents/document/image%3A235700
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    // ExternalStorageProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        path = Environment.getExternalStorageDirectory() + "/" + split[1];
                        return path;
                    }
                } else if (isDownloadsDocument(uri)) {
                    // DownloadsProvider
                    final String id = DocumentsContract.getDocumentId(uri);
                    final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    path = getDataColumn(context, contentUri, null, null);
                    return path;
                } else if (isMediaDocument(uri)) {
                    // MediaProvider
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    path = getDataColumn(context, contentUri, selection, selectionArgs);
                    return path;
                }
            }
        }
        return null;
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
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
        Bitmap smallBitmap = BitmapFactory.decodeFile(filePath, options);
        int degree = readPictureDegree(filePath);
        if (degree!=0)
            return rotaingImageView(degree,smallBitmap);
        else
            return  smallBitmap;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isPermissionDenied = false;
        switch (requestCode) {
            case CAMERA_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    // 因为是多个权限，所以需要一个循环获取每个权限的获取情况
                    for (int i = 0; i < grantResults.length; i++) {
                        // PERMISSION_DENIED 这个值代表是没有授权，我们可以把被拒绝授权的权限显示出来
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            isPermissionDenied = true;
                            Toast.makeText(MainActivity.this, permissions[i] + "权限被拒绝了--camera", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (!isPermissionDenied) {
                        open_camera();
                    }
                }
                break;
            case ALBUM_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    // 因为是多个权限，所以需要一个循环获取每个权限的获取情况
                    for (int i = 0; i < grantResults.length; i++) {
                        // PERMISSION_DENIED 这个值代表是没有授权，我们可以把被拒绝授权的权限显示出来
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            isPermissionDenied = true;
                            Toast.makeText(MainActivity.this, permissions[i] + "权限被拒绝了--album", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (!isPermissionDenied) {
                        open_album();
                    }
                }
                break;
            default:
                break;
        }
    }

    public void clickImg(View view) {

        Log.i("111", "click image");
        mInfo = PhotoView.getImageViewInfo((ImageView) view);
//        if(isAndroidQ)
//            mPhotoView.setImageURI(mImageUri);
//        else
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

    /**
     * 读取图片属性：旋转的角度
     * @param path 图片绝对路径
     * @return degree旋转的角度
     */
    public static int readPictureDegree(String path) {
        int degree  = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return degree;
    }

    /*
     * 旋转图片
     * @param angle
     * @param bitmap
     * @return Bitmap
     */
    public static Bitmap rotaingImageView(int angle , Bitmap bitmap) {
        //旋转图片 动作
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        // 创建新的图片
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
