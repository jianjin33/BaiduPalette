package com.baidu.tpalette.lifecycle;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.baidu.tpalette.PaletteTask;
import com.bumptech.glide.util.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class PaletteTracker {

    private final Set<PaletteTask> tasks =
            Collections.newSetFromMap(new WeakHashMap<PaletteTask, Boolean>());
    private final List<PaletteTask> pendingRequests = new ArrayList<>();
    private boolean isPaused;

    public void runTask(@NonNull PaletteTask task) {
        tasks.add(task);
        if (!isPaused) {
            task.start();
        } else {
            pendingRequests.add(task);
        }
    }

    @VisibleForTesting
    void addTask(PaletteTask task) {
        tasks.add(task);
    }

    public boolean clearRemoveAndRecycle(@Nullable PaletteTask task) {
        return clearRemoveAndMaybeRecycle(task, /*isSafeToRecycle=*/ true);
    }

    private boolean clearRemoveAndMaybeRecycle(@Nullable PaletteTask task, boolean isSafeToRecycle) {
        if (task == null) {
            return true;
        }
        boolean isOwnedByUs = tasks.remove(task);
        isOwnedByUs = pendingRequests.remove(task) || isOwnedByUs;
        if (isOwnedByUs) {
            if (isSafeToRecycle) {
                task.recycle();
            }
        }
        return isOwnedByUs;
    }

    public void clearTasks() {
        for (PaletteTask task : Util.getSnapshot(tasks)) {
            clearRemoveAndMaybeRecycle(task,true);
        }
        pendingRequests.clear();
    }

    public void restartTask() {
       /* for (PaletteTask task : tasks) {
            if (!task.isComplete() && !task.isCancelled()) {
                // Ensure the request will be restarted in onResume.
                if (!isPaused) {
                    task.start();
                } else {
                    pendingRequests.add(task);
                }
            }
        }*/
    }

    @Override
    public String toString() {
        return super.toString() + "{numRequests=" + tasks.size() + ", isPaused=" + isPaused + "}";
    }
}
