package com.babelmic.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Free-form audio wave visualizer — a row of vertical rounded bars that react
 * to amplitude. No shape envelope; bars are symmetric about the horizontal centre.
 *
 * <h3>States</h3>
 * <ul>
 *   <li>IDLE — bars at minimum height, gently pulsing</li>
 *   <li>SPEAKING_LOCAL — bars driven by real mic RMS via {@link #setAmplitude}</li>
 *   <li>SPEAKING_PEER  — bars driven by a smooth oscillator while TTS plays</li>
 * </ul>
 *
 * Bar colour is set via {@link #setColor} and matches the speaker's identity colour.
 */
public class WaveVisualizerView extends View {

    public enum State { IDLE, SPEAKING_LOCAL, SPEAKING_PEER }

    private static final int   BAR_COUNT   = 18;
    private static final float MIN_FRAC    = 0.06f;  // min bar height as fraction of view height
    private static final float MAX_FRAC    = 0.90f;  // max bar height
    private static final float BAR_W_RATIO = 0.030f; // bar width as fraction of view width
    private static final float GAP_RATIO   = 0.022f;

    private State state  = State.IDLE;
    private int   color  = Color.parseColor("#4ADE80");

    private final float[] targetH  = new float[BAR_COUNT]; // target height fractions [0,1]
    private final float[] displayH = new float[BAR_COUNT]; // smoothed display fractions
    // Per-bar phase offsets for the peer oscillator
    private final float[] barPhase = new float[BAR_COUNT];

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Idle pulse animator
    private ValueAnimator idleAnimator;
    private float          idlePulse = MIN_FRAC;

    // Peer oscillator animator
    private ValueAnimator peerAnimator;
    private float          peerPhase = 0f;

    private float barW, gap, startX, cy, maxHalfH;

    // ─── Constructors ─────────────────────────────────────────────────────────
    public WaveVisualizerView(Context c) { super(c); init(); }
    public WaveVisualizerView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public WaveVisualizerView(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        barPaint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < BAR_COUNT; i++) barPhase[i] = (float) i / BAR_COUNT;
        startIdlePulse();
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void setColor(int color) {
        this.color = color;
        barPaint.setColor(color);
        invalidate();
    }

    public void setState(State newState) {
        if (this.state == newState) return;
        this.state = newState;
        switch (newState) {
            case IDLE:
                stopPeerOscillator();
                animateToMin();
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

    /** Feed mic RMS value (roughly -2 to 10 dB) — only effective in SPEAKING_LOCAL. */
    public void setAmplitude(float rmsDb) {
        if (state != State.SPEAKING_LOCAL) return;
        float norm = Math.max(0f, Math.min(rmsDb, 10f)) / 10f;
        norm = 0.08f + norm * 0.92f;
        // Add natural variation per bar
        for (int i = 0; i < BAR_COUNT; i++) {
            float variation = (float)(0.6f + 0.4f * Math.sin(i * 1.3 + System.currentTimeMillis() * 0.003));
            targetH[i] = norm * variation;
        }
        postInvalidateOnAnimation();
    }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        barW     = w * BAR_W_RATIO;
        gap      = w * GAP_RATIO;
        float total = BAR_COUNT * barW + (BAR_COUNT - 1) * gap;
        startX   = (w - total) / 2f;
        cy       = h / 2f;
        maxHalfH = h * MAX_FRAC / 2f;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        barPaint.setColor(color);
        smoothHeights();

        for (int i = 0; i < BAR_COUNT; i++) {
            float halfH = Math.max(displayH[i] * maxHalfH, barW / 2f);
            float left  = startX + i * (barW + gap);
            float right = left + barW;
            float top   = cy - halfH;
            float bot   = cy + halfH;
            float r     = barW / 2f;
            canvas.drawRoundRect(new RectF(left, top, right, bot), r, r, barPaint);
        }
        postInvalidateOnAnimation();
    }

    private void smoothHeights() {
        for (int i = 0; i < BAR_COUNT; i++) {
            displayH[i] += (targetH[i] - displayH[i]) * 0.22f;
        }
    }

    // ─── Idle pulse ───────────────────────────────────────────────────────────

    private void startIdlePulse() {
        if (idleAnimator != null && idleAnimator.isRunning()) return;
        idleAnimator = ValueAnimator.ofFloat(MIN_FRAC, MIN_FRAC * 2.5f);
        idleAnimator.setDuration(1200);
        idleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        idleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        idleAnimator.addUpdateListener(a -> {
            idlePulse = (float) a.getAnimatedValue();
            for (int i = 0; i < BAR_COUNT; i++) targetH[i] = idlePulse;
            postInvalidateOnAnimation();
        });
        idleAnimator.start();
    }

    private void stopIdlePulse() {
        if (idleAnimator != null) { idleAnimator.cancel(); idleAnimator = null; }
    }

    private void animateToMin() {
        for (int i = 0; i < BAR_COUNT; i++) targetH[i] = MIN_FRAC;
    }

    // ─── Peer oscillator ──────────────────────────────────────────────────────

    private void startPeerOscillator() {
        if (peerAnimator != null && peerAnimator.isRunning()) return;
        peerAnimator = ValueAnimator.ofFloat(0f, (float)(Math.PI * 2));
        peerAnimator.setDuration(1000);
        peerAnimator.setRepeatCount(ValueAnimator.INFINITE);
        peerAnimator.setRepeatMode(ValueAnimator.RESTART);
        peerAnimator.setInterpolator(new LinearInterpolator());
        peerAnimator.addUpdateListener(a -> {
            float phase = (float) a.getAnimatedValue();
            for (int i = 0; i < BAR_COUNT; i++) {
                float amp = 0.35f + 0.55f * (float) Math.abs(Math.sin(phase + barPhase[i] * Math.PI * 2));
                targetH[i] = amp;
            }
            postInvalidateOnAnimation();
        });
        peerAnimator.start();
    }

    private void stopPeerOscillator() {
        if (peerAnimator != null) { peerAnimator.cancel(); peerAnimator = null; }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopIdlePulse();
        stopPeerOscillator();
    }
}
