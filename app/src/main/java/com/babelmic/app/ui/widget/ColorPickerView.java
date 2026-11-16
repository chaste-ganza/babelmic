package com.babelmic.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A horizontal row of colour swatches the user taps to select a colour.
 *
 * <p>Renders a fixed palette as circular swatches. The selected swatch
 * gets a white ring highlight. Fires {@link OnColorSelectedListener}
 * when the selection changes.</p>
 */
public class ColorPickerView extends View {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    // ─── Default palette ─────────────────────────────────────────────────────
    private static final int[] PALETTE = {
        Color.parseColor("#EA4335"), // Google Red
        Color.parseColor("#FF6D00"), // Deep Orange
        Color.parseColor("#FBBC05"), // Google Yellow
        Color.parseColor("#34A853"), // Google Green
        Color.parseColor("#00BCD4"), // Teal
        Color.parseColor("#1A73E8"), // Google Blue
        Color.parseColor("#7C4DFF"), // Deep Purple
        Color.parseColor("#E91E63"), // Pink
        Color.parseColor("#FF4081"), // Pink accent
        Color.parseColor("#FFFFFF"), // White
        Color.parseColor("#212121"), // Near black
    };

    private int[] palette = PALETTE;
    private int   selectedIndex = 5; // default: blue

    private final Paint swatchPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    private OnColorSelectedListener listener;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public ColorPickerView(Context context) {
        super(context); init();
    }
    public ColorPickerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init();
    }
    public ColorPickerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.WHITE);
        ringPaint.setStrokeWidth(4f);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.parseColor("#E0E0E0"));
        borderPaint.setStrokeWidth(1.5f);
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void setOnColorSelectedListener(OnColorSelectedListener l) {
        this.listener = l;
    }

    /** Pre-select a colour by value. Finds the closest match in the palette. */
    public void setSelectedColor(int color) {
        for (int i = 0; i < palette.length; i++) {
            if (palette[i] == color) { selectedIndex = i; invalidate(); return; }
        }
        // No exact match — just leave current selection
    }

    /** @return The currently selected colour. */
    public int getSelectedColor() {
        return palette[selectedIndex];
    }

    // ─── Measurement ─────────────────────────────────────────────────────────

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredH = (int) (52 * getResources().getDisplayMetrics().density);
        int h = resolveSize(desiredH, heightMeasureSpec);
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), h);
    }

    // ─── Drawing ─────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (getWidth() == 0 || getHeight() == 0) return;

        float radius   = getHeight() / 2f - 6f;
        float diameter = radius * 2f;
        float total    = palette.length * diameter + (palette.length - 1) * 8f;
        float startX   = (getWidth() - total) / 2f;
        float cy       = getHeight() / 2f;

        for (int i = 0; i < palette.length; i++) {
            float cx = startX + i * (diameter + 8f) + radius;

            swatchPaint.setColor(palette[i]);
            canvas.drawCircle(cx, cy, radius, swatchPaint);

            // Border for light colours
            if (isLight(palette[i])) {
                canvas.drawCircle(cx, cy, radius - 1f, borderPaint);
            }

            // Selection ring
            if (i == selectedIndex) {
                canvas.drawCircle(cx, cy, radius + 4f, ringPaint);
            }
        }
    }

    // ─── Touch ────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;

        float radius   = getHeight() / 2f - 6f;
        float diameter = radius * 2f;
        float total    = palette.length * diameter + (palette.length - 1) * 8f;
        float startX   = (getWidth() - total) / 2f;
        float cy       = getHeight() / 2f;
        float tx       = event.getX();
        float ty       = event.getY();

        for (int i = 0; i < palette.length; i++) {
            float cx = startX + i * (diameter + 8f) + radius;
            float dx = tx - cx;
            float dy = ty - cy;
            if (dx * dx + dy * dy <= (radius + 8f) * (radius + 8f)) {
                selectedIndex = i;
                invalidate();
                if (listener != null) listener.onColorSelected(palette[i]);
                performClick();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static boolean isLight(int color) {
        // Perceived luminance formula
        double r = Color.red(color)   / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color)  / 255.0;
        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return luminance > 0.7;
    }
}
