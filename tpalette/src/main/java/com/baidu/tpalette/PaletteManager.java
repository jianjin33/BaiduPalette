package com.baidu.tpalette;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.baidu.tpalette.lifecycle.Lifecycle;
import com.baidu.tpalette.lifecycle.LifecycleListener;
import com.baidu.tpalette.lifecycle.PaletteManagerTreeNode;
import com.baidu.tpalette.lifecycle.PaletteTracker;
import com.baidu.tpalette.lifecycle.TargetTracker;
import com.baidu.tpalette.target.TargetView;

public class PaletteManager implements LifecycleListener {

    final Lifecycle lifecycle;

    protected final TPalette tPalette;
    protected final Context context;
    private final PaletteTracker paletteTracker;
    private PaletteOptions paletteOptions;
    private final PaletteManagerTreeNode treeNode;
    private final Runnable addSelfToLifecycle = new Runnable() {
        @Override
        public void run() {
            lifecycle.addListener(PaletteManager.this);
        }
    };
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public PaletteManager(
            @NonNull TPalette tPalette, @NonNull Lifecycle lifecycle,
            @NonNull PaletteManagerTreeNode treeNode, @NonNull Context context) {
        this(
                tPalette,
                lifecycle,
                treeNode,
                new PaletteTracker(),
                context);
    }

    PaletteManager(
            TPalette tPalette,
            Lifecycle lifecycle,
            PaletteManagerTreeNode treeNode,
            PaletteTracker requestTracker,
            Context context) {
        this.tPalette = tPalette;
        this.lifecycle = lifecycle;
        this.treeNode = treeNode;
        this.paletteTracker = requestTracker;
        this.context = context;

        if (Util.isOnBackgroundThread()) {
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
        // pauseRequests();
    }

    @Override
    public void onDestroy() {
        paletteTracker.clearRequests();
        lifecycle.removeListener(this);
        mainHandler.removeCallbacks(addSelfToLifecycle);
        tPalette.unregisterPaletteManager(this);
    }

    public void resumeRequests() {
        Util.assertMainThread();
        //paletteTracker.runTask();
    }


    @NonNull
    @CheckResult
    public PaletteBuilder load(@Nullable String url) {
        return as().load(url);
    }

    @NonNull
    @CheckResult
    public PaletteBuilder load(@Nullable Bitmap bitmap) {
        return as().load(bitmap);
    }

    @NonNull
    @CheckResult
    public  PaletteBuilder as() {
        return new PaletteBuilder(context, this);
    }

/*
    public void clear(@Nullable final Target<?> target) {
        if (target == null) {
            return;
        }

        if (Util.isOnMainThread()) {
            untrackOrDelegate(target);
        } else {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    clear(target);
                }
            });
        }
    }

    private void untrackOrDelegate() {
        boolean isOwnedByUs = unTrack(target);
        if (!isOwnedByUs && !tPalette.removeFromManagers(target) && target.getRequest() != null) {
            PaletteTask task = target.getRequest();
            target.setRequest(null);
            task.clear();
        }
    }
*/

    boolean unTrack(@NonNull PaletteTask task) {
        // If the Target doesn't have a request, it's already been cleared.
        if (task == null) {
            return true;
        }

        if (paletteTracker.clearRemoveAndRecycle(task)) {
            return true;
        } else {
            return false;
        }
    }

    void track(@NonNull PaletteTask task) {
        paletteTracker.runTask(task);
    }

    @Override
    public String toString() {
        return super.toString() + "{tracker=" + paletteTracker + ", treeNode=" + treeNode + "}";
    }
}
