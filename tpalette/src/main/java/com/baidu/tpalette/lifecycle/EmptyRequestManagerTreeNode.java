package com.baidu.tpalette.lifecycle;

import android.support.annotation.NonNull;

import com.baidu.tpalette.PaletteManager;
import java.util.Collections;
import java.util.Set;

final class EmptyRequestManagerTreeNode implements PaletteManagerTreeNode {
    @NonNull
    @Override
    public Set<PaletteManager> getDescendants() {
        return Collections.emptySet();
    }
}
