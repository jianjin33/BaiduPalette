package com.baidu.palette;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.timg);
        TPalette.with(this).load(bitmap).addPaletteCallback(new PaletteCallback() {
            @Override
            public void setColor(int color, int textColor) {
            }
        }).into(root);
    }
}
