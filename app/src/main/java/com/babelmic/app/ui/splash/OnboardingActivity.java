package com.babelmic.app.ui.splash;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.babelmic.app.databinding.ActivityOnboardingBinding;
import com.babelmic.app.ui.onboarding.UsernameActivity;
import com.babelmic.app.util.ProfileManager;
import com.babelmic.app.ui.language.LanguageSelectionActivity;

/**
 * Onboarding screen — shown after splash on every launch.
 * If the user has already completed setup, "Get started" skips directly to language selection.
 */
public class OnboardingActivity extends AppCompatActivity {

    private ActivityOnboardingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnGetStarted.setOnClickListener(v -> {
            if (ProfileManager.isSetupComplete(this)) {
                // Returning user — skip onboarding, go straight to language
                startActivity(new Intent(this, LanguageSelectionActivity.class));
            } else {
                // First time — go through username + avatar setup
                startActivity(new Intent(this, UsernameActivity.class));
            }
            finish();
        });
    }
}
