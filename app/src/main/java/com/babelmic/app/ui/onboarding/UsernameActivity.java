package com.babelmic.app.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.babelmic.app.databinding.ActivityUsernameBinding;
import com.babelmic.app.ui.profile.ProfileSetupActivity;
import com.babelmic.app.util.ProfileManager;

/**
 * Screen 3 — Username entry.
 * Back → OnboardingActivity. Next → ProfileSetupActivity.
 */
public class UsernameActivity extends AppCompatActivity {

    private ActivityUsernameBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUsernameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Pre-fill if user is editing an existing name
        String saved = ProfileManager.getDisplayName(this);
        if (!saved.isEmpty()) {
            binding.etUsername.setText(saved);
            binding.etUsername.setSelection(saved.length());
        }

        binding.etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                binding.btnNext.setEnabled(s.toString().trim().length() > 0);
            }
        });

        binding.etUsername.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { proceed(); return true; }
            return false;
        });

        binding.btnNext.setOnClickListener(v -> proceed());
        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void proceed() {
        String name = binding.etUsername.getText().toString().trim();
        if (name.isEmpty()) return;
        ProfileManager.setDisplayName(this, name);
        startActivity(new Intent(this, ProfileSetupActivity.class));
        finish();
    }
}
