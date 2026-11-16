package com.babelmic.app.ui.discovery;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.babelmic.app.databinding.ActivityPendingRequestBinding;
import com.babelmic.app.model.AvatarShape;
import com.babelmic.app.model.Language;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.ui.conversation.ConversationActivity;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.util.ProfileManager;

/**
 * Screen 5 — Pending connection request (you initiated).
 *
 * This screen is shown AFTER Wi-Fi Direct has already established a P2P group.
 * It displays "you → peer" and waits for the user to tap Confirm before
 * entering the conversation.
 *
 * Tapping Confirm launches ConversationActivity and finishes this activity
 * so it is removed from the back stack. Tapping Revoke disconnects and
 * goes back to discovery.
 *
 * Back stack after Confirm: LanguageSelection → Discovery → Conversation
 * (PendingRequest is finished and NOT on the stack)
 */
public class PendingRequestActivity extends AppCompatActivity {

    private ActivityPendingRequestBinding binding;

    private boolean isOwner;
    private String  peerIp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String peerName = getIntent().getStringExtra("peer_name");
        isOwner         = getIntent().getBooleanExtra("is_group_owner", false);
        peerIp          = getIntent().getStringExtra("peer_ip");

        setupMyAvatar();
        setupPeerAvatar(peerName != null ? peerName : "Partner");
        setupButtons();
    }

    private void setupMyAvatar() {
        Language myLang = LanguageManager.findByTag(ProfileManager.getLanguageTag(this));
        String flag = (myLang != null) ? myLang.getFlagEmoji() : "🌐";
        binding.ivMyAvatar.setAvatar(
                ProfileManager.getAvatarShape(this),
                ProfileManager.getColorTop(this),
                flag);
        binding.tvMyName.setText(ProfileManager.getDisplayName(this));
    }

    private void setupPeerAvatar(String peerName) {
        binding.ivPeerAvatar.setAvatar(AvatarShape.CIRCLE, 0xFF5B21F5, "🌐");
        binding.tvPeerName.setText(peerName);
    }

    private void setupButtons() {
        // Revoke — go back to discovery without entering conversation
        binding.btnRevoke.setOnClickListener(v -> {
            // The Wi-Fi Direct connection is still active; DeviceDiscoveryActivity
            // will handle cleanup when it resumes.
            finish();
        });

        // Confirm — enter the conversation and remove this screen from the stack
        binding.btnConfirm.setOnClickListener(v -> goToConversation());
    }

    private void goToConversation() {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,
                ProfileManager.getLanguageTag(this));
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME,
                ProfileManager.getLanguageName(this));
        intent.putExtra(SessionConfig.EXTRA_IS_GROUP_OWNER, isOwner);
        intent.putExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS, peerIp);
        startActivity(intent);
        // Finish so this screen is NOT on the back stack.
        // Disconnect in ConversationActivity will return to DeviceDiscoveryActivity.
        finish();
    }
}
