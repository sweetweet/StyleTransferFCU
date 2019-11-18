package com.example.style;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatImageView;

public class roundImageView extends AppCompatImageView {
    float width, height;
    public roundImageView(Context context) {
        super(context);
    }

    public roundImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public roundImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        width = getWidth();
        height = getHeight();
    }


    public void setImageBitmapPath(String filePath) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;//只解析图片边沿，获取宽高
        BitmapFactory.decodeFile(filePath, options);

        int originalWidth = options.outWidth;
        int originalHeight = options.outHeight;
        int max=300;
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
        Log.i("11?","inSampleSize: "+inSampleSize);
        super.setImageBitmap(BitmapFactory.decodeFile(filePath,options));
    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (width >= 12 && height > 12) {
            Path path = new Path();
            //四个圆角
            path.moveTo(12, 0);
            path.lineTo(width - 12, 0);
            path.quadTo(width, 0, width, 12);
            path.lineTo(width, height - 12);
            path.quadTo(width, height, width - 12, height);
            path.lineTo(12, height);
            path.quadTo(0, height, 0, height - 12);
            path.lineTo(0, 12);
            path.quadTo(0, 0, 12, 0);

            canvas.clipPath(path);
        }
        super.onDraw(canvas);
    }
}
