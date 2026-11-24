package com.babelmic.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.babelmic.app.R;
import com.babelmic.app.ui.splash.OnboardingActivity;

/**
 * Splash screen shown on every launch.
 * Displays the frosted glass background + BabelMic logo for 1.5 seconds,
 * then navigates to OnboardingActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Draw the splash background edge-to-edge (behind the status/navigation bars)
        // and hide those bars while the splash is visible, so there are no gaps.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                ViewCompat.getWindowInsetsController(getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }, SPLASH_DURATION_MS);
    }
}
