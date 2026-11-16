package com.babelmic.app.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.babelmic.app.model.AvatarShape;

/**
 * A small composite avatar icon used on the discovery map and connection screens.
 *
 * <p>Renders two layers:
 * <ol>
 *   <li>A filled shape (heart / star / circle / rectangle) in the user's colour.</li>
 *   <li>A circular flag emoji badge in the top-right corner.</li>
 * </ol>
 * </p>
 *
 * <p>The shape occupies ~75% of the view area, leaving room for the badge to
 * overlap in the top-right corner without being clipped.</p>
 */
public class AvatarIconView extends View {

    // ─── State ────────────────────────────────────────────────────────────────
    private AvatarShape shape    = AvatarShape.CIRCLE;
    private int         color    = Color.parseColor("#5B21F5");
    private String      flagEmoji = "🌐";

    // ─── Paints ───────────────────────────────────────────────────────────────
    private final Paint shapePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeBg     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint badgeText   = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ─── Constructors ─────────────────────────────────────────────────────────

    public AvatarIconView(Context context) {
        super(context); init();
    }
    public AvatarIconView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init();
    }
    public AvatarIconView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        shapePaint.setStyle(Paint.Style.FILL);
        badgeBg.setStyle(Paint.Style.FILL);
        badgeBg.setColor(Color.WHITE);
        badgeText.setTextAlign(Paint.Align.CENTER);
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Configure the avatar appearance.
     *
     * @param shape     Shape enum
     * @param color     Fill colour (ARGB int)
     * @param flagEmoji The flag emoji string (e.g. "🇫🇷")
     */
    public void setAvatar(AvatarShape shape, int color, String flagEmoji) {
        this.shape     = shape;
        this.color     = color;
        this.flagEmoji = (flagEmoji != null && !flagEmoji.isEmpty()) ? flagEmoji : "🌐";
        shapePaint.setColor(color);
        invalidate();
    }

    public void setFlagEmoji(String flag) {
        this.flagEmoji = (flag != null && !flag.isEmpty()) ? flag : "🌐";
        invalidate();
    }

    // ─── Drawing ──────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float badgeR   = w * 0.22f;         // badge radius
        float shapeSize = w * 0.70f;         // shape bounding box
        float shapeLeft = 0f;
        float shapeTop  = h - shapeSize;    // anchor shape to bottom-left

        drawShape(canvas, shapeLeft, shapeTop, shapeSize);
        drawBadge(canvas, w - badgeR, badgeR, badgeR);
    }

    private void drawShape(Canvas canvas, float left, float top, float size) {
        float cx = left + size / 2f;
        float cy = top  + size / 2f;
        float r  = size / 2f;

        switch (shape) {
            case CIRCLE:
                canvas.drawCircle(cx, cy, r, shapePaint);
                break;

            case RECTANGLE:
                canvas.drawRoundRect(left, top, left + size, top + size,
                        size * 0.15f, size * 0.15f, shapePaint);
                break;

            case HEART:
                canvas.drawPath(heartPath(cx, cy, r), shapePaint);
                break;

            case STAR:
            default:
                canvas.drawPath(starPath(cx, cy, r, 5), shapePaint);
                break;
        }
    }

    private void drawBadge(Canvas canvas, float cx, float cy, float r) {
        // White circle background
        canvas.drawCircle(cx, cy, r + 2f, badgeBg);

        // Flag emoji centred in the badge
        float textSize = r * 1.4f;
        badgeText.setTextSize(textSize);
        float textY = cy - (badgeText.descent() + badgeText.ascent()) / 2f;
        canvas.drawText(flagEmoji, cx, textY, badgeText);
    }

    // ─── Shape path helpers ───────────────────────────────────────────────────

    private static Path heartPath(float cx, float cy, float r) {
        Path p = new Path();
        float w = r * 2f;
        float h = r * 2f;
        float x = cx - r;
        float y = cy - r * 0.8f;

        p.moveTo(x + w / 2f, y + h * 0.25f);
        // Left lobe
        p.cubicTo(x + w / 2f, y,
                  x,          y,
                  x,          y + h * 0.25f);
        // Left bottom curve to tip
        p.cubicTo(x,          y + h * 0.6f,
                  x + w / 2f, y + h * 0.75f,
                  x + w / 2f, y + h);
        // Right bottom curve from tip
        p.cubicTo(x + w / 2f, y + h * 0.75f,
                  x + w,      y + h * 0.6f,
                  x + w,      y + h * 0.25f);
        // Right lobe
        p.cubicTo(x + w,      y,
                  x + w / 2f, y,
                  x + w / 2f, y + h * 0.25f);
        p.close();
        return p;
    }

    private static Path starPath(float cx, float cy, float outerR, int points) {
        Path p = new Path();
        float innerR = outerR * 0.42f;
        double step = Math.PI / points;
        double start = -Math.PI / 2;

        for (int i = 0; i < points * 2; i++) {
            double angle = start + i * step;
            float r = (i % 2 == 0) ? outerR : innerR;
            float x = cx + (float)(r * Math.cos(angle));
            float y = cy + (float)(r * Math.sin(angle));
            if (i == 0) p.moveTo(x, y);
            else        p.lineTo(x, y);
        }
        p.close();
        return p;
    }
}
