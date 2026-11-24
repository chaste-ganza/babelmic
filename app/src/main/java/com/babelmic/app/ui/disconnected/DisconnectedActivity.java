package com.babelmic.app.ui.disconnected;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import com.babelmic.app.ui.base.BaseActivity;

import com.babelmic.app.databinding.ActivityDisconnectedBinding;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.ui.discovery.DeviceDiscoveryActivity;
import com.babelmic.app.ui.language.LanguageSelectionActivity;
import com.babelmic.app.util.ProfileManager;

/**
 * Screen 11 — Disconnected.
 * Shows peer avatar × my avatar with the disconnect reason.
 * Two actions: Change language → LanguageSelectionActivity
 *              Find peer      → DeviceDiscoveryActivity
 */
public class DisconnectedActivity extends BaseActivity {

    private ActivityDisconnectedBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDisconnectedBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        boolean peerInitiated = getIntent().getBooleanExtra("peer_initiated", false);
        String  peerName      = getIntent().getStringExtra("peer_name");
        int     peerColor     = getIntent().getIntExtra("peer_color", 0xFF4ADE80);

        if (peerName == null) peerName = "Partner";

        // Subtitle
        binding.tvSubtitle.setText(peerInitiated
                ? peerName + " disconnected from the conversation"
                : "The conversation has ended");

        // My avatar
        int myRes   = ProfileManager.getAvatarResId(this);
        int myColor = ProfileManager.getIdentityColor(this);
        binding.badgeMe.set(
                myRes != 0 ? myRes : com.babelmic.app.R.drawable.avatar_bird, myColor);

        // Peer avatar — resolve by name hash (best-effort without actual peer data)
        int peerIdx = Math.abs(peerName.hashCode()) %
                com.babelmic.app.ui.profile.AvatarGridAdapter.AVATAR_RES.length;
        int peerRes = com.babelmic.app.ui.profile.AvatarGridAdapter.AVATAR_RES[peerIdx];
        binding.badgePeer.set(peerRes, peerColor);
        binding.tvPeerName.setText(peerName);

        // Buttons
        binding.btnChangeLanguage.setOnClickListener(v -> goToLanguageSelection());

        binding.btnFindPeer.setOnClickListener(v -> {
            startActivity(new Intent(this, DeviceDiscoveryActivity.class));
            finish();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                goToLanguageSelection();
            }
        });
    }

    private void goToLanguageSelection() {
        startActivity(new Intent(this, LanguageSelectionActivity.class));
        finish();
    }

}
