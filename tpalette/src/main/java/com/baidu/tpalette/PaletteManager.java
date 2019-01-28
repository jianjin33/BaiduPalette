package com.baidu.tpalette;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.baidu.tpalette.lifecycle.Lifecycle;
import com.baidu.tpalette.lifecycle.LifecycleListener;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class PaletteManager implements LifecycleListener {

    final Lifecycle lifecycle;

    protected final TPalette tPalette;
    private final Set<PaletteTask> tasks = Collections.newSetFromMap(new WeakHashMap<PaletteTask, Boolean>());
    private final Runnable addSelfToLifecycle = new Runnable() {
        @Override
        public void run() {
            lifecycle.addListener(PaletteManager.this);
        }
    };
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PaletteManager(@NonNull TPalette tPalette, @NonNull Lifecycle lifecycle) {
        this.tPalette = tPalette;
        this.lifecycle = lifecycle;

        if (Utils.isOnBackgroundThread()) {
            mainHandler.post(addSelfToLifecycle);
        } else {
            lifecycle.addListener(this);
        }

        tPalette.registerPaletteManager(this);
    }

    @Override
    public void onStart() {
        resumeRequests();
    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {
        clearTasks();
        lifecycle.removeListener(this);
        mainHandler.removeCallbacks(addSelfToLifecycle);
        tPalette.unregisterPaletteManager(this);
    }

    public void resumeRequests() {
        Utils.assertMainThread();
    }

    @NonNull
    @Deprecated
    public PaletteBuilder load(@Nullable String url) {
        return as().load(url);
    }

    @NonNull
    public PaletteBuilder load(@Nullable Bitmap bitmap) {
        return as().load(bitmap);
    }

    @NonNull
    private PaletteBuilder as() {
        return new PaletteBuilder(this);
    }

    /**
     * 开始执行异步任务
     * @param task
     */
    void track(@NonNull PaletteTask task) {
        tasks.add(task);
        task.start();
    }

    boolean unTrack(PaletteTask task){
        if (clearRemoveAndRecycle(task,false)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 移除所有异步任务
     */
    private void clearTasks() {
        for (PaletteTask task : Utils.getSnapshot(tasks)) {
            clearRemoveAndRecycle(task, true);
        }
    }

    private boolean clearRemoveAndRecycle(@Nullable PaletteTask task, boolean isSafeToRecycle) {
        if (task == null) {
            return true;
        }
        boolean isOwnedByUs = tasks.remove(task);
        if (isOwnedByUs) {
            if (isSafeToRecycle) {
                task.recycle();
            }
        }
        return isOwnedByUs;
    }

    public static class PaletteBuilder {
        private Object object;
        private PaletteManager paletteManager;;
        private PaletteCallback callback;

        public PaletteBuilder(PaletteManager paletteManager) {
            this.paletteManager = paletteManager;
        }

        public PaletteBuilder load(@Nullable Object object) {
            this.object = object;
            return this;
        }

        public PaletteBuilder addPaletteCallback(@Nullable PaletteCallback callback) {
            this.callback = callback;
            return this;
        }

        public void submit() {
            into(null);
        }

        public void into(@Nullable View view) {
            PaletteTask paletteTask = new PaletteTask(object, view, callback);
            paletteManager.track(paletteTask);
        }
    }
}
