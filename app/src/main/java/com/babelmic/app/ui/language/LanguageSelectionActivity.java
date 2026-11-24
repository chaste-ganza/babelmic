package com.babelmic.app.ui.language;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import com.babelmic.app.ui.base.BaseActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.babelmic.app.databinding.ActivityLanguageSelectionBinding;
import com.babelmic.app.model.Language;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.ui.conversation.ConversationActivity;
import com.babelmic.app.ui.discovery.DeviceDiscoveryActivity;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.util.PermissionHelper;
import com.babelmic.app.util.ProfileManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen 5 — Language selection.
 * Shows user's avatar badge top-right. Selected language gets a full pill highlight.
 * "Find peer" button navigates to DeviceDiscoveryActivity.
 */
public class LanguageSelectionActivity extends BaseActivity
        implements LanguageAdapter.OnLanguageSelectedListener {

    private ActivityLanguageSelectionBinding binding;
    private LanguageAdapter adapter;
    private Language selectedLanguage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ProfileManager.ensureDefaultProfile(this);

        binding = ActivityLanguageSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupMyBadge();
        setupList();
        setupSearch();
        setupButtons();

        if (!PermissionHelper.hasAllPermissions(this)) {
            PermissionHelper.requestMissingPermissions(this);
        }
    }

    private void setupMyBadge() {
        int resId = ProfileManager.getAvatarResId(this);
        int color = ProfileManager.getIdentityColor(this);
        if (resId != 0) binding.badgeMyAvatar.set(resId, color);
    }

    private void setupList() {
        List<Language> all = new ArrayList<>(LanguageManager.getSupportedLanguages());
        adapter = new LanguageAdapter(all, this);
        binding.rvLanguages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLanguages.setAdapter(adapter);
    }

    private void setupSearch() {
        binding.etSearchLanguage.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int i, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int i, int b, int c) {
                filter(s.toString());
            }
        });
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnFindPeer.setEnabled(false);
        binding.btnFindPeer.setOnClickListener(v -> {
            if (selectedLanguage == null) return;
            if (!PermissionHelper.hasAllPermissions(this)) {
                PermissionHelper.requestMissingPermissions(this);
                Toast.makeText(this, "Please grant permissions first", Toast.LENGTH_SHORT).show();
                return;
            }
            navigateToDiscovery();
        });
    }

    @Override
    public void onLanguageSelected(Language language) {
        selectedLanguage = language;
        binding.btnFindPeer.setEnabled(true);
    }

    private void filter(String q) {
        List<Language> all = LanguageManager.getSupportedLanguages();
        if (q.trim().isEmpty()) { adapter.updateList(new ArrayList<>(all)); return; }
        String lower = q.toLowerCase();
        List<Language> out = new ArrayList<>();
        for (Language l : all) {
            if (l.getDisplayName().toLowerCase().contains(lower)
                    || l.getNativeName().toLowerCase().contains(lower)) out.add(l);
        }
        adapter.updateList(out);
    }

    private void navigateToDiscovery() {
        ProfileManager.setLanguage(this,
                selectedLanguage.getBcp47Tag(), selectedLanguage.getDisplayName());
        ProfileManager.markSetupComplete(this);

        startActivity(new Intent(this, DeviceDiscoveryActivity.class));
    }

    // Test / loopback mode entry point
    public void navigateToLoopback(Language language) {
        selectedLanguage = language;
        ProfileManager.setLanguage(this, language.getBcp47Tag(), language.getDisplayName());
        String targetTag = "fr";
        for (Language l : LanguageManager.getSupportedLanguages()) {
            if (!l.getBcp47Tag().equalsIgnoreCase(language.getBcp47Tag())) {
                targetTag = l.getBcp47Tag(); break;
            }
        }
        if (language.getBcp47Tag().equalsIgnoreCase("fr")) targetTag = "en";
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,      language.getBcp47Tag());
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME,     language.getDisplayName());
        intent.putExtra(SessionConfig.EXTRA_LOOPBACK_MODE,        true);
        intent.putExtra(SessionConfig.EXTRA_LOOPBACK_TARGET_LANG, targetTag);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int code,
            @androidx.annotation.NonNull String[] perms,
            @androidx.annotation.NonNull int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == PermissionHelper.REQUEST_CODE_ALL_PERMISSIONS
                && !PermissionHelper.allGranted(results)) {
            new AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("BabelMic needs Microphone and Wi-Fi Direct access.")
                    .setPositiveButton("Open Settings", (d, w) -> {
                        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        i.setData(Uri.fromParts("package", getPackageName(), null));
                        startActivity(i);
                    })
                    .setNegativeButton("Not Now", null).show();
        }
    }
}
