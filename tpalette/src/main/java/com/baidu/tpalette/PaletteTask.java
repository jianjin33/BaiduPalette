package com.baidu.tpalette;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;

import static com.baidu.tpalette.PaletteOptions.PALETTE_DEFAULT_RESIZE_BITMAP_AREA;


public class PaletteTask {
    private String TAG = PaletteTask.class.getSimpleName();
    private Object object;
    private View mTargetView;
    private PaletteCallback mPaletteCallback;
    private Palette mPalette;
    private AsyncTask asyncTask;
    private static Handler sHandler = new Handler(Looper.getMainLooper());


    public PaletteTask(Object object, View target, PaletteCallback paletteCallback) {
        this.object = object;
        this.mTargetView = target;
        this.mPaletteCallback = paletteCallback;
    }

    public void start() {
        if (object == null) {
            new IllegalArgumentException("取色对象不能为空");
            return;
        }
        palette(object);
    }

    public void recycle() {
        if (asyncTask != null) {
            asyncTask.cancel(false);
        }
        asyncTask = null;
        mPaletteCallback = null;
        mTargetView = null;
    }

    private void palette(final Object obj) {
        asyncTask = new AsyncTask<Bitmap, Void, Palette>() {
            @Override
            protected Palette doInBackground(Bitmap... params) {
                final Integer color = PaletteColorCache.hitPaletteCache(obj.toString());
                if (color != null) {
                    if (mTargetView != null) {
                        sHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mTargetView.setBackgroundColor(color);
                            }
                        });
                    }

                    if (mPaletteCallback != null) {
                        // 字体颜色无需求，暂时返回白色
                        sHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mPaletteCallback.setColor(color, Color.WHITE);
                            }
                        });
                    }

                    TPalette.get().removeFromManagers(PaletteTask.this);
                    return null;
                }


                Bitmap bitmap = null;
                if (obj instanceof String) {
                    // todo 下载方式暂未提供 自定义较好
                } else if (obj instanceof Bitmap) {
                    bitmap = (Bitmap) obj;
                } else {
                    Log.e(TAG, "只支持图片url或bitmap对象，如有其它格式请添加");
                }

                if (bitmap == null) {
                    TPalette.get().removeFromManagers(PaletteTask.this);
                    return null;
                }

                // bitmap压缩为30*30像素，并关闭获取柔和、活力等色调，提高取色速度。
                Palette.Builder builder = Palette.from(bitmap)
                        .resizeBitmapArea(PALETTE_DEFAULT_RESIZE_BITMAP_AREA)
                        .maximumColorCount(32)
                        .clearTargets();

                try {
                    mPalette = builder.generate();
                    if (!bitmap.isRecycled()) {
                        bitmap.recycle();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown during async generate", e);
                }
                TPalette.get().removeFromManagers(PaletteTask.this);
                return mPalette;
            }

            @Override
            protected void onPostExecute(final Palette colorExtractor) {
                super.onPostExecute(colorExtractor);
                if (colorExtractor != null) {
                    // todo 主线程
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            asyncCallback(colorExtractor, mPaletteCallback, obj.toString());
                        }
                    });

                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }


    /**
     * 取色处理回调
     *
     * @param palette         色值对象
     * @param paletteCallback 回调接口
     * @param cacheKey        缓存键
     */
    private void asyncCallback(Palette palette, PaletteCallback paletteCallback, String cacheKey) {
        if (palette == null) {
            if (mTargetView != null) {
                mTargetView.setBackgroundColor(getColorWithAlpha(Color.WHITE));
            }
            if (paletteCallback != null) {
                // 取不到色值时，默认返回白色+20%透明度蒙版
                paletteCallback.setColor(getColorWithAlpha(Color.WHITE), Color.WHITE);
            }
            return;
        }
        // 获取像素占比最高的色调RGB
        Palette.Swatch dominantSwatch = palette.getDominantSwatch();

        if (dominantSwatch == null) {
            if (mTargetView != null) {
                mTargetView.setBackgroundColor(getColorWithAlpha(Color.WHITE));
            }
            if (paletteCallback != null) {
                // 取不到色值时，默认返回白色+20%透明度蒙版
                paletteCallback.setColor(getColorWithAlpha(Color.WHITE), Color.WHITE);
            }
            return;
        }

        // 覆盖一层20%透明度的蒙板
        int alphaColor = getColorWithAlpha(dominantSwatch.getRgb());
        if (mTargetView != null) {
            mTargetView.setBackgroundColor(alphaColor);
        }

        if (paletteCallback != null) {
            paletteCallback.setColor(alphaColor, dominantSwatch.getBodyTextColor());
        }
        if (cacheKey != null) {
            PaletteColorCache.putPaletteCache(cacheKey, alphaColor);
        }
    }

    /**
     * 对RGB颜色进行处理，相当于覆盖一层20%蒙版效果
     *
     * @param originRGB 待处理的RGB色值
     * @return 处理之后RGB色值
     */
    private static int getColorWithAlpha(int originRGB) {
        int red = Color.red(originRGB);
        int green = Color.green(originRGB);
        int blue = Color.blue(originRGB);

        int alphaRed;
        int alphaGreen;
        int alphaBlue;

        // 眼前的黑不是黑,你要的白是什么白
        float result = (float) (blue * 0.114 + red * 0.299 + green * 0.587);
        if (result < 30.0) {
            alphaRed = 255 - (255 - 60) * (255 - red) / 255;
            alphaGreen = 255 - (255 - 50) * (255 - green) / 255;
            alphaBlue = 255 - (255 - 50) * (255 - blue) / 255;
        } else {
            // 覆盖一层20%透明度的蒙板
            alphaRed = (int) (red * 204 / 255.0f);
            alphaGreen = (int) (green * 204 / 255.0f);
            alphaBlue = (int) (blue * 204 / 255.0f);
        }


        // Color.rgb(alphaRed, alphaGreen, alphaBlue);
        // Color.rgb()由于只支持26以上版本
        return 0xff000000 | (alphaRed << 16) | (alphaGreen << 8) | alphaBlue;
    }
}
