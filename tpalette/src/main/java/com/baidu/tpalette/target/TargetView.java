package com.baidu.tpalette.target;

import android.support.annotation.Nullable;

import com.baidu.tpalette.PaletteTask;
import com.baidu.tpalette.lifecycle.LifecycleListener;

public interface TargetView<V> extends LifecycleListener {
    void onBegin(int defaultColor);

    void onSuccess(int color, int textColor);

    void onFailed(int defaultColor);

    void setRequest(@Nullable PaletteTask request);

    PaletteTask getRequest();
}
