package com.babelmic.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Draws three animated › chevrons that stream from left to right.
 * Used in the Pending/Incoming bottom sheets.
 * Flip with scaleX="-1" for the incoming (right→left) direction.
 */
public class ChevronArrowView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float phase = 0f;
    private ValueAnimator animator;

    public ChevronArrowView(Context c) { super(c); init(); }
    public ChevronArrowView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public ChevronArrowView(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        paint.setColor(0xFFFF2D78);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(22f);
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(900);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); invalidate(); });
        animator.start();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (animator != null) animator.cancel();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        float w = getWidth(), h = getHeight();
        float cy = h / 2f + paint.getTextSize() / 3f;

        for (int i = 0; i < 3; i++) {
            float t     = (phase + i / 3f) % 1f;
            float x     = t * w;
            float fade  = t > 0.7f ? (1f - (t - 0.7f) / 0.3f) : 1f;
            paint.setAlpha((int)(fade * 255));
            canvas.drawText("›", x, cy, paint);
        }
    }
}
