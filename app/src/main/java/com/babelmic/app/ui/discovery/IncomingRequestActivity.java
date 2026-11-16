package com.babelmic.app.ui.discovery;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.babelmic.app.databinding.ActivityIncomingRequestBinding;
import com.babelmic.app.model.AvatarShape;
import com.babelmic.app.model.Language;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.ui.conversation.ConversationActivity;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.util.ProfileManager;

/**
 * Screen 6 — Incoming connection request (peer initiated).
 * Shows: your avatar ← arrow ← peer avatar, with Reject / Approve buttons.
 *
 * Approve → navigates to ConversationActivity.
 * Reject  → finishes (back to discovery).
 */
public class IncomingRequestActivity extends AppCompatActivity {

    private ActivityIncomingRequestBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityIncomingRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String peerName = getIntent().getStringExtra("peer_name");
        boolean isOwner = getIntent().getBooleanExtra("is_group_owner", false);
        String  peerIp  = getIntent().getStringExtra("peer_ip");

        setupMyAvatar();
        setupPeerAvatar(peerName != null ? peerName : "Partner");

        binding.btnReject.setOnClickListener(v -> finish());

        binding.btnApprove.setOnClickListener(v -> {
            Intent intent = new Intent(this, ConversationActivity.class);
            intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,
                    ProfileManager.getLanguageTag(this));
            intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME,
                    ProfileManager.getLanguageName(this));
            intent.putExtra(SessionConfig.EXTRA_IS_GROUP_OWNER, isOwner);
            intent.putExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS, peerIp);
            startActivity(intent);
            finish();
        });
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
}
