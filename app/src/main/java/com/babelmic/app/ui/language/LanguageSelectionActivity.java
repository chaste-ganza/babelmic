package com.babelmic.app.ui.language;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.babelmic.app.databinding.ActivityLanguageSelectionBinding;
import com.babelmic.app.model.Language;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.ui.discovery.DeviceDiscoveryActivity;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.util.PermissionHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point of BabelMic.
 *
 * <p>The user selects the language they speak from a searchable list, then taps
 * "Find Partner" to proceed to device discovery.</p>
 *
 * <p>Also handles runtime permission requests for microphone and Wi-Fi Direct.</p>
 */
public class LanguageSelectionActivity extends AppCompatActivity
        implements LanguageAdapter.OnLanguageSelectedListener {

    private ActivityLanguageSelectionBinding binding;
    private LanguageAdapter adapter;

    private Language selectedLanguage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLanguageSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupSearch();
        setupContinueButton();

        // Request permissions early so the user grants them before entering the session
        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestMissingPermissions(this);
        }
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void setupRecyclerView() {
        List<Language> languages = new ArrayList<>(LanguageManager.getSupportedLanguages());
        adapter = new LanguageAdapter(languages, this);
        binding.rvLanguages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLanguages.setAdapter(adapter);
        binding.rvLanguages.setHasFixedSize(true);
    }

    private void setupSearch() {
        binding.etSearchLanguage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterLanguages(s.toString());
            }
        });
    }

    private void setupContinueButton() {
        binding.btnFindPartner.setEnabled(false);
        binding.btnFindPartner.setOnClickListener(v -> {
            if (selectedLanguage == null) {
                Toast.makeText(this, "Please select your language first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!PermissionHelper.hasAllPermissions(this)) {
                PermissionHelper.requestMissingPermissions(this);
                Toast.makeText(this, "Please grant all permissions to continue",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            navigateToDiscovery();
        });
    }

    // ─── Language selection callback ──────────────────────────────────────────

    @Override
    public void onLanguageSelected(Language language) {
        selectedLanguage = language;
        binding.tvSelectedLanguage.setText(
                language.getFlagEmoji() + "  " + language.getDisplayName()
                        + " — " + language.getNativeName());
        binding.tvSelectedLanguage.setVisibility(View.VISIBLE);
        binding.btnFindPartner.setEnabled(true);
    }

    // ─── Filtering ────────────────────────────────────────────────────────────

    private void filterLanguages(String query) {
        List<Language> all = LanguageManager.getSupportedLanguages();
        if (query.trim().isEmpty()) {
            adapter.updateList(new ArrayList<>(all));
            return;
        }
        String lower = query.toLowerCase();
        List<Language> filtered = new ArrayList<>();
        for (Language lang : all) {
            if (lang.getDisplayName().toLowerCase().contains(lower)
                    || lang.getNativeName().toLowerCase().contains(lower)) {
                filtered.add(lang);
            }
        }
        adapter.updateList(filtered);
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void navigateToDiscovery() {
        Intent intent = new Intent(this, DeviceDiscoveryActivity.class);
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,  selectedLanguage.getBcp47Tag());
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME, selectedLanguage.getDisplayName());
        startActivity(intent);
    }

    // ─── Permissions result ───────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_ALL_PERMISSIONS) {
            if (!PermissionHelper.allGranted(grantResults)) {
                Toast.makeText(this,
                        "BabelMic needs microphone and Wi-Fi Direct permissions to work.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
