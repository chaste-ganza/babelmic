package com.babelmic.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

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
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        }, SPLASH_DURATION_MS);
    }
}
