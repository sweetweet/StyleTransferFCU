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
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import java.io.FileNotFoundException;

public class chooseStyle extends AppCompatActivity {

    private static final String TAG_show_style = "showStyle";
    ImageView style_imgView;
    ImageView content_imgView;
    ImageView origin_imgView;
    Uri imageUri;
    int selected_id = 0;
    int selected_tag = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_style);
        style_imgView = findViewById(R.id.imageView_style);
        content_imgView = findViewById(R.id.img_content_small_150);
        origin_imgView = findViewById(R.id.content_imgView);
        Intent getImage = getIntent();
        if (getImage != null) {
            //获取intent传递过来的uri数据
            imageUri = getImage.getData();
            if (imageUri != null) {
                Bitmap bitmap = null;
                try {
                    bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(imageUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
//                content_imgView.setImageBitmap(bitmap);//显示到ImageView上
                Bitmap newBit = getRoundBitmapByShader(bitmap, content_imgView.getMaxHeight(), 10, 2);
                content_imgView.setImageBitmap(newBit);//显示到ImageView上
            }
        }
    }

    public void show_styleImg(View view) {
//        将原本选择的图片的背景清空
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
        selected_tag=1;
        try {
            selected_tag = Integer.valueOf((String)view.getTag());
        } catch (Exception e){
            e.printStackTrace();
        }
        Log.i("111","tag:" + selected_tag);
    }

    public void go_back(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setDataAndType(imageUri, "image/*");
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this, view, "content_image");
        // start the new activity
        startActivity(intent, options.toBundle());
    }


    public static Bitmap getRoundBitmapByShader(Bitmap bitmap, int edge, int radius, int boarder) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int outWidth;
        int outHeight;
        float widthScale = edge * 1f / width;
        float heightScale = edge * 1f / height;

        if (width > height) {
            outWidth = edge;
            outHeight = edge * height / width;
        } else if (width < height) {
            outHeight = edge;
            outWidth = edge * width / height;
        } else {
            outHeight = edge;
            outWidth = edge;
        }

        Matrix matrix = new Matrix();
        matrix.setScale(widthScale, heightScale);
        //创建输出的bitmap
        Bitmap desBitmap = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888);
        //创建canvas并传入desBitmap，这样绘制的内容都会在desBitmap上
        Canvas canvas = new Canvas(desBitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        //创建着色器
        BitmapShader bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        //给着色器配置matrix
        bitmapShader.setLocalMatrix(matrix);
        paint.setShader(bitmapShader);
        //创建矩形区域并且预留出border
        RectF rect = new RectF(boarder, boarder, outWidth - boarder, outHeight- boarder);
        //把传入的bitmap绘制到圆角矩形区域内
        canvas.drawRoundRect(rect, radius, radius, paint);

        if (boarder > 0) {
            //绘制boarder
            Paint boarderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boarderPaint.setColor(Color.RED);
            boarderPaint.setStyle(Paint.Style.STROKE);
            boarderPaint.setStrokeWidth(boarder);
            canvas.drawRoundRect(rect, radius, radius, boarderPaint);
        }
        return desBitmap;
    }

    public void transfer(View view) {
        Intent intent = new Intent(this, show_styler.class);
        intent.setDataAndType(imageUri, "image/*");
        intent.putExtra("style_number",selected_tag);
        ActivityOptionsCompat options = ActivityOptionsCompat
                .makeSceneTransitionAnimation(this, content_imgView, "content_image");
        // start the new activity
        startActivity(intent, options.toBundle());
    }
}
