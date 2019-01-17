package com.baidu.tpalette.target;

import android.support.annotation.Nullable;

import com.baidu.tpalette.PaletteTask;

public class SimpleTarget implements TargetView {

    @Override
    public void onBegin(int defaultColor) {

    }

    @Override
    public void onSuccess(int color, int textColor) {

    }

    @Override
    public void onFailed(int defaultColor) {

    }

    @Override
    public void setRequest(@Nullable PaletteTask request) {

    }

    @Override
    public PaletteTask getRequest() {
        return null;
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {

    }
}
