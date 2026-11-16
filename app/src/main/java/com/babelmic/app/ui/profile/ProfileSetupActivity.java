package com.babelmic.app.ui.profile;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.babelmic.app.databinding.ActivityProfileSetupBinding;
import com.babelmic.app.model.AvatarShape;
import com.babelmic.app.ui.language.LanguageSelectionActivity;
import com.babelmic.app.ui.widget.AvatarVisualizerView;
import com.babelmic.app.util.ProfileManager;

/**
 * Screen 2 — Avatar setup.
 * User picks their avatar shape and colour.
 * Navigates to LanguageSelectionActivity on Confirm.
 */
public class ProfileSetupActivity extends AppCompatActivity {

    private ActivityProfileSetupBinding binding;
    private ShapePickerAdapter  shapeAdapter;
    private ColorSwatchAdapter  colorAdapter;

    private AvatarShape selectedShape = AvatarShape.STAR;
    private int         selectedColor = 0xFFFFEB00; // default yellow like mockup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If launched with skip_to_language (returning user), go straight to language screen
        if (getIntent().getBooleanExtra("skip_to_language", false)) {
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
            return;
        }

        binding = ActivityProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Load any previously saved values
        selectedShape = ProfileManager.getAvatarShape(this);
        selectedColor = ProfileManager.getColorTop(this);

        setupPreview();
        setupShapePicker();
        setupColorPicker();
        setupButtons();
    }

    private void setupPreview() {
        binding.avatarPreview.setAvatar(selectedShape, selectedColor, selectedColor);
        binding.avatarPreview.setState(AvatarVisualizerView.VisualizerState.IDLE);
    }

    private void refreshPreview() {
        binding.avatarPreview.setAvatar(selectedShape, selectedColor, selectedColor);
    }

    private void setupShapePicker() {
        shapeAdapter = new ShapePickerAdapter(shape -> {
            selectedShape = shape;
            refreshPreview();
        });
        shapeAdapter.setSelectedShape(selectedShape);
        shapeAdapter.setPreviewColor(selectedColor);

        binding.rvShapes.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvShapes.setAdapter(shapeAdapter);
    }

    private void setupColorPicker() {
        colorAdapter = new ColorSwatchAdapter(color -> {
            selectedColor = color;
            shapeAdapter.setPreviewColor(color);
            refreshPreview();
        });
        colorAdapter.setSelectedColor(selectedColor);

        binding.rvColors.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvColors.setAdapter(colorAdapter);
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.btnConfirm.setOnClickListener(v -> {
            ProfileManager.setAvatarShape(this, selectedShape);
            ProfileManager.setColors(this, selectedColor, selectedColor);
            startActivity(new Intent(this, LanguageSelectionActivity.class));
            finish();
        });
    }
}
