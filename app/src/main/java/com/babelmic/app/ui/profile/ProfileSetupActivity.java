package com.babelmic.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.babelmic.app.databinding.ActivityProfileSetupBinding;
import com.babelmic.app.ui.language.LanguageSelectionActivity;
import com.babelmic.app.util.ProfileManager;

/**
 * Screen 2 — Avatar setup.
 * User picks one of the six built-in avatar images.
 * Navigates to LanguageSelectionActivity on Confirm.
 */
public class ProfileSetupActivity extends AppCompatActivity {

    private ActivityProfileSetupBinding binding;
    private AvatarPickerAdapter avatarAdapter;
    private int selectedAvatarResId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If launched with skip_to_language (returning user), skip to language screen
        if (getIntent().getBooleanExtra("skip_to_language", false)) {
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
            return;
        }

        binding = ActivityProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load previously saved avatar (default to first)
        int saved = ProfileManager.getAvatarResId(this);
        selectedAvatarResId = (saved != 0) ? saved : AvatarPickerAdapter.AVATARS[0];

        setupAvatarPicker();
        setupButtons();
    }

    private void setupAvatarPicker() {
        avatarAdapter = new AvatarPickerAdapter((resId, position) -> {
            selectedAvatarResId = resId;
            // Show the selected avatar in the large preview slot
            binding.avatarPreview.setImageResource(resId);
        });
        avatarAdapter.setSelectedByResId(selectedAvatarResId);

        binding.rvShapes.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvShapes.setAdapter(avatarAdapter);

        // Set initial preview
        binding.avatarPreview.setImageResource(selectedAvatarResId);
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.btnConfirm.setOnClickListener(v -> {
            ProfileManager.setAvatarResId(this, selectedAvatarResId);
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
        });
    }
}
