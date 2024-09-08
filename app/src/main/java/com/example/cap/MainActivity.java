package com.example.cap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.example.cap.pages.ImageHalfActivity;

public class MainActivity extends FragmentActivity {

    TextView mTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTv = (TextView) findViewById(R.id.text_main);
        mTv.setOnClickListener(e -> {
            Intent intent = new Intent(this, ImageHalfActivity.class);
            startActivity(intent);
        });
    }
}
