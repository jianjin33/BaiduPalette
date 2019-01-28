package com.baidu.tpalette;

public class PaletteOptions {
    /**
     * 控制取色对bitmap的压缩程度，越大则取色耗时越长，30*30的取色耗时约为200ms左右
     */
    public static final int PALETTE_DEFAULT_RESIZE_BITMAP_AREA = 30 * 30;

    /**
     * 取色在内存中缓存的大小，不宜过大。
     */
    public static int paletteCacheSize = 128;
}
