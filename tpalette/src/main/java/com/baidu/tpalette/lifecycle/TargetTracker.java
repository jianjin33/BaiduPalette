package com.baidu.tpalette.lifecycle;

import android.support.annotation.NonNull;

import com.baidu.tpalette.target.TargetView;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;


public final class TargetTracker implements LifecycleListener {

  private final Set<TargetView<?>> targets =
      Collections.newSetFromMap(new WeakHashMap<TargetView<?>, Boolean>());

  public void track(@NonNull TargetView<?> target) {
    targets.add(target);
  }

  public void unTrack(@NonNull TargetView<?> target) {
    targets.remove(target);
  }

  @Override
  public void onStart() {
    for (TargetView<?> target : targets) {
      target.onStart();
    }
  }

  @Override
  public void onStop() {
    for (TargetView<?> target : targets) {
      target.onStop();
    }
  }

  @Override
  public void onDestroy() {
    for (TargetView<?> target : targets) {
      target.onDestroy();
    }
  }

  public void clear() {
    targets.clear();
  }
}
