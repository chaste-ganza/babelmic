package com.babelmic.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;

import com.babelmic.app.ui.base.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.babelmic.app.databinding.ActivityProfileSetupBinding;
import com.babelmic.app.ui.language.LanguageSelectionActivity;
import com.babelmic.app.util.ProfileManager;

/**
 * Screen 4 — Avatar setup.
 *
 * Shows the currently selected avatar in a large circle with the identity
 * colour ring. Tapping the avatar opens the AvatarPickerBottomSheet.
 * The colour swatch row changes the ring colour instantly.
 * Confirm → LanguageSelectionActivity.
 */
public class ProfileSetupActivity extends BaseActivity
        implements AvatarPickerBottomSheet.OnAvatarConfirmedListener {

    private ActivityProfileSetupBinding binding;
    private ColorSwatchAdapter colorAdapter;

    private int    selectedAvatarRes  = -1;
    private String selectedAvatarName = "";
    private int    selectedColor      = 0xFFFF2D78; // default pink

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Skip directly to language if profile already complete
        if (getIntent().getBooleanExtra("skip_to_language", false)) {
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
            return;
        }

        binding = ActivityProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load previously saved values
        selectedAvatarName = ProfileManager.getAvatarName(this);
        selectedColor      = ProfileManager.getIdentityColor(this);

        // Resolve res ID from saved name
        for (int i = 0; i < AvatarGridAdapter.AVATAR_NAMES.length; i++) {
            if (AvatarGridAdapter.AVATAR_NAMES[i].equals(selectedAvatarName)) {
                selectedAvatarRes = AvatarGridAdapter.AVATAR_RES[i];
                break;
            }
        }
        // Default to first avatar if none saved
        if (selectedAvatarRes < 0) {
            selectedAvatarRes  = AvatarGridAdapter.AVATAR_RES[0];
            selectedAvatarName = AvatarGridAdapter.AVATAR_NAMES[0];
        }

        binding.tvUsername.setText(ProfileManager.getDisplayName(this));

        setupAvatarPreview();
        setupColorPicker();
        setupButtons();
    }

    private void setupAvatarPreview() {
        binding.ivAvatar.setImageResource(selectedAvatarRes);
        applyRingColor(selectedColor);
    }

    private void applyRingColor(int color) {
        // iv_ring uses android:src (not background), so tint via ImageView
        android.graphics.drawable.Drawable d = binding.ivRing.getDrawable();
        if (d != null) {
            d = d.mutate();
            d.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            binding.ivRing.setImageDrawable(d);
        } else {
            // Fallback: set background tint
            binding.ivRing.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(color));
        }
    }

    private void setupColorPicker() {
        colorAdapter = new ColorSwatchAdapter(color -> {
            selectedColor = color;
            applyRingColor(color);
        });
        colorAdapter.setSelectedColor(selectedColor);

        binding.rvColors.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvColors.setAdapter(colorAdapter);
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Tap avatar → open picker bottom sheet
        binding.flAvatarContainer.setOnClickListener(v -> {
            AvatarPickerBottomSheet sheet = AvatarPickerBottomSheet.newInstance(
                    selectedColor, selectedAvatarName);
            sheet.setOnAvatarConfirmedListener(this);
            sheet.show(getSupportFragmentManager(), "avatar_picker");
        });

        binding.btnConfirm.setOnClickListener(v -> {
            ProfileManager.setAvatarName(this, selectedAvatarName);
            ProfileManager.setAvatarResId(this, selectedAvatarRes);
            ProfileManager.setIdentityColor(this, selectedColor);
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
        });
    }

    // ─── AvatarPickerBottomSheet callback ─────────────────────────────────────

    @Override
    public void onAvatarConfirmed(int drawableResId, String avatarName) {
        selectedAvatarRes  = drawableResId;
        selectedAvatarName = avatarName;
        binding.ivAvatar.setImageResource(drawableResId);
    }
}
