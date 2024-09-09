package com.example.cap;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.PixelCopy;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class TransparentActivity extends FragmentActivity {

    private ImageView mBackgroundImageView;
    private FrameLayout mRootContainer;
    private ViewGroup mContent;
    private View mBackgroundMask;

    private Activity mLaunchActivity;

    private TransparentConfig mConfig;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        mLaunchActivity = ((App)getApplication()).getlastResumeActivity();
        mConfig = new TransparentConfig(this);

        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);

        window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transparent);
        mBackgroundImageView = findViewById(R.id.cap_bg);
        mRootContainer = findViewById(R.id.image_container);
        mContent = findViewById(R.id.real_content);
        mBackgroundMask = findViewById(R.id.transparent_mask);
        initEvent();
        setupBackground();
    }

    private void initEvent() {
        if (mConfig.enableBgCancelable()) {
            mBackgroundMask.setOnClickListener(l -> {
                finish();
            });
        }
    }

    protected void setupBackground() {
        ColorDrawable colorDrawable = new ColorDrawable(Color.argb(128, 0, 0, 0));
        mBackgroundMask.setBackground(colorDrawable);
        if (mConfig.enableScaleBackground()) {
            captureLastActivity(bitmap -> {
                if (bitmap == null) {
                    return;
                }
                mBackgroundImageView.setImageBitmap(bitmap);
                ShapeDrawable shapeDrawable = new ShapeDrawable(new RectShape());
                shapeDrawable.getPaint().setColor(Color.BLACK);
                mRootContainer.setBackground(shapeDrawable);
                mBackgroundImageView.post(() -> {
                    playScaleAnimate( false, null);
                });
            });
        } else {
            mBackgroundImageView.setVisibility(View.GONE);
            mBackgroundMask.post(() -> {
                playNormalAnimate(false, null);
            });
        }
    }

    private void captureLastActivity(@NonNull CaptureCallback callback) {
        final Window window = mLaunchActivity.getWindow();
        final View decor = window.getDecorView();

        Bitmap bitmap = Bitmap.createBitmap(decor.getWidth(), decor.getHeight(), Bitmap.Config.ARGB_8888);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final Rect rect = new Rect(0, 0, window.getDecorView().getWidth(), window.getDecorView().getHeight());
            PixelCopy.request(mLaunchActivity.getWindow(), rect, bitmap, copyResult -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    callback.callback(getRoundedTopCornersBitmap(bitmap, mConfig.getBackgroundCorner()));
                } else {
                    callback.callback(null);
                }
            }, new Handler(Looper.getMainLooper()));
        } else {
            Canvas canvas = new Canvas(bitmap);
            decor.draw(canvas);
            callback.callback(getRoundedTopCornersBitmap(bitmap, mConfig.getBackgroundCorner()));
        }
    }

    private void playNormalAnimate(boolean reverse, Animator.AnimatorListener listener) {
        int height = mBackgroundMask.getHeight();
        ObjectAnimator contentTranslationYAnimator =  ObjectAnimator.ofFloat(mContent, "translationY", reverse ? 0 : height, reverse ? height : 0);
        contentTranslationYAnimator.setDuration(reverse ? mConfig.getExitAnimationDuration() : mConfig.getEnterAnimationDuration());
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(contentTranslationYAnimator);
        if (listener != null) {
            animatorSet.addListener(listener);
        }
        animatorSet.start();
    }

    private void playScaleAnimate(boolean reverse, Animator.AnimatorListener listener) {
        int duration = reverse ? mConfig.getExitAnimationDuration() : mConfig.getEnterAnimationDuration();

        if (duration == 0) {
            listener.onAnimationEnd(new ValueAnimator());
            return;
        }

        // 缩放动画
        float startScale = reverse ? mConfig.getTransScale() : 1;
        float endScale = reverse ? 1 : mConfig.getTransScale();
        ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(mBackgroundImageView, "scaleX", startScale, endScale);
        ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(mBackgroundImageView, "scaleY", startScale, endScale);

        // 平移动画
        int height = mBackgroundImageView.getHeight();
        float deltaY = height * (1 - mConfig.getTransScale()) / 2 + mConfig.getTransDy();
        ObjectAnimator translationYAnimator = ObjectAnimator.ofFloat(mBackgroundImageView, "translationY", reverse ? deltaY : 0, reverse ? 0 : deltaY);
        ObjectAnimator contentTranslationYAnimator =  ObjectAnimator.ofFloat(mContent, "translationY", reverse ? 0 : height, reverse ? height : 0);
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

        return output;
    }

    @Override
    public void finish() {
        if (mBackgroundImageView == null) {
            super.finish();
            return;
        }
        if (mConfig.shouldHideMaskOnFinish()) {
            mBackgroundMask.setVisibility(View.INVISIBLE);
        }
        AnimatorListenerAdapter listenerAdapter = new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!mConfig.shouldHideMaskOnFinish()) {
                    mBackgroundMask.setVisibility(View.INVISIBLE);
                }
                super.onAnimationEnd(animation);
                TransparentActivity.super.finish();
            }
        };
        if (mConfig.enableScaleBackground()) {
            playScaleAnimate( true, listenerAdapter);
        } else {
            playNormalAnimate( true, listenerAdapter);
        }
    }

    public void setRealContent(View realContent) {
        mContent.removeAllViews();
        mContent.addView(realContent);
    }

    public void setRealContent(@LayoutRes int id) {
        mContent.removeAllViews();
        View view = LayoutInflater.from(this).inflate(id, mContent, false);
        mContent.addView(view);
    }

    interface CaptureCallback {
        void callback(@Nullable Bitmap bitmap);
    }

    public static class TransparentConfig {
        private boolean mEnableBgCancelable = false;
        private boolean mEnableScaleBackground = true;
        private float mTransScale = 0.96f;
        private float mTransDy = 70;
        private int mEnterAnimationDuration = 300;
        private int mExitAnimationDuration = 100;
        private int mBackgroundCorner = 50;
        private boolean mHideMaskOnFinish = false;

        public TransparentConfig(TransparentActivity activity) {
            try {
                String configJson = activity.getIntent().getStringExtra("animate_config");
                Uri uri = activity.getIntent().getData();
                if (uri != null && !TextUtils.isEmpty(configJson)) {
                    configJson = uri.getQueryParameter("animate_config");
                }
                if (configJson == null || TextUtils.isEmpty(configJson)) {
                    configJson = "{}";
                }
                JSONObject config = new JSONObject(configJson);
                mEnableBgCancelable = config.optBoolean("bg_cancelable");
                mEnableScaleBackground = config.optBoolean("scalable_bg", true);
                mTransScale = (float) config.optDouble("scale_radio", 0.96);
                mTransDy = config.optInt("dy", 70);
                mEnterAnimationDuration = config.optInt("enter_duration", 300);
                mExitAnimationDuration = config.optInt("exit_duration", 100);
                mBackgroundCorner = config.optInt("corner", 50);
                mHideMaskOnFinish = config.optBoolean("hide_mask_on_finish", mEnableScaleBackground);
            } catch (JSONException | RuntimeException e) {
                e.printStackTrace();
            }
        }

        public boolean enableBgCancelable() {
            return mEnableBgCancelable;
        }

        public boolean enableScaleBackground() {
            return mEnableScaleBackground;
        }

        public float getTransScale() {
            return mTransScale;
        }

        public float getTransDy() {
            return mTransDy;
        }

        public int getEnterAnimationDuration() {
            return mEnterAnimationDuration;
        }

        public int getExitAnimationDuration() {
            return mExitAnimationDuration;
        }

        public int getBackgroundCorner() {
            return mBackgroundCorner;
        }

        public boolean shouldHideMaskOnFinish() {
            return mHideMaskOnFinish;
        }
    }
}
