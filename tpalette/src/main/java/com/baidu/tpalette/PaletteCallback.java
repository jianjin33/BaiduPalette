package com.baidu.tpalette;

import android.support.annotation.ColorInt;

public interface PaletteCallback {
    void setColor(@ColorInt int color, @ColorInt int textColor);
}
