package com.babelmic.app.ui.discovery;

import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.babelmic.app.databinding.ActivityDeviceDiscoveryBinding;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.network.WifiDirectManager;
import com.babelmic.app.ui.conversation.ConversationActivity;

import java.net.InetAddress;
import java.util.List;

/**
 * Shows nearby Wi-Fi Direct peers and lets the user tap one to connect.
 *
 * <p><b>Flow:</b>
 * <ol>
 *   <li>Activity starts → registers the BroadcastReceiver and starts peer discovery.</li>
 *   <li>As peers are found, {@link DeviceAdapter} populates the list.</li>
 *   <li>User taps a device → {@link WifiDirectManager#connectTo(WifiP2pDevice)} is called.</li>
 *   <li>When the P2P group forms, {@code onConnectionEstablished} fires and we
 *       navigate to {@link ConversationActivity}.</li>
 * </ol>
 * </p>
 */
public class DeviceDiscoveryActivity extends AppCompatActivity
        implements WifiDirectManager.WifiDirectCallback,
                   DeviceAdapter.OnDeviceSelectedListener {

    private ActivityDeviceDiscoveryBinding binding;
    private WifiDirectManager wifiDirectManager;
    private DeviceAdapter deviceAdapter;

    // Language chosen on the previous screen
    private String myLanguageTag;
    private String myLanguageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceDiscoveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Retrieve language from intent
        myLanguageTag  = getIntent().getStringExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG);
        myLanguageName = getIntent().getStringExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME);

        setupToolbar();
        setupRecyclerView();
        setupWifiDirect();
        setupRefreshButton();
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Find a Partner");
        }
        binding.tvYourLanguage.setText("Your language: " + myLanguageName);
    }

    private void setupRecyclerView() {
        deviceAdapter = new DeviceAdapter(this);
        binding.rvDevices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDevices.setAdapter(deviceAdapter);
    }

    private void setupWifiDirect() {
        wifiDirectManager = new WifiDirectManager(this, this);
    }

    private void setupRefreshButton() {
        binding.btnRefresh.setOnClickListener(v -> {
            showScanning(true);
            wifiDirectManager.startDiscovery();
        });
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        wifiDirectManager.register();
        showScanning(true);
        wifiDirectManager.startDiscovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        wifiDirectManager.stopDiscovery();
        wifiDirectManager.unregister();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't disconnect here — connection must survive the transition to ConversationActivity
    }

    // ─── WifiDirectCallback ───────────────────────────────────────────────────

    @Override
    public void onWifiDirectAvailabilityChanged(boolean available) {
        runOnUiThread(() -> {
            if (!available) {
                showError("Wi-Fi Direct is not available on this device.");
            }
        });
    }

    @Override
    public void onPeersDiscovered(List<WifiP2pDevice> peers) {
        runOnUiThread(() -> {
            showScanning(false);
            deviceAdapter.updateDevices(peers);
            binding.tvNoPeers.setVisibility(peers.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public void onConnectionEstablished(InetAddress groupOwnerAddress, boolean isGroupOwner) {
        runOnUiThread(() -> {
            binding.layoutConnecting.setVisibility(View.GONE);
            navigateToConversation(groupOwnerAddress.getHostAddress(), isGroupOwner);
        });
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            binding.layoutConnecting.setVisibility(View.GONE);
            Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            binding.layoutConnecting.setVisibility(View.GONE);
            showError(message);
        });
    }

    // ─── DeviceAdapter callback ───────────────────────────────────────────────

    @Override
    public void onDeviceSelected(WifiP2pDevice device) {
        binding.layoutConnecting.setVisibility(View.VISIBLE);
        binding.tvConnectingTo.setText("Connecting to " + device.deviceName + "…");
        wifiDirectManager.connectTo(device);
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void navigateToConversation(String peerIp, boolean isGroupOwner) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,  myLanguageTag);
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME, myLanguageName);
        intent.putExtra(SessionConfig.EXTRA_IS_GROUP_OWNER,   isGroupOwner);
        intent.putExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS,  peerIp);
        startActivity(intent);
        // Keep this activity alive so the user can go back and re-discover if needed
    }

    // ─── UI helpers ───────────────────────────────────────────────────────────

    private void showScanning(boolean scanning) {
        binding.layoutScanning.setVisibility(scanning ? View.VISIBLE : View.GONE);
        if (scanning) {
            binding.tvNoPeers.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
