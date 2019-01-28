package com.baidu.tpalette;

import android.support.v4.util.LruCache;

public class PaletteColorCache {

    /**
     * 设置一个最大可存储128个RGB色值的容器
     * 注意：目前只缓存一个颜色值，后期如有需要可以缓存Palette的整个Swatch
     */
    private static LruCache<String, Integer> PALETTE_RGB_CACHE = new LruCache<>(PaletteOptions.paletteCacheSize);


    /**
     * 从缓存中获取
     *
     * @param key
     * @return
     */
    public static Integer hitPaletteCache(String key) {
        return PALETTE_RGB_CACHE.get(key);
    }

    public static void putPaletteCache(String cacheKey, Integer alphaColor) {
        PALETTE_RGB_CACHE.put(cacheKey, alphaColor);
    }
}
