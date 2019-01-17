package com.baidu.tpalette.lifecycle;

import android.support.annotation.NonNull;


import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;


class ActivityFragmentLifecycle implements Lifecycle {
    private final Set<LifecycleListener> lifecycleListenerListeners =
            Collections.newSetFromMap(new WeakHashMap<LifecycleListener, Boolean>());
    private boolean isStarted;
    private boolean isDestroyed;

    @Override
    public void addListener(@NonNull LifecycleListener listener) {
        lifecycleListenerListeners.add(listener);

        if (isDestroyed) {
            listener.onDestroy();
        } else if (isStarted) {
            listener.onStart();
        } else {
            listener.onStop();
        }
    }

    @Override
    public void removeListener(@NonNull LifecycleListener listener) {
        lifecycleListenerListeners.remove(listener);
    }

    void onStart() {
        isStarted = true;
        for (LifecycleListener lifecycleListenerListener : lifecycleListenerListeners) {
            lifecycleListenerListener.onStart();
        }
    }

    void onStop() {
        isStarted = false;
        for (LifecycleListener lifecycleListenerListener : lifecycleListenerListeners) {
            lifecycleListenerListener.onStop();
        }
    }

    void onDestroy() {
        isDestroyed = true;
        for (LifecycleListener lifecycleListenerListener : lifecycleListenerListeners) {
            lifecycleListenerListener.onDestroy();
        }
    }
}
