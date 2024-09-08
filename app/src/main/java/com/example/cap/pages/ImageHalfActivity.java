package com.example.cap.pages;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.example.cap.R;
import com.example.cap.TransparentActivity;

public class ImageHalfActivity extends TransparentActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRealContent(R.layout.image_content);
        imageView = findViewById(R.id.img_content);
        Glide.with(this)
                .load("https://i0.hdslb.com/bfs/bangumi/image/96f9aeb74c9646c318f25bba798462061bd800d7.png")
                .fitCenter()
                .override(getResources().getDisplayMetrics().widthPixels, Target.SIZE_ORIGINAL)
                .into(imageView);
        imageView.setOnClickListener(l -> {
            Intent intent = new Intent(this, ImageHalfActivity.class);
            startActivity(intent);
        });

    }
}
