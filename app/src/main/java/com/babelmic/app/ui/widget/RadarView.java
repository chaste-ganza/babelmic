package com.babelmic.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A circular radar/sonar view that sweeps a rotating gradient arc to signal
 * active peer discovery.
 *
 * <p>The radar consists of:
 * <ul>
 *   <li>Concentric circle grid (3 rings)</li>
 *   <li>A faint cross-hair</li>
 *   <li>A rotating sweep arc — a {@link SweepGradient} from the primary colour
 *       fading to transparent over ~90°</li>
 * </ul>
 * </p>
 *
 * <p>Call {@link #startScan()} / {@link #stopScan()} to control animation.
 * When stopped the radar dims to an idle state.</p>
 */
public class RadarView extends View {

    private static final long SWEEP_DURATION_MS = 2000;

    // ─── Paints ───────────────────────────────────────────────────────────────
    private final Paint ringPaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ─── State ────────────────────────────────────────────────────────────────
    private float   sweepAngle   = 0f;   // current rotation in degrees
    private boolean scanning     = false;
    private int     primaryColor = Color.parseColor("#1A73E8");

    private ValueAnimator sweepAnimator;

    // Detected peer blip positions (angle in degrees, 0 = top)
    private float[] blipAngles    = new float[0];
    private float[] blipDistances = new float[0]; // 0.0 = centre, 1.0 = edge

    // ─── Constructors ─────────────────────────────────────────────────────────

    public RadarView(Context context) {
        super(context); init();
    }
    public RadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init();
    }
    public RadarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setColor(Color.parseColor("#2A1A73E8"));
        ringPaint.setStrokeWidth(1.5f);

        crossPaint.setStyle(Paint.Style.STROKE);
        crossPaint.setColor(Color.parseColor("#1A1A73E8"));
        crossPaint.setStrokeWidth(1f);

        sweepPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.parseColor("#EA4335"));
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void setPrimaryColor(int color) {
        this.primaryColor = color;
        invalidate();
    }

    public void startScan() {
        if (scanning) return;
        scanning = true;
        sweepAnimator = ValueAnimator.ofFloat(0f, 360f);
        sweepAnimator.setDuration(SWEEP_DURATION_MS);
        sweepAnimator.setRepeatCount(ValueAnimator.INFINITE);
        sweepAnimator.setRepeatMode(ValueAnimator.RESTART);
        sweepAnimator.setInterpolator(new LinearInterpolator());
        sweepAnimator.addUpdateListener(anim -> {
            sweepAngle = (float) anim.getAnimatedValue();
            invalidate();
        });
        sweepAnimator.start();
    }

    public void stopScan() {
        scanning = false;
        if (sweepAnimator != null) {
            sweepAnimator.cancel();
            sweepAnimator = null;
        }
        invalidate();
    }

    /**
     * Place blip indicators for detected peers.
     *
     * @param angles    Angle from top (0°=top, 90°=right) for each peer
     * @param distances Normalised distance from centre [0, 1] for each peer
     */
    public void setPeerBlips(float[] angles, float[] distances) {
        this.blipAngles    = angles    != null ? angles    : new float[0];
        this.blipDistances = distances != null ? distances : new float[0];
        invalidate();
    }

    public boolean isScanning() { return scanning; }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth()  / 2f;
        float cy = getHeight() / 2f;
        float r  = Math.min(cx, cy) - 8f;

        drawRings(canvas, cx, cy, r);
        drawCrossHair(canvas, cx, cy, r);
        if (scanning) drawSweep(canvas, cx, cy, r);
        drawBlips(canvas, cx, cy, r);
    }

    private void drawRings(Canvas canvas, float cx, float cy, float r) {
        for (int i = 1; i <= 3; i++) {
            float rr = r * i / 3f;
            canvas.drawCircle(cx, cy, rr, ringPaint);
        }
    }

    private void drawCrossHair(Canvas canvas, float cx, float cy, float r) {
        canvas.drawLine(cx - r, cy, cx + r, cy, crossPaint);
        canvas.drawLine(cx, cy - r, cx, cy + r, crossPaint);
    }

    private void drawSweep(Canvas canvas, float cx, float cy, float r) {
        // Build a sweep gradient centred at (cx, cy)
        // Colour is primaryColor (opaque) → transparent over ~90 degrees
        int colourFull = (primaryColor & 0x00FFFFFF) | 0x99000000; // 60% opacity
        int colourFade = (primaryColor & 0x00FFFFFF) | 0x00000000; // transparent

        SweepGradient sg = new SweepGradient(cx, cy,
                new int[]{colourFade, colourFade, colourFull, colourFade},
                new float[]{0f, 0.65f, 0.98f, 1f});
        sweepPaint.setShader(sg);

        canvas.save();
        canvas.rotate(sweepAngle, cx, cy);
        canvas.drawCircle(cx, cy, r, sweepPaint);
        canvas.restore();

        // Leading edge — a bright thin line
        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor((primaryColor & 0x00FFFFFF) | 0xCC000000);
        linePaint.setStrokeWidth(2f);
        linePaint.setStyle(Paint.Style.STROKE);
        double rad = Math.toRadians(sweepAngle - 90); // -90 so 0° = top
        float ex = cx + r * (float) Math.cos(rad);
        float ey = cy + r * (float) Math.sin(rad);
        canvas.drawLine(cx, cy, ex, ey, linePaint);
    }

    private void drawBlips(Canvas canvas, float cx, float cy, float r) {
        float blipR = 7f * getResources().getDisplayMetrics().density;
        for (int i = 0; i < blipAngles.length && i < blipDistances.length; i++) {
            double rad = Math.toRadians(blipAngles[i] - 90);
            float dist = blipDistances[i] * r * 0.85f;
            float bx = cx + dist * (float) Math.cos(rad);
            float by = cy + dist * (float) Math.sin(rad);

            // Outer glow
            Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            RadialGradient glow = new RadialGradient(bx, by, blipR * 2.5f,
                    new int[]{0x44EA4335, 0x00EA4335},
                    null, Shader.TileMode.CLAMP);
            glowPaint.setShader(glow);
            canvas.drawCircle(bx, by, blipR * 2.5f, glowPaint);

            // Core dot
            canvas.drawCircle(bx, by, blipR, dotPaint);
        }
    }
}
