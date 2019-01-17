package com.baidu.tpalette;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.Preconditions;
import android.view.View;

import com.baidu.tpalette.lifecycle.PaletteManagerRetriever;

import java.util.ArrayList;
import java.util.List;
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

    private PaletteManagerRetriever paletteManagerRetriever;
    private final List<PaletteManager> managers = new ArrayList<>();


    private TPalette() {
    }


    public static PaletteManager with(@NonNull Context context) {
        return getRetriever(context).get(context);
    }

    @NonNull
    public static PaletteManager with(@NonNull Activity activity) {
        return getRetriever(activity).get(activity);
    }

    @NonNull
    public static PaletteManager with(@NonNull FragmentActivity activity) {
        return getRetriever(activity).get(activity);
    }

    @NonNull
    public static PaletteManager with(@NonNull android.app.Fragment fragment) {
        return getRetriever(fragment.getActivity()).get(fragment);
    }


    @NonNull
    public static PaletteManager with(@NonNull Fragment fragment) {
        return getRetriever(fragment.getActivity()).get(fragment);
    }

    /**
     * Get the singleton.
     *
     * @return the singleton
     */
    @NonNull
    public static TPalette get(@NonNull Context context) {
        if (tPalette == null) {
            synchronized (TPalette.class) {
                if (tPalette == null) {
                    checkAndInitialize(context);
                }
            }
        }

        return tPalette;
    }

    private static void checkAndInitialize(@NonNull Object context) {
        if (isInitializing) {
            throw new IllegalStateException("is initializing, can't call TPalette.withLife()");
        }
        isInitializing = true;
        init(context);
        isInitializing = false;
    }

    private static void init(@NonNull Object context) {
        Context applicationContext;
        if (context instanceof Activity) {
            applicationContext = ((Activity) context).getApplicationContext();
        } else {
            applicationContext = ((Fragment) context).getActivity().getApplicationContext();

        }
         tPalette = new TPalette();
    }

    @NonNull
    private static PaletteManagerRetriever getRetriever(@Nullable Context context) {
        if (context == null){
            throw new NullPointerException("context is null");
        }
        return TPalette.get(context).getRequestManagerRetriever();
    }

    @NonNull
    public PaletteManagerRetriever getRequestManagerRetriever() {
        return paletteManagerRetriever;
    }

    void registerPaletteManager(PaletteManager paletteManager) {
        synchronized (managers) {
            if (managers.contains(paletteManager)) {
                throw new IllegalStateException("Cannot register already registered manager");
            }
            managers.add(paletteManager);
        }
    }

    public void unregisterPaletteManager(PaletteManager paletteManager) {
        synchronized (managers) {
            if (!managers.contains(paletteManager)) {
                throw new IllegalStateException("Cannot unregister not yet registered manager");
            }
            managers.remove(paletteManager);
        }
    }
}
