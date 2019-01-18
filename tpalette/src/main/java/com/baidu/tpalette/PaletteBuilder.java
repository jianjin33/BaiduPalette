package com.baidu.tpalette;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.View;

public class PaletteBuilder {
    private Object object;
    private Context context;
    private PaletteManager paletteManager;;
    private PaletteCallback callback;

    public PaletteBuilder(Context context,PaletteManager paletteManager) {
        this.context = context;
        this.paletteManager = paletteManager;
    }

    public PaletteBuilder load(@Nullable Object object) {
        this.object = object;
        return this;
    }

    public PaletteBuilder addPaletteCallback(@Nullable PaletteCallback callback) {
        this.callback = callback;
        return this;
    }

    public void into(@Nullable View view) {
        PaletteTask paletteTask = new PaletteTask(context, object, view, callback);
        paletteManager.track(paletteTask);
    }

}
