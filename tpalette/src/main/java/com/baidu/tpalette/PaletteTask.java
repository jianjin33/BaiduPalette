package com.baidu.tpalette;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.View;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static android.content.ContentValues.TAG;
import static com.baidu.tpalette.PaletteOptions.PALETTE_DEFAULT_RESIZE_BITMAP_AREA;
import static java.lang.Thread.sleep;

public class PaletteTask {
    private Context context;
    private Object object;
    private View target;
    private PaletteCallback paletteCallback;


    public PaletteTask(Context context, Object object, View target, PaletteCallback paletteCallback) {
        this.context = context;
        this.object = object;
        this.target = target;
        this.paletteCallback = paletteCallback;
    }

    public void start() {
        palette(context, object);
    }

    public void recycle() {
        paletteCallback = null;
        target = null;
    }

    private void palette(final Context context, final Object obj) {
        new AsyncTask<Bitmap, Void, Palette>() {
            @Nullable
            protected Palette doInBackground(Bitmap... params) {

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                final Integer color = ColorCache.hitPaletteCache(obj.toString());
                if (color != null) {
                    // 字体颜色无需求，暂时返回白色
                    //UiThreadHandler.post(() -> paletteCallback.setColor(color, Color.WHITE));
                    if (target != null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                target.setBackgroundColor(color);
                            }
                        });

                    }
                    if (paletteCallback != null) {
                        paletteCallback.setColor(color, Color.WHITE);
                    }
                    return null;
                }


                Bitmap bitmap = null;
                if (obj instanceof String) {
                    FutureTarget<Bitmap> futureTarget = Glide.with(context).asBitmap().load(obj.toString()).submit();
                    try {
                        //超时时间10s
                        bitmap = futureTarget.get(10, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        e.printStackTrace();
                    }
                    //bitmap = ImageLoaderOption.getInstance().submit(context, obj.toString(), null);
                } else if (obj instanceof Bitmap) {
                    bitmap = (Bitmap) obj;
                } else {
                    Log.e(TAG, "只支持图片url或bitmap对象，如有其它格式请添加");
                }

                if (bitmap == null) {
                    return null;
                }

                // bitmap压缩为30*30像素，并关闭获取柔和、活力等色调，提高取色速度。
                Palette.Builder builder = Palette.from(bitmap)
                        .resizeBitmapArea(PALETTE_DEFAULT_RESIZE_BITMAP_AREA)
                        .maximumColorCount(32)
                        .clearTargets();

                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    return builder.generate();
                } catch (Exception e) {
                    Log.e(TAG, "Exception thrown during async generate", e);
                    return null;
                }
            }

            protected void onPostExecute(@Nullable final Palette colorExtractor) {
                if (colorExtractor != null) {
                    // todo 主线程
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            asyncCallback(colorExtractor, paletteCallback, obj.toString());
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
            if (target != null) {
                target.setBackgroundColor(getColorWithAlpha(Color.WHITE));
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
            if (target != null) {
                target.setBackgroundColor(getColorWithAlpha(Color.WHITE));
            }
            if (paletteCallback != null) {
                // 取不到色值时，默认返回白色+20%透明度蒙版
                paletteCallback.setColor(getColorWithAlpha(Color.WHITE), Color.WHITE);
            }
            return;
        }

        // 覆盖一层20%透明度的蒙板
        int alphaColor = getColorWithAlpha(dominantSwatch.getRgb());
        if (target != null) {
            target.setBackgroundColor(alphaColor);
        }

        if (paletteCallback != null) {
            paletteCallback.setColor(alphaColor, dominantSwatch.getBodyTextColor());
        }
        if (cacheKey != null) {
            ColorCache.putPaletteCache(cacheKey, alphaColor);
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
