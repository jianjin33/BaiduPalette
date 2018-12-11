package com.baidu.tpalette;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import java.util.concurrent.Executor;

/**
 * @Description TPalette 取色框架的入口；TPalette取色是基于Google的取色框架Palette的一层封装，旨在使用起来
 * 更加方便，可以自动管理异步的生命周期及其回调处理，同时能和一些图片加载框架结合使用，避免多次在内存中创建bitmap对象。
 * @Author JianZuming
 * @Date 18/12/11
 * @Version V1.0.0
 * @Update 更新说明
 */
public class TPalette {

    private static TPalette tPalette;
    private static volatile boolean isInitializing;

    private Context context;
    private int cacheSize;
    private Executor executor;


    private TPalette(Context context, int cacheSize, Executor executor) {
        this.context = context;
        this.cacheSize = cacheSize;
        this.executor = executor;
    }


    /**
     * 获取TPalette单例对象
     */
    @NonNull
    public static TPalette withLife(@NonNull Object context) {
        if (context instanceof Activity || context instanceof Fragment) {
            if (tPalette == null) {
                synchronized (TPalette.class) {
                    if (tPalette == null) {
                        checkAndInitializeGlide(context);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("argument must be activity or fragment");
        }

        return tPalette;
    }


    private static void checkAndInitializeGlide(@NonNull Object context) {
        if (isInitializing) {
            throw new IllegalStateException("is initializing, can't call TPalette.withLife()");
        }

        isInitializing = true;
        init(context);
        isInitializing = false;
    }

    private static void init(@NonNull Object context) {
        init(context, new TPaletteBuilder());
    }

    private static void init(@NonNull Object context, @NonNull TPaletteBuilder builder) {
        Context applicationContext;
        if (context instanceof Activity) {
            applicationContext = ((Activity) context).getApplicationContext();
        } else {
            applicationContext = ((Fragment) context).getActivity().getApplicationContext();

        }

        TPalette tPalette = builder.build(applicationContext);

        TPalette.tPalette = tPalette;
    }

    private static class TPaletteBuilder {
        private int cacheSize;
        private Executor executor;

        public TPaletteBuilder setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        public TPaletteBuilder setExecutor(Executor executor) {
            this.executor = executor;
            return this;
        }

        public TPalette build(Context context) {
            return new TPalette(context, cacheSize, executor);
        }

    }
}
