package com.example.style;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
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
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.widget.ImageView.ScaleType.FIT_CENTER;

public class show_styler extends AppCompatActivity {
    public static final int CROP = 3;
    String imgPath;
    ImageView result_imgView;
    Bitmap cropResult = null;
    Bitmap transferResultBitmap;
    String resultPath;
    Uri resultUri;
    Uri croppedUri;
    View mParent;
    View mProcess;
    //点击放大后的背景
    View mBg;
    View mBtnCrop;
    ImageButton mBtnShare;
    PhotoView mPhotoView;
    Info mInfo;
    AlphaAnimation in = new AlphaAnimation(0, 1);
    AlphaAnimation out = new AlphaAnimation(1, 0);

    Boolean isCurrentActivity = true;

    private String totalTime;
    public int style_number;
    public float quality;

    ServiceConnection connection;
    public MyReceiver receiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("113", "on create");
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
        mBtnShare = findViewById(R.id.btn_share);
        Intent getImage = getIntent();
        if (getImage != null) {
            Bundle bd = getImage.getExtras();
            assert bd != null;
            style_number = bd.getInt("style_number");
//            flag = bd.getInt("model_mode");
            imgPath = bd.getString("imgPath");
            quality = bd.getFloat("quality");
            //            new Thread(() -> {
            Bitmap smallBitmap = MainActivity.getSmallBitmap(imgPath);
            assert smallBitmap != null;
            Bitmap blurBitmap = FastBlurUtil.toBlur(smallBitmap, 10);
//                runOnUiThread(() -> {
            result_imgView.setScaleType(FIT_CENTER);
            result_imgView.setImageBitmap(blurBitmap);
            result_imgView.setClickable(false);
//                });
//            }).start();

            Log.i("113", "set imageview");

            Intent intent = new Intent(this, transferService.class);
            connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.i("113", "service connection ");
                    transferService.MyBinder binder = (transferService.MyBinder) service;
                    transferService mService = binder.getService();
                    mService.setImagePath(imgPath);
                    mService.setQuality(quality);
                    mService.setStyleNumber(style_number);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.i("113", "disconnected");
                }
            };

            bindService(intent, connection, Context.BIND_AUTO_CREATE);
            receiver = new MyReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("BROADCAST");
            this.registerReceiver(receiver, filter);
            Log.i("113", "register successfully");
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
        mProcess = findViewById(R.id.process);
        mBg = findViewById(R.id.result_bg);
        mPhotoView = findViewById(R.id.result_img);
        mPhotoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    public void shareImage(View view) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM,resultUri);
        startActivity(intent);
    }

    public class MyReceiver extends BroadcastReceiver {
        //自定义一个广播接收器
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            assert bundle != null;
            resultUri = Uri.parse(bundle.getString("uri"));
            resultPath = bundle.getString("path");
            totalTime = bundle.getString("time");
            Log.i("113", "result uri: " + resultUri);
            unbindService(connection);
            showResult();
            mProcess.setVisibility(View.INVISIBLE);
            mBtnShare.setVisibility(View.VISIBLE);
            if(!isCurrentActivity)
                sendNotification();
        }

        public MyReceiver() {
        }
    }

    private void sendNotification() {
        Log.i("113", "notification");
        Bitmap smallBitmap = MainActivity.getSmallBitmap(resultPath);
        Intent intent = new Intent(this, this.getClass());
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
//        intent.putExtra("flag", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, FLAG_UPDATE_CURRENT);

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, "result")
                .setContentTitle("风格转换完毕")
                .setContentText("点击查看")
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.drawable.icon1))
                .setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(smallBitmap)
                        .bigLargeIcon(null))
                .setAutoCancel(true)
                .setContentIntent(pIntent)
                .build();
        manager.notify(1, notification);
    }

    private void showResult() {
        transferResultBitmap = BitmapFactory.decodeFile(resultPath);
        assert transferResultBitmap != null;
        int degree = MainActivity.readPictureDegree(resultPath);
        if (degree != 0)
            transferResultBitmap= MainActivity.rotaingImageView(degree,transferResultBitmap);
        result_imgView.setImageBitmap(transferResultBitmap);
        result_imgView.setClickable(true);
        Toast.makeText(show_styler.this, "totoal time: " + totalTime + "s", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        Log.i("113", "on resume");
        isCurrentActivity = true;
        super.onResume();
    }

    @Override
    protected void onStart() {
        Log.i("113","on start");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.i("113", "on restart");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.i("113", "on stop");
        isCurrentActivity = false;
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.i("113", "on Pause");
        isCurrentActivity = false;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
//        if(result_flag)
        Log.i("113", "on destroy");
        unregisterReceiver(receiver);
        super.onDestroy();
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
        Log.i("113","click image");
        mInfo = PhotoView.getImageViewInfo((ImageView) view);
        mPhotoView.setImageBitmap(transferResultBitmap);

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
        Log.i("111", "crop image");
        croppedUri = null;
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "RESULT_CROP_" + timeStamp;
        String fff = "file://" + "/" + Environment.getExternalStorageDirectory() + "/Styler/"; //设置图片名称
        File fullFile = new File(fff, imageFileName + ".jpg"); //将图片路径转换成uri
        croppedUri = Uri.parse(fullFile.toString());

        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        }
        cropIntent.setDataAndType(resultUri, "image/*");
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
        switch (requestCode) {
            case CROP:
                if (croppedUri == null) {//如果没有选取照片，则直接返回
                    return;
                }
                mParent.setVisibility(View.GONE);
                Log.i("113", "cropped image uri: " + croppedUri);
                if (resultCode == RESULT_OK) {
                    resultUri = croppedUri;
                    imgPath = MainActivity.getRealFilePath(this, croppedUri);
//                    cropResult = BitmapFactory.decodeFile(imgPath);
//                    result_imgView.setImageBitmap(cropResult);
                    transferResultBitmap = BitmapFactory.decodeFile(imgPath);
                    result_imgView.setImageBitmap(transferResultBitmap);
                    result_imgView.setClickable(true);
                }
                break;
            default:
                break;
        }
    }

}
