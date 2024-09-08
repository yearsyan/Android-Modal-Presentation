package com.example.cap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public class TransparentActivity extends FragmentActivity {

    private ImageView imageView;
    private FrameLayout container;
    private ViewGroup content;

    private Activity lastActivity;

    private final float transScale = 0.96f;
    private final float transDy = 70f;
    private final int animationDuration = 400;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        lastActivity = ((App)getApplication()).getlastResumeActivity();

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent);
        imageView = findViewById(R.id.cap_bg);
        container = findViewById(R.id.image_container);
        content = findViewById(R.id.real_content);
        getBgBitmap();
    }

    protected void getBgBitmap() {
        // 获取当前窗口
        final Window window = lastActivity.getWindow();

        // 创建目标 Bitmap，大小为整个窗口的宽高
        final Bitmap bitmap = Bitmap.createBitmap(
                window.getDecorView().getWidth(),
                window.getDecorView().getHeight(),
                Bitmap.Config.ARGB_8888);

        // 定义截图区域（整个窗口）
        final Rect rect = new Rect(0, 0, window.getDecorView().getWidth(), window.getDecorView().getHeight());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(lastActivity.getWindow(), rect, bitmap, copyResult -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    imageView.setImageBitmap(getRoundedTopCornersBitmap(bitmap, 50));
                    ShapeDrawable shapeDrawable = new ShapeDrawable(new RectShape());
                    shapeDrawable.getPaint().setColor(Color.BLACK);
                    container.setBackground(shapeDrawable);
                    imageView.post(() -> {
                        playAnimate( false, null);
                    });
                }
            }, new Handler(Looper.getMainLooper()));
        }
    }

    private void playAnimate(boolean reverse, Animator.AnimatorListener listener) {
        // 缩放动画
        float startScale = reverse ? transScale : 1;
        float endScale = reverse ? 1 : transScale;
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(imageView, "scaleX", startScale, endScale);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(imageView, "scaleY", startScale, endScale);

        // 平移动画
        int height = imageView.getHeight();
        float deltaY = height * (1 - transScale) / 2 + transDy ;
        ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(imageView, "translationY", reverse ? deltaY : 0, reverse ? 0 : deltaY);
        ObjectAnimator contentTranslationYAnimator =  ObjectAnimator.ofFloat(content, "translationY", reverse ? 0 : height, reverse ? height : 0);

        // 设置动画时长
        int duration = reverse ? (int) (animationDuration * 0.2) : animationDuration;
        scaleXAnimator.setDuration(duration);
        scaleYAnimator.setDuration(duration);
        translationYAnimator.setDuration(duration);

        // 一起启动动画
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator, translationYAnimator, contentTranslationYAnimator);
        if (listener != null) {
            animatorSet.addListener(listener);
        }
        animatorSet.start();

    }

    private static Bitmap getRoundedTopCornersBitmap(Bitmap bitmap, float cornerRadius) {
        // 创建一个与原始 Bitmap 大小相同的新 Bitmap
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // 初始化 Paint 并启用抗锯齿
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        // 定义一个与 Bitmap 相同大小的矩形
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        // 创建一个 Path，定义左上和右上为圆角，左下和右下为直角
        Path path = new Path();
        float[] radii = new float[]{
                cornerRadius, cornerRadius,
                cornerRadius, cornerRadius,
                0f, 0f, // 右下直角
                0f, 0f  // 左下直角
        };
        path.addRoundRect(rectF, radii, Path.Direction.CW);

        // 将 Path 裁剪到 Canvas
        canvas.drawARGB(0, 0, 0, 0); // 透明背景
        canvas.drawPath(path, paint);

        // 使用 SrcIn 模式来绘制 bitmap
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        canvas.drawARGB((int) (0.5 * 255), 0, 0, 0);

        return output;
    }

    @Override
    public void finish() {
        if (imageView == null) {
            super.finish();
            return;
        }
        playAnimate( true, new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                TransparentActivity.super.finish();
            }
        });
    }

    public void setRealContent(View realContent) {
        content.removeAllViews();
        content.addView(realContent);
    }

    public void setRealContent(@LayoutRes int id) {
        content.removeAllViews();
        View view = LayoutInflater.from(this).inflate(id, content, false);
        content.addView(view);
    }
}
