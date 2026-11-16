package com.babelmic.app.ui.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.babelmic.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Spotify-lyrics-style subtitle view for the conversation screen.
 *
 * <h3>Normal state</h3>
 * Shows only the current exchange — large centred text that fades in.
 * Two lines:
 * <ol>
 *   <li>Original spoken text (smaller, secondary colour)</li>
 *   <li>Translated text (larger, primary colour)</li>
 * </ol>
 *
 * <h3>Expanded state</h3>
 * User taps the view → it expands to show a scrollable history of all
 * previous exchanges, one per line, oldest at top. Tap again to collapse.
 *
 * <h3>Usage</h3>
 * <pre>
 *   subtitleView.showExchange("Bonjour !", "Hello!");   // new exchange arrives
 *   subtitleView.showPartial("Bon…");                   // live STT partial
 * </pre>
 */
public class SubtitleView extends FrameLayout {

    private static final long FADE_DURATION_MS    = 300;
    private static final long COLLAPSE_DELAY_MS   = 4000; // auto-collapse history after 4s

    // ─── Child views (inflated from layout) ───────────────────────────────────
    private View       normalState;
    private TextView   tvOriginal;
    private TextView   tvTranslated;
    private TextView   tvPartial;
    private TextView   tvTapHint;

    private View       expandedState;
    private ScrollView historyScroll;
    private TextView   tvHistory;
    private TextView   tvCollapse;

    // ─── State ────────────────────────────────────────────────────────────────
    private boolean isExpanded = false;
    private final List<String> historyLines = new ArrayList<>();
    private final Runnable collapseRunnable = this::collapse;

    // ─── Constructors ─────────────────────────────────────────────────────────

    public SubtitleView(@NonNull Context context) {
        super(context); init(context);
    }
    public SubtitleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs); init(context);
    }
    public SubtitleView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_subtitle, this, true);

        normalState  = findViewById(R.id.subtitle_normal);
        tvOriginal   = findViewById(R.id.tv_subtitle_original);
        tvTranslated = findViewById(R.id.tv_subtitle_translated);
        tvPartial    = findViewById(R.id.tv_subtitle_partial);
        tvTapHint    = findViewById(R.id.tv_subtitle_tap_hint);

        expandedState = findViewById(R.id.subtitle_expanded);
        historyScroll = findViewById(R.id.subtitle_history_scroll);
        tvHistory     = findViewById(R.id.tv_subtitle_history);
        tvCollapse    = findViewById(R.id.tv_subtitle_collapse);

        expandedState.setVisibility(View.GONE);

        // Tap to expand/collapse
        normalState.setOnClickListener(v -> {
            if (!historyLines.isEmpty()) expand();
        });
        tvCollapse.setOnClickListener(v -> collapse());
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Show a live partial STT result (what the user is currently saying).
     * Displayed in the partial text area; cleared when a full exchange arrives.
     */
    public void showPartial(String partial) {
        post(() -> {
            tvPartial.setText(partial);
            tvPartial.setVisibility(partial.isEmpty() ? View.GONE : View.VISIBLE);
            tvOriginal.setVisibility(View.GONE);
            tvTranslated.setVisibility(View.GONE);
            tvTapHint.setVisibility(View.GONE);
        });
    }

    /**
     * Show a completed exchange — the original spoken text and its translation.
     * Fades in the new content and archives the previous exchange to history.
     *
     * @param original   What was spoken (original language)
     * @param translated What it means (target language) — the prominent line
     */
    public void showExchange(String original, String translated) {
        // Archive current content to history before replacing
        String prevOriginal   = tvOriginal.getText().toString();
        String prevTranslated = tvTranslated.getText().toString();
        if (!prevOriginal.isEmpty() || !prevTranslated.isEmpty()) {
            if (!prevOriginal.isEmpty())   historyLines.add(prevOriginal);
            if (!prevTranslated.isEmpty()) historyLines.add("→ " + prevTranslated);
            historyLines.add(""); // blank separator between exchanges
        }

        post(() -> {
            // Clear partial
            tvPartial.setVisibility(View.GONE);
            tvPartial.setText("");

            // Fade out current content, then fade in new
            normalState.animate()
                    .alpha(0f)
                    .setDuration(FADE_DURATION_MS / 2)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            tvOriginal.setText(original);
                            tvTranslated.setText(translated);
                            tvOriginal.setVisibility(
                                    original.isEmpty() ? View.GONE : View.VISIBLE);
                            tvTranslated.setVisibility(
                                    translated.isEmpty() ? View.GONE : View.VISIBLE);
                            tvTapHint.setVisibility(
                                    historyLines.isEmpty() ? View.GONE : View.VISIBLE);

                            normalState.animate()
                                    .alpha(1f)
                                    .setDuration(FADE_DURATION_MS)
                                    .setListener(null)
                                    .start();
                        }
                    }).start();
        });
    }

    /** Clear all content and history. */
    public void clear() {
        post(() -> {
            tvOriginal.setText("");
            tvTranslated.setText("");
            tvPartial.setText("");
            tvOriginal.setVisibility(View.GONE);
            tvTranslated.setVisibility(View.GONE);
            tvPartial.setVisibility(View.GONE);
            tvTapHint.setVisibility(View.GONE);
            historyLines.clear();
            collapse();
        });
    }

    // ─── Expand / collapse ────────────────────────────────────────────────────

    private void expand() {
        if (isExpanded) return;
        isExpanded = true;

        // Build history text
        StringBuilder sb = new StringBuilder();
        for (String line : historyLines) {
            sb.append(line).append("\n");
        }
        tvHistory.setText(sb.toString().trim());

        normalState.setVisibility(View.GONE);
        expandedState.setVisibility(View.VISIBLE);
        expandedState.setAlpha(0f);
        expandedState.animate().alpha(1f).setDuration(FADE_DURATION_MS).start();

        // Scroll to bottom of history
        historyScroll.post(() ->
                historyScroll.fullScroll(ScrollView.FOCUS_DOWN));

        // Auto-collapse after delay
        removeCallbacks(collapseRunnable);
        postDelayed(collapseRunnable, COLLAPSE_DELAY_MS);
    }

    private void collapse() {
        if (!isExpanded) return;
        isExpanded = false;
        removeCallbacks(collapseRunnable);

        expandedState.animate()
                .alpha(0f)
                .setDuration(FADE_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        expandedState.setVisibility(View.GONE);
                        normalState.setVisibility(View.VISIBLE);
                        normalState.setAlpha(0f);
                        normalState.animate().alpha(1f)
                                .setDuration(FADE_DURATION_MS).start();
                    }
                }).start();
    }
}
