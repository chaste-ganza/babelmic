package com.babelmic.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
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
 * - Two soft concentric glowing rings (warm yellow/orange gradient)
 * - "You" avatar icon pinned at the centre
 * - Peer avatar icons floating at defined positions
 * - Tap a peer to select it (highlighted ring); tap again or tap empty to deselect
 *
 * <p>All drawing is done on Canvas. Peer positions are expressed as
 * normalised (x, y) coordinates in [-1, 1] relative to the view centre.</p>
 */
public class PeerRadarView extends View {

    // ─── Peer data model ─────────────────────────────────────────────────────

    public static class PeerNode {
        public final String id;
        public final String name;
        public final String flagEmoji;
        public final AvatarShape shape;
        public final int   color;
        /** Normalised position: -1=left/top, 0=centre, 1=right/bottom */
        public float nx, ny;

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
        /** @param peer  The peer that was tapped, or null if selection cleared. */
        void onPeerTapped(@Nullable PeerNode peer);
    }

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final List<PeerNode> peers = new ArrayList<>();
    private PeerNode selectedPeer = null;
    private OnPeerTappedListener tapListener;

    // "You" node
    private AvatarShape myShape    = AvatarShape.CIRCLE;
    private int         myColor    = Color.parseColor("#5B21F5");
    private String      myFlag     = "🌐";

    // Paints
    private final Paint ringPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shapePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeBgPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint namePaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Pulse animation
    private float pulseScale = 1f;
    private ValueAnimator pulseAnimator;

    // Geometry
    private float cx, cy, outerR, innerR;
    private float iconR;    // radius of each avatar icon circle

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
        badgeBgPaint.setStyle(Paint.Style.FILL);
        badgeBgPaint.setColor(Color.WHITE);

        textPaint.setTextAlign(Paint.Align.CENTER);

        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setColor(Color.parseColor("#111111"));

        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setColor(Color.parseColor("#5B21F5"));
        selectionPaint.setStrokeWidth(3f);

        startPulse();
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
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx      = w / 2f;
        cy      = h / 2f;
        outerR  = Math.min(w, h) * 0.46f;
        innerR  = outerR * 0.52f;
        iconR   = outerR * 0.13f;

        float density = getResources().getDisplayMetrics().density;
        namePaint.setTextSize(11f * density);
        textPaint.setTextSize(iconR * 1.1f);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        drawRings(canvas);
        drawPeers(canvas);
        drawMyIcon(canvas);
    }

    // ─── Ring drawing ─────────────────────────────────────────────────────────

    private void drawRings(Canvas canvas) {
        // Outer glow ring
        RadialGradient outerGrad = new RadialGradient(cx, cy,
                outerR * pulseScale,
                new int[]{0x00FFE082, 0x33FFE082, 0x66FFF3CD, 0xAAFFEB00},
                new float[]{0f, 0.3f, 0.65f, 1f},
                Shader.TileMode.CLAMP);
        ringPaint.setShader(outerGrad);
        canvas.drawCircle(cx, cy, outerR * pulseScale, ringPaint);

        // Inner glow ring
        RadialGradient innerGrad = new RadialGradient(cx, cy,
                innerR,
                new int[]{0x00FFD740, 0x55FFD740, 0xBBFFEB00},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);
        ringPaint.setShader(innerGrad);
        canvas.drawCircle(cx, cy, innerR, ringPaint);
    }

    // ─── Peer icons ───────────────────────────────────────────────────────────

    private void drawPeers(Canvas canvas) {
        for (PeerNode peer : peers) {
            float px = cx + peer.nx * outerR * 0.75f;
            float py = cy + peer.ny * outerR * 0.75f;

            boolean isSelected = peer == selectedPeer;

            // Selection ring
            if (isSelected) {
                canvas.drawCircle(px, py, iconR + 6f, selectionPaint);
            }

            // Shape fill
            shapePaint.setColor(peer.color);
            drawAvatarShape(canvas, peer.shape, px, py, iconR);

            // Flag badge (top-right)
            float badgeR = iconR * 0.45f;
            float bx = px + iconR * 0.65f;
            float by = py - iconR * 0.65f;
            canvas.drawCircle(bx, by, badgeR + 2f, badgeBgPaint);
            textPaint.setTextSize(badgeR * 1.3f);
            float ty = by - (textPaint.descent() + textPaint.ascent()) / 2f;
            canvas.drawText(peer.flagEmoji, bx, ty, textPaint);

            // Name label below icon
            float nameY = py + iconR + namePaint.getTextSize() + 4f;
            canvas.drawText(peer.name, px, nameY, namePaint);
        }
    }

    // ─── My icon (centre) ─────────────────────────────────────────────────────

    private void drawMyIcon(Canvas canvas) {
        shapePaint.setColor(myColor);
        drawAvatarShape(canvas, myShape, cx, cy, iconR);

        // Flag badge
        float badgeR = iconR * 0.45f;
        float bx = cx + iconR * 0.65f;
        float by = cy - iconR * 0.65f;
        canvas.drawCircle(bx, by, badgeR + 2f, badgeBgPaint);
        textPaint.setTextSize(badgeR * 1.3f);
        float ty = by - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(myFlag, bx, ty, textPaint);

        // "You" label
        canvas.drawText("You", cx, cy + iconR + namePaint.getTextSize() + 4f, namePaint);
    }

    // ─── Shape drawing ────────────────────────────────────────────────────────

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

    private static Path heartPath(float cx, float cy, float r) {
        float w = r * 2f, h = r * 2f;
        float x = cx - r, y = cy - r * 0.8f;
        Path p = new Path();
        p.moveTo(x + w / 2f, y + h * 0.25f);
        p.cubicTo(x + w/2f, y, x, y, x, y + h * 0.25f);
        p.cubicTo(x, y + h*0.6f, x + w/2f, y + h*0.75f, x + w/2f, y + h);
        p.cubicTo(x + w/2f, y + h*0.75f, x+w, y + h*0.6f, x+w, y + h*0.25f);
        p.cubicTo(x+w, y, x + w/2f, y, x + w/2f, y + h*0.25f);
        p.close();
        return p;
    }

    private static Path starPath(float cx, float cy, float outerR, int pts) {
        float innerR = outerR * 0.42f;
        double step = Math.PI / pts, start = -Math.PI / 2;
        Path p = new Path();
        for (int i = 0; i < pts * 2; i++) {
            double angle = start + i * step;
            float r = (i % 2 == 0) ? outerR : innerR;
            float x = cx + (float)(r * Math.cos(angle));
            float y = cy + (float)(r * Math.sin(angle));
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
        float hitR = iconR * 2f; // generous hit area

        PeerNode tapped = null;
        for (PeerNode peer : peers) {
            float px = cx + peer.nx * outerR * 0.75f;
            float py = cy + peer.ny * outerR * 0.75f;
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

    // ─── Pulse animation ─────────────────────────────────────────────────────

    private void startPulse() {
        pulseAnimator = ValueAnimator.ofFloat(0.95f, 1.05f);
        pulseAnimator.setDuration(2000);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new LinearInterpolator());
        pulseAnimator.addUpdateListener(a -> {
            pulseScale = (float) a.getAnimatedValue();
            invalidate();
        });
        pulseAnimator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }
}
