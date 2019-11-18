package com.example.style;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.bm.library.Info;
import com.bm.library.PhotoView;


public class chooseStyle extends AppCompatActivity {

    private static final String TAG_show_style = "showStyle";
    ImageView style_imgView;
    roundImageView content_imgView;
//    Button change_mod_btn;
    Uri imageUri;
    String imgPath;
    int selected_id = 0;
    int selected_tag = 0;
//    int flag = 0;

    View mParent;
    //点击放大后的背景
    View mBg;
    PhotoView mPhotoView;
    Info mInfo;
    AlphaAnimation in = new AlphaAnimation(0, 1);
    AlphaAnimation out = new AlphaAnimation(1, 0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_style);
//        postponeEnterTransition();
        //沉浸式状态栏
        View decorView = getWindow().getDecorView();
        int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        decorView.setSystemUiVisibility(option);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.hide();

        style_imgView = findViewById(R.id.imageView_style);
        content_imgView = findViewById(R.id.img_content_small_150);
//        change_mod_btn = findViewById(R.id.btn_model_method);

        Intent getImage = getIntent();
        if (getImage != null) {
            Bundle extras = getImage.getExtras();
            assert extras != null;
            imgPath = extras.getString("imgPath");

            Bitmap bmp = getSmallBitmap(imgPath);
            assert bmp != null;
            Log.i("112","bitmap height: "+bmp.getHeight()+" width: " + bmp.getWidth());
            content_imgView.setImageBitmap(bmp);
//            startPostponedEnterTransition();
//            content_imgView.setImageBitmapPath(imgPath);
        }
//        flag = Integer.valueOf((String) change_mod_btn.getTag());

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

        mParent = findViewById(R.id.style_parent);
        mBg = findViewById(R.id.style_bg);
        mPhotoView = findViewById(R.id.style_img);
        mPhotoView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    }

    public void show_styleImg(View view) {
//        将原本选择的图片的背景清空
        Log.i("112","imageview height: "+content_imgView.getHeight()+" width: " + content_imgView.getWidth());
        ImageView style_imageScroll;
        if (selected_id != 0) {
            style_imageScroll = findViewById(selected_id);
            style_imageScroll.setBackgroundResource(0);
        }
//        设置背景
        selected_id = view.getId();
        style_imageScroll = findViewById(selected_id);
        style_imageScroll.setBackground(getDrawable(R.drawable.scroll_img_bkgd));

        Drawable drawable = style_imageScroll.getDrawable();
        Log.i(TAG_show_style, "Drawable: " + drawable);
        style_imgView.setImageDrawable(drawable);
        selected_tag = 1;
        try {
            selected_tag = Integer.valueOf((String) view.getTag());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("112", "tag:" + selected_tag);
    }

    public void go_back(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("imgPath", imgPath);
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this, content_imgView, "content_image");
        // start the new activity
        startActivity(intent, options.toBundle());
    }

    public Drawable roundBitmap(Bitmap bitmap,float radius) {
        Log.i("112","圆角");
        RoundedBitmapDrawable roundedDrawable = RoundedBitmapDrawableFactory.create(getResources(),bitmap);
        roundedDrawable.setCornerRadius(radius);
        return roundedDrawable;
    }


    public void transfer(View view) {
        Intent intent = new Intent(this, show_styler.class);
        intent.putExtra("style_number", selected_tag);
//        intent.putExtra("model_mode", flag);
        intent.putExtra("imgPath", imgPath);
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this, content_imgView, "content_image");
        // start the new activity
        if (selected_id != 0) {
            startActivity(intent, options.toBundle());
        } else {
            Toast.makeText(this, "選擇一個風格", Toast.LENGTH_SHORT).show();
        }
    }

//    public void change_mod(View view) {
//        if (flag == 1) {
//            change_mod_btn.setTag(0);
//            flag = 0;
//            change_mod_btn.setText("Interpreter\n(Tflite)");
//        } else {
//            change_mod_btn.setTag(1);
//            flag = 1;
//            change_mod_btn.setText("TensorFlow \n Inference \nInterface\n(pb)");
//        }
//    }

    public static Bitmap getSmallBitmap(String filePath) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//只解析图片边沿，获取宽高
        BitmapFactory.decodeFile(filePath, options);

        int originalWidth = options.outWidth;
        int originalHeight = options.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        int max=150;
        float ratio=1;
        int inSampleSize; // 默认像素压缩比例，压缩为原图的1/2

        if(originalHeight > originalWidth && originalHeight>=max){
            ratio = (float) originalHeight/max;
        }
        else if(originalHeight <= originalWidth && originalWidth >=max){
            ratio = (float) originalWidth/max;
        }
        inSampleSize = (int)ratio;
        options.inJustDecodeBounds = false;

        options.inSampleSize = inSampleSize;
        Log.i("112","inSampleSize: "+inSampleSize);
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
        mPhotoView.animaFrom(mInfo);

        mPhotoView.enable();
        mPhotoView.setOnClickListener(v -> {
            mBg.startAnimation(out);
            mPhotoView.animaTo(mInfo, () -> mParent.setVisibility(View.GONE));
        });
    }

    @Override
    public void onBackPressed() {
        if (mParent.getVisibility() == View.VISIBLE) {
            mBg.startAnimation(out);
            mPhotoView.animaTo(mInfo, () -> mParent.setVisibility(View.GONE));
        } else {
            super.onBackPressed();
        }
    }
}
