package com.baidu.tpalette.lifecycle;

import android.support.annotation.NonNull;


class ApplicationLifecycle implements Lifecycle {
  @Override
  public void addListener(@NonNull LifecycleListener listener) {
    listener.onStart();
  }

  @Override
  public void removeListener(@NonNull LifecycleListener listener) {
  }
}
