package com.babelmic.app.ui.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovery radar view.
 *
 * <h3>Visuals</h3>
 * <ul>
 *   <li>3 pink/rose rings expand outward from centre (sonar wave effect).</li>
 *   <li>Your avatar sits at the centre with your identity ring colour.</li>
 *   <li>Peer avatars float at assigned positions with their ring colours.</li>
 *   <li>Tapping a peer selects it; animated chevrons (>>>) stream toward the peer's avatar.</li>
 * </ul>
 */
public class PeerRadarView extends View {

    // ─── Peer data model ─────────────────────────────────────────────────────

    public static class PeerNode {
        public final String id;
        public       String name;
        public       String flagEmoji;
        public final int    color;        // identity ring colour
        public       int    avatarResId;  // animal avatar drawable res
        public float nx, ny;             // normalised position [-1, 1]

        public PeerNode(String id, String name, String flagEmoji,
                        int color, int avatarResId, float nx, float ny) {
            this.id          = id;
            this.name        = name;
            this.flagEmoji   = flagEmoji;
            this.color       = color;
            this.avatarResId = avatarResId;
            this.nx          = nx;
            this.ny          = ny;
        }
    }

    public interface OnPeerTappedListener {
        void onPeerTapped(@Nullable PeerNode peer);
    }

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final int   RING_COUNT     = 3;
    private static final long  WAVE_CYCLE_MS  = 2400L;
    private static final float RING_MAX_ALPHA = 0.45f;
    private static final int   WAVE_COLOR     = 0xFFFF2D78; // pink

    // ─── Fields ───────────────────────────────────────────────────────────────
    private final List<PeerNode> peers = new ArrayList<>();
    private PeerNode selectedPeer = null;
    private OnPeerTappedListener tapListener;

    // My avatar
    private int    myAvatarResId = -1;
    private int    myRingColor   = 0xFFFF2D78;
    private String myFlag        = "🌐";

    // Paints
    private final Paint wavePaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgePaint     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint    = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Chevron animation
    private float chevronPhase = 0f;
    private ValueAnimator chevronAnimator;

    // Wave animation
    private float wavePhase = 0f;
    private ValueAnimator waveAnimator;

    // Bitmap cache for avatar images
    private final Map<Integer, Bitmap> bitmapCache = new HashMap<>();

    // Geometry
    private float cx, cy, maxR, iconR;

    // ─── Constructors ─────────────────────────────────────────────────────────
    public PeerRadarView(Context c) { super(c); init(); }
    public PeerRadarView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public PeerRadarView(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        wavePaint.setStyle(Paint.Style.STROKE);

        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(6f);

        selectionPaint.setStyle(Paint.Style.STROKE);
        selectionPaint.setColor(Color.parseColor("#FF2D78"));
        selectionPaint.setStrokeWidth(6f);

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.parseColor("#111111"));

        badgePaint.setTextAlign(Paint.Align.CENTER);

        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setColor(Color.WHITE);
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void setMyAvatar(int avatarResId, int ringColor, String flagEmoji) {
        myAvatarResId = avatarResId;
        myRingColor   = ringColor;
        myFlag        = (flagEmoji != null) ? flagEmoji : "🌐";
        invalidate();
    }

    public void setPeers(List<PeerNode> peers) {
        this.peers.clear();
        this.peers.addAll(peers);
        selectedPeer = null;
        invalidate();
    }

    public void setOnPeerTappedListener(OnPeerTappedListener l) { tapListener = l; }
    public @Nullable PeerNode getSelectedPeer() { return selectedPeer; }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startWaveAnimation();
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopWaveAnimation();
        stopChevronAnimation();
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        cx    = w / 2f;
        cy    = h / 2f;
        maxR  = Math.min(w, h) * 0.44f;
        iconR = maxR * 0.14f;

        float density = getResources().getDisplayMetrics().density;
        textPaint.setTextSize(11f * density);
        badgePaint.setTextSize(iconR * 0.8f);
        wavePaint.setStrokeWidth(2.5f * density);
        bitmapCache.clear(); // geometry changed — regenerate at new size
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        drawWaveRings(canvas);
        drawPeers(canvas);
        drawMyAvatar(canvas);
        if (selectedPeer != null) drawChevrons(canvas);
    }

    // ─── Wave rings ───────────────────────────────────────────────────────────

    private void drawWaveRings(Canvas canvas) {
        for (int i = 0; i < RING_COUNT; i++) {
            float phase = (wavePhase + (float) i / RING_COUNT) % 1f;
            float r     = phase * maxR;
            float alpha = (1f - phase) * RING_MAX_ALPHA;
            if (r < 2f) continue;
            wavePaint.setColor(Color.argb((int)(alpha * 255),
                    0xFF, 0x2D, 0x78)); // #FF2D78 pink
            canvas.drawCircle(cx, cy, r, wavePaint);
        }
    }

    // ─── Peer avatars ─────────────────────────────────────────────────────────

    private void drawPeers(Canvas canvas) {
        for (PeerNode peer : peers) {
            float px = cx + peer.nx * maxR * 0.75f;
            float py = cy + peer.ny * maxR * 0.75f;

            drawAvatarCircle(canvas, px, py, peer.avatarResId, peer.color,
                    peer == selectedPeer);

            // Name label
            canvas.drawText(peer.name, px, py + iconR + textPaint.getTextSize() + 4f, textPaint);
        }
    }

    private void drawMyAvatar(Canvas canvas) {
        drawAvatarCircle(canvas, cx, cy, myAvatarResId, myRingColor, false);
        canvas.drawText("You", cx, cy + iconR + textPaint.getTextSize() + 4f, textPaint);
    }

    private void drawAvatarCircle(Canvas canvas, float px, float py,
                                   int avatarResId, int ringColor, boolean isSelected) {
        // White circle background
        canvas.drawCircle(px, py, iconR, circlePaint);

        // Avatar bitmap clipped to circle
        if (avatarResId > 0) {
            Bitmap bmp = getCircleBitmap(avatarResId, (int)(iconR * 1.8f));
            if (bmp != null) {
                canvas.drawBitmap(bmp, px - iconR * 0.9f, py - iconR * 0.9f, null);
            }
        }

        // Identity ring
        ringPaint.setColor(ringColor);
        canvas.drawCircle(px, py, iconR + 3f, ringPaint);

        // Selection ring (selected peer gets an extra outer ring)
        if (isSelected) {
            canvas.drawCircle(px, py, iconR + 9f, selectionPaint);
        }
    }

    // ─── Chevron animation (from my avatar toward selected peer) ─────────────

    private void drawChevrons(Canvas canvas) {
        if (selectedPeer == null) return;
        float px = cx + selectedPeer.nx * maxR * 0.75f;
        float py = cy + selectedPeer.ny * maxR * 0.75f;

        // Direction vector from my avatar to peer
        float dx = px - cx, dy = py - cy;
        float dist = (float) Math.sqrt(dx*dx + dy*dy);
        if (dist < 1f) return;
        float ux = dx / dist, uy = dy / dist;

        // Draw 3 chevron characters spaced along the path
        Paint chvPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        chvPaint.setColor(0xFFFF2D78);
        chvPaint.setTextAlign(Paint.Align.CENTER);
        chvPaint.setTextSize(iconR * 0.7f);
        chvPaint.setAlpha(255);

        float startDist = iconR + 14f;
        float endDist   = dist - iconR - 14f;
        if (endDist <= startDist) return;
        float span = endDist - startDist;

        for (int i = 0; i < 3; i++) {
            // Each chevron travels independently through [0, span]
            float t = ((chevronPhase + i / 3f) % 1f);
            float d = startDist + t * span;

            float chvX = cx + ux * d;
            float chvY = cy + uy * d;

            // Fade out near peer
            float fade = t > 0.75f ? (1f - (t - 0.75f) * 4f) : 1f;
            chvPaint.setAlpha((int)(fade * 255));

            canvas.drawText("›", chvX, chvY + chvPaint.getTextSize() / 3f, chvPaint);
        }
    }

    // ─── Bitmap helpers ───────────────────────────────────────────────────────

    private Bitmap getCircleBitmap(int resId, int size) {
        if (bitmapCache.containsKey(resId)) return bitmapCache.get(resId);
        try {
            Bitmap src = BitmapFactory.decodeResource(getResources(), resId);
            if (src == null) return null;
            Bitmap scaled = Bitmap.createScaledBitmap(src, size, size, true);
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(output);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            c.drawOval(new RectF(0, 0, size, size), p);
            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            c.drawBitmap(scaled, 0, 0, p);
            bitmapCache.put(resId, output);
            return output;
        } catch (Exception e) { return null; }
    }

    // ─── Touch ────────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() != MotionEvent.ACTION_DOWN) return true;
        float tx = event.getX(), ty = event.getY();
        float hitR = iconR * 2.2f;

        PeerNode tapped = null;
        for (PeerNode peer : peers) {
            float px = cx + peer.nx * maxR * 0.75f;
            float py = cy + peer.ny * maxR * 0.75f;
            float dx = tx - px, dy = ty - py;
            if (dx*dx + dy*dy <= hitR*hitR) { tapped = peer; break; }
        }

        PeerNode prev = selectedPeer;
        selectedPeer = (tapped == selectedPeer) ? null : tapped;

        if (selectedPeer != null && selectedPeer != prev) {
            startChevronAnimation();
        } else if (selectedPeer == null) {
            stopChevronAnimation();
        }

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
        waveAnimator.addUpdateListener(a -> { wavePhase = (float) a.getAnimatedValue(); invalidate(); });
        waveAnimator.start();
    }

    private void stopWaveAnimation() {
        if (waveAnimator != null) { waveAnimator.cancel(); waveAnimator = null; }
    }

    // ─── Chevron animation ────────────────────────────────────────────────────

    private void startChevronAnimation() {
        stopChevronAnimation();
        chevronAnimator = ValueAnimator.ofFloat(0f, 1f);
        chevronAnimator.setDuration(900);
        chevronAnimator.setRepeatCount(ValueAnimator.INFINITE);
        chevronAnimator.setRepeatMode(ValueAnimator.RESTART);
        chevronAnimator.setInterpolator(new LinearInterpolator());
        chevronAnimator.addUpdateListener(a -> { chevronPhase = (float) a.getAnimatedValue(); invalidate(); });
        chevronAnimator.start();
    }

    private void stopChevronAnimation() {
        if (chevronAnimator != null) { chevronAnimator.cancel(); chevronAnimator = null; }
        chevronPhase = 0f;
    }
}
