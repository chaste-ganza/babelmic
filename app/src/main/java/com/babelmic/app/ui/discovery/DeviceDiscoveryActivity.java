package com.babelmic.app.ui.discovery;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import com.babelmic.app.ui.base.BaseActivity;

import com.babelmic.app.databinding.ActivityDeviceDiscoveryBinding;
import com.babelmic.app.network.WifiDirectManager;
import com.babelmic.app.ui.widget.PeerRadarView;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.util.ProfileManager;
import com.babelmic.app.model.Language;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Screen 6 — Find your peer.
 * Pink wave rings, avatar badges with identity rings, chevron animation on tap.
 * "Connect peer" enabled only after selecting a peer.
 * Pending/Incoming shown as bottom sheets over this screen.
 */
public class DeviceDiscoveryActivity extends BaseActivity
        implements WifiDirectManager.WifiDirectCallback {

    private ActivityDeviceDiscoveryBinding binding;
    private WifiDirectManager wifiDirectManager;

    private final Map<String, WifiP2pDevice>      deviceMap   = new HashMap<>();
    private final Map<String, PeerRadarView.PeerNode> peerNodeMap = new HashMap<>();
    private final List<PeerRadarView.PeerNode>    peerNodes   = new ArrayList<>();
    private final Random rng = new Random();

    private PeerRadarView.PeerNode selectedNode = null;
    private String                 peerIpAddress = null;
    private boolean                isGroupOwner  = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceDiscoveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupMyAvatar();
        setupRadar();
        setupButtons();
        wifiDirectManager = new WifiDirectManager(this, this);
    }

    private void setupMyAvatar() {
        int resId = ProfileManager.getAvatarResId(this);
        int color = ProfileManager.getIdentityColor(this);
        Language myLang = LanguageManager.findByTag(ProfileManager.getLanguageTag(this));
        String flag = (myLang != null) ? myLang.getFlagEmoji() : "🌐";

        binding.badgeMyAvatar.set(resId != 0 ? resId : com.babelmic.app.R.drawable.avatar_bird, color);
        binding.peerRadar.setMyAvatar(resId != 0 ? resId : com.babelmic.app.R.drawable.avatar_bird, color, flag);
    }

    private void setupRadar() {
        binding.peerRadar.setOnPeerTappedListener(peer -> {
            selectedNode = peer;
            binding.btnConnectPeer.setEnabled(peer != null);
        });
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnConnectPeer.setEnabled(false);
        binding.btnConnectPeer.setOnClickListener(v -> {
            if (selectedNode == null) return;
            WifiP2pDevice device = deviceMap.get(selectedNode.id);
            if (device != null) {
                wifiDirectManager.connectTo(device);
                binding.btnConnectPeer.setEnabled(false);
                binding.btnConnectPeer.setText("Connecting…");
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        wifiDirectManager.register();
        Language myLang = LanguageManager.findByTag(ProfileManager.getLanguageTag(this));
        String langTag  = (myLang != null) ? myLang.getBcp47Tag() : "en";
        wifiDirectManager.registerService(ProfileManager.getDisplayName(this), langTag);
    }

    @Override protected void onPause() {
        super.onPause();
        wifiDirectManager.stopDiscovery();
        wifiDirectManager.unregister();
    }

    // ─── WifiDirectCallback ───────────────────────────────────────────────────

    @Override public void onWifiDirectAvailabilityChanged(boolean available) {
        runOnUiThread(() -> {
            if (!available) {
                new AlertDialog.Builder(this)
                        .setTitle("Wi-Fi Direct Unavailable")
                        .setMessage("Please enable Wi-Fi to find nearby BabelMic users.")
                        .setPositiveButton("Open Settings",
                                (d, w) -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                        .setNegativeButton("Cancel", null).show();
            }
        });
    }

    @Override public void onPeerServiceFound(WifiP2pDevice device, String peerName, String langTag) {
        runOnUiThread(() -> {
            if (peerNodeMap.containsKey(device.deviceAddress)) {
                PeerRadarView.PeerNode node = peerNodeMap.get(device.deviceAddress);
                if (node != null) {
                    node.name      = peerName;
                    Language lang  = LanguageManager.findByTag(langTag);
                    node.flagEmoji = (lang != null) ? lang.getFlagEmoji() : "🌐";
                    binding.peerRadar.setPeers(peerNodes);
                }
                return;
            }

            double angle = rng.nextDouble() * 2 * Math.PI;
            float  dist  = 0.35f + rng.nextFloat() * 0.45f;
            Language lang = LanguageManager.findByTag(langTag);
            String   flag = (lang != null) ? lang.getFlagEmoji() : "🌐";

            // Default avatar until handshake provides real one
            int avatarRes = com.babelmic.app.R.drawable.avatar_rabbit;

            PeerRadarView.PeerNode node = new PeerRadarView.PeerNode(
                    device.deviceAddress, peerName, flag,
                    0xFF4ADE80, // default green ring until real colour arrives
                    avatarRes,
                    (float)(dist * Math.cos(angle)),
                    (float)(dist * Math.sin(angle)));

            peerNodes.add(node);
            peerNodeMap.put(device.deviceAddress, node);
            deviceMap.put(device.deviceAddress, device);
            binding.peerRadar.setPeers(peerNodes);
        });
    }

    @Override public void onConnectionEstablished(InetAddress groupOwnerAddress, boolean isGroupOwner) {
        runOnUiThread(() -> {
            this.peerIpAddress = groupOwnerAddress.getHostAddress();
            this.isGroupOwner  = isGroupOwner;
            String peerName    = selectedNode != null ? selectedNode.name : "Partner";
            int    peerAvatar  = selectedNode != null ? selectedNode.avatarResId
                                                     : com.babelmic.app.R.drawable.avatar_rabbit;
            int    peerColor   = selectedNode != null ? selectedNode.color : 0xFF4ADE80;

            if (selectedNode != null) {
                // We initiated — show pending bottom sheet
                PendingBottomSheet sheet = PendingBottomSheet.newInstance(
                        peerName, peerAvatar, peerColor,
                        isGroupOwner, this.peerIpAddress);
                sheet.show(getSupportFragmentManager(), "pending");
            } else {
                // They initiated — show incoming bottom sheet
                IncomingBottomSheet sheet = IncomingBottomSheet.newInstance(
                        peerName, peerAvatar, peerColor,
                        isGroupOwner, this.peerIpAddress);
                sheet.show(getSupportFragmentManager(), "incoming");
            }
            binding.btnConnectPeer.setText("Connect peer");
            binding.btnConnectPeer.setEnabled(false);
        });
    }

    @Override public void onDisconnected() {
        runOnUiThread(() -> {
            binding.btnConnectPeer.setText("Connect peer");
            binding.btnConnectPeer.setEnabled(selectedNode != null);
        });
    }

    @Override public void onError(String message) {
        runOnUiThread(() -> {
            binding.btnConnectPeer.setText("Connect peer");
            binding.btnConnectPeer.setEnabled(selectedNode != null);
            new AlertDialog.Builder(this)
                    .setTitle("Connection Failed")
                    .setMessage(message + "\n\nMake sure both devices have Wi-Fi on.")
                    .setPositiveButton("Retry", (d, w) -> {
                        Language myLang = LanguageManager.findByTag(ProfileManager.getLanguageTag(this));
                        wifiDirectManager.registerService(
                                ProfileManager.getDisplayName(this),
                                myLang != null ? myLang.getBcp47Tag() : "en");
                    })
                    .setNegativeButton("Cancel", null).show();
        });
    }
}
