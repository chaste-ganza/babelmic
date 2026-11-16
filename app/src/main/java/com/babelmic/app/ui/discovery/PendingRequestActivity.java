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
 * Shows: your avatar → arrow → peer avatar, with a Revoke button.
 *
 * In a real two-device scenario, once the peer approves on their
 * IncomingRequestActivity, Wi-Fi Direct fires onConnectionEstablished
 * and navigation to ConversationActivity happens automatically.
 * For now we simulate the approval after 2 seconds for single-device testing.
 */
public class PendingRequestActivity extends AppCompatActivity {

    private ActivityPendingRequestBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPendingRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String peerName    = getIntent().getStringExtra("peer_name");
        boolean isOwner    = getIntent().getBooleanExtra("is_group_owner", false);
        String  peerIp     = getIntent().getStringExtra("peer_ip");

        setupMyAvatar();
        setupPeerAvatar(peerName != null ? peerName : "Partner");

        binding.btnRevoke.setOnClickListener(v -> {
            finish(); // back to discovery
        });

        // Navigate to conversation (connection was already established by Wi-Fi Direct)
        goToConversation(isOwner, peerIp);
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
        // Peer profile not yet known — show default
        binding.ivPeerAvatar.setAvatar(AvatarShape.CIRCLE, 0xFF5B21F5, "🌐");
        binding.tvPeerName.setText(peerName);
    }

    private void goToConversation(boolean isOwner, String peerIp) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,
                ProfileManager.getLanguageTag(this));
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME,
                ProfileManager.getLanguageName(this));
        intent.putExtra(SessionConfig.EXTRA_IS_GROUP_OWNER, isOwner);
        intent.putExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS, peerIp);
        startActivity(intent);
        // Keep pending screen on back stack so user can revoke
    }
}
