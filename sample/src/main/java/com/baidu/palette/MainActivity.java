package com.baidu.palette;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.img);
    }

    public void load(View view) {
        startActivity(new Intent(this, Main2Activity.class));


//        RequestOptions option = ;
       /* RequestOptions option = new RequestOptions().skipMemoryCache(true);
        Glide.with(this).load(R.drawable.timg)
                .apply(option.transform(new AlbumTransformation(this,R.drawable.timg)))
                .into(imageView);
        Log.e("bitmap", "imageView宽度:" + imageView.getWidth() + "imageView高度：" + imageView.getHeight());*/
    }
}
