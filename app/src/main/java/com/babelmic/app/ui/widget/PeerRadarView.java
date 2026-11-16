package com.babelmic.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.babelmic.app.model.AvatarShape;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom view that renders the peer discovery field:
 *
 * <h3>Visual design</h3>
 * <ul>
 *   <li>Multiple concentric rings expand outward from the centre and fade to
 *       transparent — a classic sonar / radar wave effect.</li>
 *   <li>Three rings are staggered evenly in time so at least one is always
 *       mid-expansion.</li>
 *   <li>"You" avatar sits at the centre.</li>
 *   <li>Peer avatar icons float at their assigned positions.</li>
 *   <li>Tapping a peer selects it (purple outline); tapping again deselects.</li>
 * </ul>
 */
public class PeerRadarView extends View {

    // ─── Peer data model ─────────────────────────────────────────────────────

    public static class PeerNode {
        public final String id;
        public       String name;
        public       String flagEmoji;
        public final AvatarShape shape;
        public final int   color;
        public float nx, ny;  // normalised position in [-1, 1]

        public PeerNode(String id, String name, String flagEmoji,
                        AvatarShape shape, int color, float nx, float ny) {
            this.id        = id;
            this.name      = name;
            this.flagEmoji = flagEmoji;
            this.shape     = shape;
            this.color     = color;
            this.nx        = nx;
            this.ny        = ny;
        }
    }

    public interface OnPeerTappedListener {
        void onPeerTapped(@Nullable PeerNode peer);
    }

    // ─── Wave ring constants ──────────────────────────────────────────────────
    private static final int   RING_COUNT        = 3;
    private static final long  WAVE_CYCLE_MS     = 2400L;   // one ring full expansion
    private static final float RING_MAX_ALPHA    = 0.55f;   // opacity at start of expansion

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final List<PeerNode> peers = new ArrayList<>();
    private PeerNode selectedPeer = null;
    private OnPeerTappedListener tapListener;

    // "You" node
    private AvatarShape myShape = AvatarShape.CIRCLE;
    private int         myColor = Color.parseColor("#5B21F5");
    private String      myFlag  = "🌐";

    // Paints
    private final Paint ringPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shapePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeBgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Wave animation — single animator driving a phase value [0, 1)
    private ValueAnimator waveAnimator;
    private float         wavePhase = 0f;  // advances 0→1 each WAVE_CYCLE_MS

    // Geometry
    private float cx, cy, maxR, iconR;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public PeerRadarView(Context context) {
        super(context); init();
    }
    public PeerRadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init();
    }
    public PeerRadarView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        ringPaint.setStyle(Paint.Style.STROKE);

        badgeBgPaint.setStyle(Paint.Style.FILL);
        badgeBgPaint.setColor(Color.WHITE);

        textPaint.setTextAlign(Paint.Align.CENTER);

        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setColor(Color.parseColor("#111111"));

        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setColor(Color.parseColor("#5B21F5"));
        selectionPaint.setStrokeWidth(3f);
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void setMyAvatar(AvatarShape shape, int color, String flagEmoji) {
        myShape = shape;
        myColor = color;
        myFlag  = (flagEmoji != null) ? flagEmoji : "🌐";
        invalidate();
    }

    public void setPeers(List<PeerNode> peers) {
        this.peers.clear();
        this.peers.addAll(peers);
        selectedPeer = null;
        invalidate();
    }

    public void setOnPeerTappedListener(OnPeerTappedListener l) {
        this.tapListener = l;
    }

    public @Nullable PeerNode getSelectedPeer() { return selectedPeer; }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startWaveAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopWaveAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx    = w / 2f;
        cy    = h / 2f;
        maxR  = Math.min(w, h) * 0.46f;
        iconR = maxR * 0.14f;

        float density = getResources().getDisplayMetrics().density;
        namePaint.setTextSize(11f * density);
        textPaint.setTextSize(iconR * 1.1f);
        ringPaint.setStrokeWidth(2.5f * density);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        drawWaveRings(canvas);
        drawPeers(canvas);
        drawMyIcon(canvas);
    }

    // ─── Wave rings ───────────────────────────────────────────────────────────

    /**
     * Draw RING_COUNT rings, each staggered by 1/RING_COUNT of the cycle.
     * Ring i has phase = (wavePhase + i/RING_COUNT) mod 1.
     * Radius = phase × maxR, alpha = (1 - phase) × RING_MAX_ALPHA.
     */
    private void drawWaveRings(Canvas canvas) {
        for (int i = 0; i < RING_COUNT; i++) {
            float phase = (wavePhase + (float) i / RING_COUNT) % 1f;
            float r     = phase * maxR;
            float alpha = (1f - phase) * RING_MAX_ALPHA;

            if (r < 2f) continue; // skip tiny rings at start

            // Warm yellow ring colour matching the mockup
            int ringColor = Color.argb(
                    (int)(alpha * 255),
                    0xFF, 0xD7, 0x40);  // #FFD740

            ringPaint.setColor(ringColor);
            canvas.drawCircle(cx, cy, r, ringPaint);
        }
    }

    // ─── Peer icons ───────────────────────────────────────────────────────────

    private void drawPeers(Canvas canvas) {
        for (PeerNode peer : peers) {
            float px = cx + peer.nx * maxR * 0.75f;
            float py = cy + peer.ny * maxR * 0.75f;

            // Selection ring
            if (peer == selectedPeer) {
                canvas.drawCircle(px, py, iconR + 7f, selectionPaint);
            }

            shapePaint.setColor(peer.color);
            drawAvatarShape(canvas, peer.shape, px, py, iconR);
            drawFlagBadge(canvas, px, py, iconR, peer.flagEmoji);

            // Name label
            canvas.drawText(peer.name, px,
                    py + iconR + namePaint.getTextSize() + 4f, namePaint);
        }
    }

    // ─── My icon ─────────────────────────────────────────────────────────────

    private void drawMyIcon(Canvas canvas) {
        shapePaint.setColor(myColor);
        drawAvatarShape(canvas, myShape, cx, cy, iconR);
        drawFlagBadge(canvas, cx, cy, iconR, myFlag);
        canvas.drawText("You", cx,
                cy + iconR + namePaint.getTextSize() + 4f, namePaint);
    }

    // ─── Shape + badge helpers ────────────────────────────────────────────────

    private void drawAvatarShape(Canvas canvas, AvatarShape shape,
                                  float px, float py, float r) {
        switch (shape) {
            case CIRCLE:
                canvas.drawCircle(px, py, r, shapePaint);
                break;
            case RECTANGLE: {
                float corner = r * 0.3f;
                canvas.drawRoundRect(px - r, py - r, px + r, py + r,
                        corner, corner, shapePaint);
                break;
            }
            case HEART:
                canvas.drawPath(heartPath(px, py, r), shapePaint);
                break;
            case STAR:
            default:
                canvas.drawPath(starPath(px, py, r, 5), shapePaint);
                break;
        }
    }

    private void drawFlagBadge(Canvas canvas, float px, float py,
                                float r, String flag) {
        float badgeR = r * 0.44f;
        float bx = px + r * 0.65f;
        float by = py - r * 0.65f;
        canvas.drawCircle(bx, by, badgeR + 2f, badgeBgPaint);
        textPaint.setTextSize(badgeR * 1.3f);
        float ty = by - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(flag, bx, ty, textPaint);
    }

    // ─── Path helpers ─────────────────────────────────────────────────────────

    private static Path heartPath(float cx, float cy, float r) {
        float w = r * 2f, h = r * 2f;
        float x = cx - r, y = cy - r * 0.8f;
        Path p = new Path();
        p.moveTo(x + w/2f, y + h*0.25f);
        p.cubicTo(x+w/2f, y, x, y, x, y+h*0.25f);
        p.cubicTo(x, y+h*0.6f, x+w/2f, y+h*0.75f, x+w/2f, y+h);
        p.cubicTo(x+w/2f, y+h*0.75f, x+w, y+h*0.6f, x+w, y+h*0.25f);
        p.cubicTo(x+w, y, x+w/2f, y, x+w/2f, y+h*0.25f);
        p.close();
        return p;
    }

    private static Path starPath(float cx, float cy, float outerR, int pts) {
        float innerR = outerR * 0.42f;
        double step = Math.PI / pts, start = -Math.PI / 2;
        Path p = new Path();
        for (int i = 0; i < pts * 2; i++) {
            double angle = start + i * step;
            float  r     = (i % 2 == 0) ? outerR : innerR;
            float  x     = cx + (float)(r * Math.cos(angle));
            float  y     = cy + (float)(r * Math.sin(angle));
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    // ─── Touch ────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;
        float tx = event.getX(), ty = event.getY();
        float hitR = iconR * 2f;

        PeerNode tapped = null;
        for (PeerNode peer : peers) {
            float px = cx + peer.nx * maxR * 0.75f;
            float py = cy + peer.ny * maxR * 0.75f;
            float dx = tx - px, dy = ty - py;
            if (dx*dx + dy*dy <= hitR*hitR) { tapped = peer; break; }
        }

        selectedPeer = (tapped == selectedPeer) ? null : tapped;
        invalidate();
        performClick();
        if (tapListener != null) tapListener.onPeerTapped(selectedPeer);
        return true;
    }

    @Override public boolean performClick() { return super.performClick(); }

    // ─── Wave animation ───────────────────────────────────────────────────────

    private void startWaveAnimation() {
        if (waveAnimator != null && waveAnimator.isRunning()) return;
        waveAnimator = ValueAnimator.ofFloat(0f, 1f);
        waveAnimator.setDuration(WAVE_CYCLE_MS);
        waveAnimator.setRepeatCount(ValueAnimator.INFINITE);
        waveAnimator.setRepeatMode(ValueAnimator.RESTART);
        waveAnimator.setInterpolator(new LinearInterpolator());
        waveAnimator.addUpdateListener(anim -> {
            wavePhase = (float) anim.getAnimatedValue();
            invalidate();
        });
        waveAnimator.start();
    }

    private void stopWaveAnimation() {
        if (waveAnimator != null) {
            waveAnimator.cancel();
            waveAnimator = null;
        }
    }
}
