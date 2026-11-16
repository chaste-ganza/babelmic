package com.babelmic.app.ui.onboarding;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.babelmic.app.databinding.ActivityUsernameBinding;
import com.babelmic.app.ui.language.LanguageSelectionActivity;
import com.babelmic.app.ui.profile.ProfileSetupActivity;
import com.babelmic.app.util.ProfileManager;

/**
 * First screen — user enters their display name.
 * Shown only on first launch; skipped on subsequent opens.
 *
 * Flow: Username → ProfileSetupActivity (avatar) → LanguageSelectionActivity → Discovery
 */
public class UsernameActivity extends AppCompatActivity {

    private ActivityUsernameBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip if already set up — go straight to language selection
        if (ProfileManager.isSetupComplete(this)) {
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
            return;
        }

        binding = ActivityUsernameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnConfirm.setEnabled(false);

        binding.etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.btnConfirm.setEnabled(s.toString().trim().length() > 0);
            }
        });

        binding.etUsername.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                proceed();
                return true;
            }
            return false;
        });

        binding.btnConfirm.setOnClickListener(v -> proceed());
    }

    private void proceed() {
        String name = binding.etUsername.getText().toString().trim();
        if (name.isEmpty()) return;

        // Save name to profile, leave other fields as defaults
        ProfileManager.setDisplayName(this, name);

        startActivity(new Intent(this, ProfileSetupActivity.class));
        finish();
    }
}
