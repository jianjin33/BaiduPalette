package com.baidu.tpalette.lifecycle;

import android.support.annotation.NonNull;

/**
 * @author JianZuming
 * @description
 * @date 2019/1/17 对Application管理的具体实现
 */
class ApplicationLifecycle implements Lifecycle {
    @Override
    public void addListener(@NonNull LifecycleListener listener) {
        listener.onStart();
    }

    @Override
    public void removeListener(@NonNull LifecycleListener listener) {
    }
}
