package com.baidu.tpalette.lifecycle;

import android.support.annotation.NonNull;

/**
 * @author JianZuming
 * @description 添加和移除对Activity、Fragment、Application监听回调
 * @date 2019/1/17
 */
public interface Lifecycle {

    void addListener(@NonNull LifecycleListener listener);

    void removeListener(@NonNull LifecycleListener listener);
}
