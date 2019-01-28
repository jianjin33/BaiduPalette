package com.baidu.tpalette.lifecycle;

/**
 * @author JianZuming
 * @description 通过Fragment或Activity的生命周期来控制取色异步任务生命周期。
 * @date 2019/1/17
 */
public interface LifecycleListener {
    /**
     * 对应{@link android.app.Fragment#onStart()}
     * {@link android.app.Activity#onStart()}
     */
    void onStart();

    /**
     * 对应{@link android.app.Fragment#onStop()}
     * {@link android.app.Activity#onStop()}}
     */
    void onStop();

    /**
     * 对应{@link android.app.Fragment#onDestroy()}
     * {@link android.app.Activity#onDestroy()}
     */
    void onDestroy();
}
