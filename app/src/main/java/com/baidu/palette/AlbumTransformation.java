package com.baidu.palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapResource;

import java.security.MessageDigest;

public class AlbumTransformation {
    private static Paint sMaskingPaint = new Paint();
    private static Paint mPaint = new Paint();
    private static int LINE_WIDTH = 20;
    private Context mContext;
    private BitmapPool mBitmapPool;
    private int mRadius;


    static {
        sMaskingPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        sMaskingPaint.setAntiAlias(true);
        sMaskingPaint.setDither(true);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.BLUE);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStrokeWidth(LINE_WIDTH);
    }

    /**
     * @param context
     * @param radius  专辑覆盖半圆的半径 px
     */
    public AlbumTransformation(Context context, int radius) {
        this(context, Glide.get(context).getBitmapPool(), 50);
    }

    public AlbumTransformation(Context context, BitmapPool pool, int radius) {
        mBitmapPool = pool;
        mContext = context.getApplicationContext();
        this.mRadius = radius;
    }

    @NonNull
    public Resource<Bitmap> transform(@NonNull Context context, @NonNull Resource<Bitmap> resource, int outWidth, int outHeight) {
        Bitmap source = resource.get();

        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap result = mBitmapPool.get(width, height, Bitmap.Config.ARGB_8888);
        if (result == null) {
            result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }


        Canvas canvas = new Canvas(result);

        // 图片
        canvas.drawBitmap(source, 0, 0, mPaint);

        // 边框线 + 缺口半圆线
        Path path = new Path();
        path.moveTo(0, 0);
        path.lineTo(0, height);
        path.lineTo(width, height);
        path.lineTo(width, height / 2 + mRadius);
        path.arcTo(width - mRadius, height / 2 - mRadius,
                width + mRadius, height / 2 + mRadius,
                90, 180, false);
        path.lineTo(width, 0);
        path.close();
        canvas.drawPath(path, mPaint);

        // 半圆区域绘制
        canvas.drawArc(width - mRadius, height / 2 - mRadius, width + mRadius, height / 2 + mRadius, 90, 180, true, sMaskingPaint);

        return BitmapResource.obtain(result, mBitmapPool);
    }

    public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
        messageDigest.update(key().getBytes());

    }

    private String key() {
        return "MaskTransformation(maskId=" + AlbumTransformation.this + ")";
    }
}
