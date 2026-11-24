package com.babelmic.app.ui.base;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Shared base for the normal BabelMic screens.
 *
 * <p>The background is drawn edge-to-edge so there are no empty strips at the
 * top and bottom. The actual content is then pushed just below the status bar
 * (and the camera cutout on phones that have one), so nothing sits underneath
 * the clock, battery, notification icons, or the camera.</p>
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Draw the window all the way to the edges, behind the system bars.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        applyTopInset();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        applyTopInset();
    }

    /**
     * Shift the screen's content down by the height of the status bar and any
     * camera cutout. The root background still fills the whole window.
     */
    private void applyTopInset() {
        ViewGroup content = (ViewGroup) findViewById(android.R.id.content);
        if (content == null || content.getChildCount() == 0) {
            return;
        }

        View root = content.getChildAt(0);
        final int originalTopPadding = root.getPaddingTop();

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            int statusBar = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout()).top;
            int top = Math.max(statusBar, cutout);

            view.setPadding(
                    view.getPaddingLeft(),
                    originalTopPadding + top,
                    view.getPaddingRight(),
                    view.getPaddingBottom());
            return insets;
        });
    }
}
