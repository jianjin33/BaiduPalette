package com.baidu.palette;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.request.RequestOptions;

import jp.wasabeef.glide.transformations.MaskTransformation;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.img);
    }

    public void load(View view) {
        String url ="http://www.ppt123.net/beijing/UploadFiles_8374/201201/2012011411274518.jpg";

//        RequestOptions option = ;
        RequestOptions option = new RequestOptions().skipMemoryCache(true);
        Glide.with(this).load(R.drawable.timg)
                .apply(option.transform(new AlbumTransformation(this,R.drawable.timg)))
                .into(imageView);
        Log.e("bitmap", "imageView宽度:" + imageView.getWidth() + "imageView高度：" + imageView.getHeight());
    }
}
