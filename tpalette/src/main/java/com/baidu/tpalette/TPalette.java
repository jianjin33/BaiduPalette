package com.baidu.tpalette;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;


import com.baidu.tpalette.lifecycle.PaletteManagerRetriever;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JianZuming
 * @description 取色功能的入口。单例对象。
 * @date 2019/1/21
 * 用法：1.TPalette.with(Context).load(url/bitmap).into(view);
 * 2.TPalette.with(Context).load(url/bitmap).addPaletteCallback(PaletteCallback).into(view);
 * 3.TPalette.with(Context).load(url/bitmap).addPaletteCallback(PaletteCallback).submit();
 */
public class TPalette {
    private static String TAG = TPalette.class.getSimpleName();
    private static TPalette tPalette;
    private static volatile boolean isInitializing; // 判断正在初始化


    private PaletteManagerRetriever paletteManagerRetriever;
    private final List<PaletteManager> managers = new ArrayList<>();

    private TPalette(PaletteManagerRetriever paletteManagerRetriever) {
        this.paletteManagerRetriever = paletteManagerRetriever;
    }

    /**
     * 内部管理取色任务的生命周期时，会获取该单例对象，一般外部无需使用该方法来
     * @return
     */
    @NonNull
    public static TPalette get() {
        if (tPalette == null) {
            synchronized (TPalette.class) {
                if (tPalette == null) {
                    checkAndInitialize();
                }
            }
        }
        return tPalette;
    }

    private static void checkAndInitialize() {
        if (isInitializing) {
            throw new IllegalStateException("is initializing, can't call TPalette.get()");
        }
        isInitializing = true;

        PaletteManagerRetriever paletteManagerRetriever = new PaletteManagerRetriever(null);
        tPalette = new TPalette(paletteManagerRetriever);

        isInitializing = false;
    }

    public static PaletteManager with(@NonNull Context context) {
        return getRetriever().get(context);
    }

    @NonNull
    public static PaletteManager with(@NonNull Activity activity) {
        return getRetriever().get(activity);
    }

    @NonNull
    public static PaletteManager with(@NonNull FragmentActivity activity) {
        return getRetriever().get(activity);
    }

    @NonNull
    public static PaletteManager with(@NonNull android.app.Fragment fragment) {
        return getRetriever().get(fragment);
    }

    @NonNull
    public static PaletteManager with(@NonNull Fragment fragment) {
        return getRetriever().get(fragment);
    }

    @NonNull
    private static PaletteManagerRetriever getRetriever() {
        // 初始化TPalette的地方，同时会创建一个PaletteManagerRetriever对象
        return TPalette.get().getPaletteManagerRetriever();
    }

    @NonNull
    public PaletteManagerRetriever getPaletteManagerRetriever() {
        return paletteManagerRetriever;
    }


    void registerPaletteManager(PaletteManager paletteManager) {
        synchronized (managers) {
            if (managers.contains(paletteManager)) {
                Log.e(TAG, "Cannot register already registered manager");
                return;
            }
            managers.add(paletteManager);
        }
    }

    void unregisterPaletteManager(PaletteManager paletteManager) {
        synchronized (managers) {
            if (!managers.contains(paletteManager)) {
                Log.e(TAG, "Cannot unregister not yet registered manager");
                return;
            }
            managers.remove(paletteManager);
        }
    }

    boolean removeFromManagers(PaletteTask task) {
        synchronized (managers) {
            for (PaletteManager paletteManager : managers) {
                if (paletteManager.unTrack(task)) {
                    return true;
                }
            }
        }

        return false;
    }
}
