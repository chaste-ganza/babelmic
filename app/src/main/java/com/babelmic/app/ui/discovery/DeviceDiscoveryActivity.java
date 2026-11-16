package com.babelmic.app.ui.discovery;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.babelmic.app.databinding.ActivityDeviceDiscoveryBinding;
import com.babelmic.app.model.AvatarShape;
import com.babelmic.app.model.Language;
import com.babelmic.app.network.WifiDirectManager;
import com.babelmic.app.ui.widget.PeerRadarView;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.util.ProfileManager;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Screen 4 — Find your peer.
 *
 * Uses Wi-Fi Direct DNS-SD service discovery to find other BabelMic users.
 * Each discovered peer is shown as an avatar icon on the radar field.
 * User taps a peer to select, then taps Confirm to initiate connection.
 */
public class DeviceDiscoveryActivity extends AppCompatActivity
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

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void setupMyAvatar() {
        Language myLang = LanguageManager.findByTag(ProfileManager.getLanguageTag(this));
        String flag = (myLang != null) ? myLang.getFlagEmoji() : "🌐";
        binding.peerRadar.setMyAvatar(
                ProfileManager.getAvatarShape(this),
                ProfileManager.getColorTop(this),
                flag);
    }

    private void setupRadar() {
        binding.peerRadar.setOnPeerTappedListener(peer -> {
            selectedNode = peer;
            binding.btnConfirm.setEnabled(peer != null);
        });
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        binding.btnConfirm.setEnabled(false);
        binding.btnConfirm.setOnClickListener(v -> {
            if (selectedNode == null) return;
            WifiP2pDevice device = deviceMap.get(selectedNode.id);
            if (device != null) {
                wifiDirectManager.connectTo(device);
                binding.btnConfirm.setEnabled(false);
                binding.btnConfirm.setText("Connecting…");
            }
        });
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        wifiDirectManager.register();
        Language myLang = LanguageManager.findByTag(ProfileManager.getLanguageTag(this));
        String langTag  = (myLang != null) ? myLang.getBcp47Tag() : "en";
        wifiDirectManager.registerService(ProfileManager.getDisplayName(this), langTag);
    }

    @Override
    protected void onPause() {
        super.onPause();
        wifiDirectManager.stopDiscovery();
        wifiDirectManager.unregister();
    }

    // ─── WifiDirectCallback ───────────────────────────────────────────────────

    @Override
    public void onWifiDirectAvailabilityChanged(boolean available) {
        runOnUiThread(() -> {
            if (!available) {
                new AlertDialog.Builder(this)
                        .setTitle("Wi-Fi Direct Unavailable")
                        .setMessage("Please enable Wi-Fi to find nearby BabelMic users.")
                        .setPositiveButton("Open Settings",
                                (d, w) -> startActivity(
                                        new Intent(Settings.ACTION_WIFI_SETTINGS)))
                        .setNegativeButton("Cancel", null).show();
            }
        });
    }

    /**
     * Called by {@link WifiDirectManager} each time a BabelMic peer service is discovered.
     * Adds the peer to the radar field if not already present.
     */
    @Override
    public void onPeerServiceFound(WifiP2pDevice device, String peerName, String langTag) {
        runOnUiThread(() -> {
            if (peerNodeMap.containsKey(device.deviceAddress)) {
                // Update existing node's displayed info
                PeerRadarView.PeerNode existing = peerNodeMap.get(device.deviceAddress);
                if (existing != null) {
                    existing.name      = peerName;
                    existing.flagEmoji = getFlagForLang(langTag);
                    binding.peerRadar.setPeers(peerNodes);
                }
                return;
            }

            // New peer — place at random position within field
            double angle = rng.nextDouble() * 2 * Math.PI;
            float  dist  = 0.35f + rng.nextFloat() * 0.45f;
            float  nx    = (float)(dist * Math.cos(angle));
            float  ny    = (float)(dist * Math.sin(angle));

            Language lang = LanguageManager.findByTag(langTag);
            String   flag = (lang != null) ? lang.getFlagEmoji() : "🌐";

            PeerRadarView.PeerNode node = new PeerRadarView.PeerNode(
                    device.deviceAddress,
                    peerName,
                    flag,
                    AvatarShape.CIRCLE,
                    0xFF5B21F5,
                    nx, ny);

            peerNodes.add(node);
            peerNodeMap.put(device.deviceAddress, node);
            deviceMap.put(device.deviceAddress, device);

            binding.peerRadar.setPeers(peerNodes);
        });
    }

    @Override
    public void onConnectionEstablished(InetAddress groupOwnerAddress, boolean isGroupOwner) {
        runOnUiThread(() -> {
            this.peerIpAddress = groupOwnerAddress.getHostAddress();
            this.isGroupOwner  = isGroupOwner;

            String peerName = selectedNode != null ? selectedNode.name : "Partner";

            if (selectedNode != null) {
                // We initiated the connection — show pending screen
                Intent intent = new Intent(this, PendingRequestActivity.class);
                intent.putExtra("peer_name",      peerName);
                intent.putExtra("is_group_owner", isGroupOwner);
                intent.putExtra("peer_ip",        this.peerIpAddress);
                startActivity(intent);
            } else {
                // Connection came to us — show incoming request screen
                Intent intent = new Intent(this, IncomingRequestActivity.class);
                intent.putExtra("peer_name",      peerName);
                intent.putExtra("is_group_owner", isGroupOwner);
                intent.putExtra("peer_ip",        this.peerIpAddress);
                startActivity(intent);
            }

            binding.btnConfirm.setText("Confirm");
            binding.btnConfirm.setEnabled(false);
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            binding.btnConfirm.setText("Confirm");
            binding.btnConfirm.setEnabled(selectedNode != null);
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            binding.btnConfirm.setText("Confirm");
            binding.btnConfirm.setEnabled(selectedNode != null);
            new AlertDialog.Builder(this)
                    .setTitle("Connection Failed")
                    .setMessage(message + "\n\nMake sure both devices have Wi-Fi on "
                            + "and are within range.")
                    .setPositiveButton("Retry", (d, w) -> {
                        Language myLang = LanguageManager.findByTag(
                                ProfileManager.getLanguageTag(this));
                        String langTag = (myLang != null) ? myLang.getBcp47Tag() : "en";
                        wifiDirectManager.registerService(
                                ProfileManager.getDisplayName(this), langTag);
                    })
                    .setNegativeButton("Cancel", null).show();
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String getFlagForLang(String langTag) {
        Language lang = LanguageManager.findByTag(langTag);
        return (lang != null) ? lang.getFlagEmoji() : "🌐";
    }
}
