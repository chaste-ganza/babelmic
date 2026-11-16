package com.babelmic.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.babelmic.app.model.AvatarShape;

/**
 * Custom view that renders an avatar as a set of vertical rounded bars whose
 * heights follow a shape envelope (heart, star, circle, rectangle).
 *
 * <h3>States</h3>
 * <ul>
 *   <li><b>IDLE</b> — all bars collapsed to small grey dots at the centre line,
 *       gently pulsing to signal "ready".</li>
 *   <li><b>SPEAKING_LOCAL</b> — bars animate to the shape envelope, heights
 *       driven by real microphone RMS amplitude via {@link #setAmplitude(float)}.</li>
 *   <li><b>SPEAKING_PEER</b> — same animation driven by a simulated oscillator
 *       that is started/stopped by TTS playback callbacks.</li>
 * </ul>
 *
 * <h3>Customisation</h3>
 * Call {@link #setAvatar(AvatarShape, int, int)} to configure shape and colours.
 * The view draws a vertical linear gradient from {@code colorTop} to
 * {@code colorBottom} across the bars when active.
 *
 * <h3>Thread safety</h3>
 * All public setters are safe to call from any thread — they post to the UI
 * thread internally.
 */
public class AvatarVisualizerView extends View {

    // ─── State ────────────────────────────────────────────────────────────────

    public enum VisualizerState {
        IDLE,
        SPEAKING_LOCAL,
        SPEAKING_PEER
    }

    // ─── Constants ────────────────────────────────────────────────────────────

    private static final int   BAR_COUNT       = ShapeEnvelopeCalculator.BAR_COUNT;
    private static final float BAR_WIDTH_RATIO = 0.045f;  // bar width as fraction of view width
    private static final float GAP_RATIO       = 0.025f;  // gap between bars as fraction of width
    private static final float DOT_RADIUS      = 6f;      // idle dot radius in dp
    private static final float IDLE_PULSE_MIN  = 0.6f;
    private static final float IDLE_PULSE_MAX  = 1.0f;
    private static final long  IDLE_PULSE_MS   = 1400;
    private static final long  AMPLITUDE_SMOOTH_MS = 80;  // smoothing window

    // ─── Fields ───────────────────────────────────────────────────────────────

    private VisualizerState state = VisualizerState.IDLE;

    // Avatar config
    private AvatarShape shape      = AvatarShape.CIRCLE;
    private int         colorTop   = Color.parseColor("#1A73E8");
    private int         colorBottom= Color.parseColor("#4FC3F7");
    private float[]     envelope   = ShapeEnvelopeCalculator.compute(AvatarShape.CIRCLE);

    // Current displayed bar heights [0..1], smoothed
    private final float[] displayHeights = new float[BAR_COUNT];

    // Target bar heights [0..1] — set by amplitude or oscillator
    private final float[] targetHeights  = new float[BAR_COUNT];

    // Paints
    private final Paint barPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Idle pulse animator
    private ValueAnimator idleAnimator;
    private float          idlePulse = IDLE_PULSE_MIN;

    // Peer oscillator animator
    private ValueAnimator peerAnimator;

    // Amplitude smoothing
    private float smoothedAmplitude = 0f;

    // Geometry — computed in onSizeChanged
    private float barWidth, gap, totalBarsWidth, startX, centerY;
    private float maxHalfHeight;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public AvatarVisualizerView(Context context) {
        super(context);
        init();
    }

    public AvatarVisualizerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AvatarVisualizerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dotPaint.setColor(Color.parseColor("#BDBDBD"));
        dotPaint.setStyle(Paint.Style.FILL);
        barPaint.setStyle(Paint.Style.FILL);
        startIdlePulse();
        // Kick the draw loop
        postInvalidateOnAnimation();
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Configure the avatar shape and colours.
     *
     * @param shape      Shape enum value
     * @param colorTop   ARGB colour at the top of the bars
     * @param colorBottom ARGB colour at the bottom of the bars
     */
    public void setAvatar(AvatarShape shape, int colorTop, int colorBottom) {
        this.shape       = shape;
        this.colorTop    = colorTop;
        this.colorBottom = colorBottom;
        this.envelope    = ShapeEnvelopeCalculator.compute(shape);
        invalidateShader();
        postInvalidate();
    }

    /**
     * Set the current state.
     * Switching to IDLE stops animations and collapses bars.
     * Switching to SPEAKING_PEER starts the oscillator.
     * Switching to SPEAKING_LOCAL stops the oscillator (amplitude drives it instead).
     */
    public void setState(VisualizerState newState) {
        if (this.state == newState) return;
        this.state = newState;

        switch (newState) {
            case IDLE:
                stopPeerOscillator();
                animateToIdle();
                startIdlePulse();
                break;

            case SPEAKING_LOCAL:
                stopIdlePulse();
                stopPeerOscillator();
                break;

            case SPEAKING_PEER:
                stopIdlePulse();
                startPeerOscillator();
                break;
        }
    }

    /**
     * Feed a raw RMS amplitude value from {@code onRmsChanged}.
     * Android reports dB in roughly [-2, 10] range; we normalise to [0, 1].
     * Only meaningful in {@link VisualizerState#SPEAKING_LOCAL}.
     *
     * @param rmsDb Value from {@link android.speech.RecognitionListener#onRmsChanged}
     */
    public void setAmplitude(float rmsDb) {
        if (state != VisualizerState.SPEAKING_LOCAL) return;

        // Normalise: clamp to [0, 10], map to [0.1, 1.0] so there's always a bit of movement
        float normalised = Math.max(0f, Math.min(rmsDb, 10f)) / 10f;
        normalised = 0.1f + normalised * 0.9f;

        // Exponential moving average for smoothing
        smoothedAmplitude = smoothedAmplitude * 0.6f + normalised * 0.4f;

        // Apply to target heights using the shape envelope
        for (int i = 0; i < BAR_COUNT; i++) {
            targetHeights[i] = envelope[i] * smoothedAmplitude;
        }
        postInvalidateOnAnimation();
    }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        computeGeometry(w, h);
        invalidateShader();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        smoothHeights();

        boolean anyActive = false;
        for (float v : displayHeights) {
            if (v > 0.01f) { anyActive = true; break; }
        }

        if (!anyActive || state == VisualizerState.IDLE) {
            drawIdleDots(canvas);
        } else {
            drawBars(canvas);
        }

        // Keep animating
        postInvalidateOnAnimation();
    }

    private void drawIdleDots(Canvas canvas) {
        float dotR = DOT_RADIUS * getResources().getDisplayMetrics().density * idlePulse;
        for (int i = 0; i < BAR_COUNT; i++) {
            float cx = startX + i * (barWidth + gap) + barWidth / 2f;
            canvas.drawCircle(cx, centerY, dotR, dotPaint);
        }
    }

    private void drawBars(Canvas canvas) {
        for (int i = 0; i < BAR_COUNT; i++) {
            float halfH = displayHeights[i] * maxHalfHeight;
            if (halfH < DOT_RADIUS) halfH = DOT_RADIUS; // minimum dot size

            float left   = startX + i * (barWidth + gap);
            float right  = left + barWidth;
            float top    = centerY - halfH;
            float bottom = centerY + halfH;
            float radius = barWidth / 2f;

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, radius, radius, barPaint);
        }
    }

    // ─── Geometry ─────────────────────────────────────────────────────────────

    private void computeGeometry(int w, int h) {
        barWidth        = w * BAR_WIDTH_RATIO;
        gap             = w * GAP_RATIO;
        totalBarsWidth  = BAR_COUNT * barWidth + (BAR_COUNT - 1) * gap;
        startX          = (w - totalBarsWidth) / 2f;
        centerY         = h / 2f;
        maxHalfHeight   = h * 0.46f; // leave 4% padding top and bottom
    }

    private void invalidateShader() {
        if (getHeight() == 0) return;
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, getHeight(),
                colorTop, colorBottom,
                Shader.TileMode.CLAMP);
        barPaint.setShader(gradient);
    }

    // ─── Smooth height interpolation ──────────────────────────────────────────

    private void smoothHeights() {
        float factor = 0.25f; // approach speed per frame
        for (int i = 0; i < BAR_COUNT; i++) {
            displayHeights[i] += (targetHeights[i] - displayHeights[i]) * factor;
        }
    }

    // ─── Idle pulse animator ──────────────────────────────────────────────────

    private void startIdlePulse() {
        if (idleAnimator != null && idleAnimator.isRunning()) return;
        idleAnimator = ValueAnimator.ofFloat(IDLE_PULSE_MIN, IDLE_PULSE_MAX);
        idleAnimator.setDuration(IDLE_PULSE_MS);
        idleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        idleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        idleAnimator.setInterpolator(new DecelerateInterpolator());
        idleAnimator.addUpdateListener(anim -> {
            idlePulse = (float) anim.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        idleAnimator.start();
    }

    private void stopIdlePulse() {
        if (idleAnimator != null) {
            idleAnimator.cancel();
            idleAnimator = null;
        }
        idlePulse = 1f;
    }

    private void animateToIdle() {
        // Smoothly collapse all bars to zero
        for (int i = 0; i < BAR_COUNT; i++) {
            targetHeights[i] = 0f;
        }
        smoothedAmplitude = 0f;
    }

    // ─── Peer oscillator ──────────────────────────────────────────────────────

    /**
     * Drives the peer avatar bars with a natural-looking breathing oscillation
     * while TTS audio is playing — no actual amplitude data needed.
     */
    private void startPeerOscillator() {
        if (peerAnimator != null && peerAnimator.isRunning()) return;
        peerAnimator = ValueAnimator.ofFloat(0f, (float) (Math.PI * 2));
        peerAnimator.setDuration(1200);
        peerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        peerAnimator.setRepeatMode(ValueAnimator.RESTART);
        peerAnimator.addUpdateListener(anim -> {
            float phase = (float) anim.getAnimatedValue();
            for (int i = 0; i < BAR_COUNT; i++) {
                // Each bar gets a slightly different phase offset for a wave effect
                double offset = phase + (i * Math.PI / BAR_COUNT);
                float amp = 0.45f + 0.45f * (float) Math.sin(offset);
                targetHeights[i] = envelope[i] * amp;
            }
            postInvalidateOnAnimation();
        });
        peerAnimator.start();
    }

    private void stopPeerOscillator() {
        if (peerAnimator != null) {
            peerAnimator.cancel();
            peerAnimator = null;
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopIdlePulse();
        stopPeerOscillator();
    }
}
