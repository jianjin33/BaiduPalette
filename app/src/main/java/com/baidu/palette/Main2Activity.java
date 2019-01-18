package com.baidu.palette;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.baidu.tpalette.PaletteCallback;
import com.baidu.tpalette.TPalette;

public class Main2Activity extends AppCompatActivity {

    private View root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        root = findViewById(R.id.root);
    }

    public void start(View view) {
        String url = "http://www.ppt123.net/beijing/UploadFiles_8374/201201/2012011411274518.jpg";
        TPalette.with(this).load(url).addPaletteCallback(new PaletteCallback() {
            @Override
            public void setColor(int color, int textColor) {
                Log.d("test", "是否还会执行？");
            }
        }).into(root);
    }
}
