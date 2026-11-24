package com.babelmic.app.ui.connected;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.babelmic.app.ui.base.BaseActivity;

import com.babelmic.app.R;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.ui.conversation.ConversationActivity;

/**
 * Screen 9 — Connected successfully.
 * Shows a green checkmark for 1.5 seconds then navigates to ConversationActivity.
 * Removes itself from the back stack so disconnect returns to DeviceDiscoveryActivity.
 */
public class ConnectedSuccessfullyActivity extends BaseActivity {

    private static final long DISPLAY_MS = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected_successfully);

        boolean isOwner = getIntent().getBooleanExtra(SessionConfig.EXTRA_IS_GROUP_OWNER, false);
        String  peerIp  = getIntent().getStringExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS);
        String  langTag = getIntent().getStringExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG);
        String  langName= getIntent().getStringExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(this, ConversationActivity.class);
            intent.putExtra(SessionConfig.EXTRA_IS_GROUP_OWNER,   isOwner);
            intent.putExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS,  peerIp);
            intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,  langTag);
            intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME, langName);
            // Clear this activity from stack — disconnect goes back to discovery
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
            finish();
        }, DISPLAY_MS);
    }
}
