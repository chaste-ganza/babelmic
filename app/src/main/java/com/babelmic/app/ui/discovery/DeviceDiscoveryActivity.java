package com.babelmic.app.ui.discovery;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

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
import java.util.List;
import java.util.Random;

/**
 * Screen 4 — Find your peer.
 * Shows a glowing radar field with floating peer avatar icons.
 * User taps a peer to select it, then taps Confirm to send a connection request.
 */
public class DeviceDiscoveryActivity extends AppCompatActivity
        implements WifiDirectManager.WifiDirectCallback {

    private ActivityDeviceDiscoveryBinding binding;
    private WifiDirectManager wifiDirectManager;

    // Map from Wi-Fi Direct device address → PeerRadarView.PeerNode
    private final java.util.Map<String, PeerRadarView.PeerNode> peerNodeMap =
            new java.util.HashMap<>();
    // Map from PeerRadarView.PeerNode id → WifiP2pDevice
    private final java.util.Map<String, WifiP2pDevice> deviceMap =
            new java.util.HashMap<>();

    private PeerRadarView.PeerNode pendingSelectedNode = null;
    private String                 peerIpAddress       = null;
    private boolean                isGroupOwner        = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceDiscoveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupMyAvatar();
        setupRadar();
        setupButtons();
        setupWifiDirect();
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
            pendingSelectedNode = peer;
            binding.btnConfirm.setEnabled(peer != null);
        });
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.btnConfirm.setEnabled(false);
        binding.btnConfirm.setOnClickListener(v -> {
            if (pendingSelectedNode == null) return;
            WifiP2pDevice device = deviceMap.get(pendingSelectedNode.id);
            if (device != null) {
                wifiDirectManager.connectTo(device);
                binding.btnConfirm.setEnabled(false);
                binding.btnConfirm.setText("Connecting…");
            }
        });
    }

    private void setupWifiDirect() {
        wifiDirectManager = new WifiDirectManager(this, this);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        wifiDirectManager.register();
        wifiDirectManager.startDiscovery();
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
                        .setMessage("Enable Wi-Fi to find nearby partners.")
                        .setPositiveButton("Open Settings",
                                (d, w) -> startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)))
                        .setNegativeButton("Cancel", null).show();
            }
        });
    }

    @Override
    public void onPeersDiscovered(List<WifiP2pDevice> devices) {
        runOnUiThread(() -> {
            deviceMap.clear();
            peerNodeMap.clear();

            List<PeerRadarView.PeerNode> nodes = new ArrayList<>();
            Random rng = new Random();

            for (WifiP2pDevice device : devices) {
                // Position peers randomly within the radar field
                double angle = rng.nextDouble() * 2 * Math.PI;
                float dist   = 0.4f + rng.nextFloat() * 0.45f;
                float nx     = (float)(dist * Math.cos(angle));
                float ny     = (float)(dist * Math.sin(angle));

                PeerRadarView.PeerNode node = new PeerRadarView.PeerNode(
                        device.deviceAddress,
                        device.deviceName.isEmpty() ? "Partner" : device.deviceName,
                        "🌐",         // flag updated after handshake
                        AvatarShape.CIRCLE,
                        0xFF5B21F5,   // default colour, updated after handshake
                        nx, ny);

                nodes.add(node);
                peerNodeMap.put(device.deviceAddress, node);
                deviceMap.put(device.deviceAddress,   device);
            }

            binding.peerRadar.setPeers(nodes);
            binding.btnConfirm.setEnabled(false);
            pendingSelectedNode = null;
        });
    }

    @Override
    public void onConnectionEstablished(InetAddress groupOwnerAddress, boolean isGroupOwner) {
        runOnUiThread(() -> {
            this.peerIpAddress = groupOwnerAddress.getHostAddress();
            this.isGroupOwner  = isGroupOwner;

            // Navigate to PendingRequestActivity to show the "waiting for peer" screen
            Intent intent = new Intent(this, PendingRequestActivity.class);
            intent.putExtra("peer_name",
                    pendingSelectedNode != null ? pendingSelectedNode.name : "Partner");
            intent.putExtra("is_group_owner", isGroupOwner);
            intent.putExtra("peer_ip",        this.peerIpAddress);
            startActivity(intent);
            binding.btnConfirm.setText("Confirm");
            binding.btnConfirm.setEnabled(false);
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            binding.btnConfirm.setText("Confirm");
            binding.btnConfirm.setEnabled(pendingSelectedNode != null);
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            binding.btnConfirm.setText("Confirm");
            binding.btnConfirm.setEnabled(pendingSelectedNode != null);
            new AlertDialog.Builder(this)
                    .setTitle("Connection Failed")
                    .setMessage(message + "\n\nMake sure both devices have Wi-Fi on.")
                    .setPositiveButton("Retry", (d, w) -> wifiDirectManager.startDiscovery())
                    .setNegativeButton("Cancel", null).show();
        });
    }
}
