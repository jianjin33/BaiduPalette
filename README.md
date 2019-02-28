# BaiduPalette
针对Google Palette的封装

#### 背景
google发布的`palette`的取色框架，已经完成对图片取色的需求。
但是取色过程是个耗时操作，必须放在子线程中异步执行，尤其是处理较大的`bitmap`，耗时会更长，
一般可通过`Handler`向`MainThread`发消息，通知刷新UI，对获取的色值进行相应控件的着色等操作。

既然有异步，就会涉及到生命周期以及内存泄漏等问题的存在，
如果在项目中一个或几个固定的地方使用取色，暂可直接使用`palette`框架中的异步回调，
注意处理好内存泄漏的问题即可，但是一个项目中多处使用取色，还是需要对其进行封装或写个工具类，
让每一处取色的代码更为简洁和优雅，便于维护。本篇即是对其的封装。

#### 介绍
本取色框架的封装最主要是分享一下实现思路，仅供参考，仅供参考，仅供参考，
不要直接添加依赖来使用，内部对外暴露的接口可能并不能满足项目需求，还需要一些修改。

使用方式1：
```
 TPalette.with(this)    // 传入Activity/Fragment/Context
         .load(bitmap)  // 传入bitmap对象或者图片url地址（url方式暂时无效）
         .addPaletteCallback(new PaletteCallback() {
            @Override
            public void setColor(int color, int textColor) {
                // 取得色值之后的回调
            }
        }).into(root);  // 传入View对象，对改View设置背景
 ```

使用方式2：
 ```
 TPalette.with(this)    // 传入Activity/Fragment/Context
         .load(bitmap)  // 传入bitmap对象或者图片url地址（url方式暂时无效）
         .addPaletteCallback(new PaletteCallback() {
            @Override
            public void setColor(int color, int textColor) {
                // 取得色值之后的回调
            }
        }).submit();
```

记得添加依赖
```
    implementation 'com.github.jianjin33:BaiduPalette:0.0.3'
    implementation 'com.android.support:palette-v7:28.0.0'
```

解释说明：
1. 以上的代码中`with`传入的`Activity/Fragment/Context`对象，是为了控制取色异步任务的生命周期；
2. `load`传入`bitmap`对象或图片url,传入url可对其进行网络图片下载，
至于如何下载可使用各大图片框架进行下载或自己实现图片下载，这个框架中没添加下载图片这部分内容。
（额外说下，其实可以提供一个下载接口，让使用该框架的使用者去自己实现下载，
这样虽然增加了框架拓展性，但同时又会增加了使用复杂度）；
3. `addPaletteCallback`添加取色回调，这里只有两个颜色，可能不符合业务需求，
需要自己在框架中确定回调颜色的策略，Palette有7种色值集合（鲜明的，暗淡的，色值占比最多的等）
每种又包含背景色、标题字体色、内容字体色等。这些都需要自己去根据需求做调整；
4. `into`传入view没什么可说的，包括`submit`就是一个任务的开始方法；
5. 本框架的思路是来源于Glide图片加载框架，其中对异步的生命周期的控制，完全参考Glide来写的，
也是对Glide学习之后的一种使用方式。
