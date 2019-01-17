package com.baidu.tpalette.lifecycle;

import android.support.annotation.NonNull;

import com.baidu.tpalette.PaletteManager;

import java.util.Set;

/**
 * Provides access to the relatives of a PaletteManager based on the current context. The context
 * hierarchy is provided by nesting in Activity and Fragments; the application context does not
 * provide access to any other RequestManagers hierarchically.
 */
public interface PaletteManagerTreeNode {

  @NonNull
  Set<PaletteManager> getDescendants();
}
