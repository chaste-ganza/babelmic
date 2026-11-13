package com.babelmic.app.network;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Looper;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages Android Wi-Fi Direct (P2P) device discovery and connection negotiation.
 *
 * <p><b>Responsibilities:</b>
 * <ul>
 *   <li>Registering / unregistering the Wi-Fi Direct BroadcastReceiver.</li>
 *   <li>Starting and stopping peer discovery.</li>
 *   <li>Connecting to a chosen peer device.</li>
 *   <li>Reporting the group owner IP address once a P2P group forms
 *       (the group owner runs the socket server; the client connects to it).</li>
 * </ul>
 * </p>
 *
 * <p><b>Lifecycle:</b>
 * <pre>
 *   onCreate  → register()
 *   onResume  → startDiscovery()
 *   onPause   → stopDiscovery()
 *   onDestroy → unregister() + disconnect()
 * </pre>
 * </p>
 */
public class WifiDirectManager {

    private static final String TAG = "WifiDirectManager";

    /** Port used for the BabelMic socket channel between the two devices. */
    public static final int BABELMIC_PORT = 8988;

    // ─── Callback interface ───────────────────────────────────────────────────

    public interface WifiDirectCallback {
        /** Wi-Fi Direct hardware became available / unavailable. */
        void onWifiDirectAvailabilityChanged(boolean available);

        /** The list of nearby discoverable peers was updated. */
        void onPeersDiscovered(List<WifiP2pDevice> peers);

        /**
         * A P2P group was successfully formed.
         *
         * @param groupOwnerAddress  IP of the group owner (runs the socket server).
         * @param isGroupOwner       true if this device is the group owner.
         */
        void onConnectionEstablished(InetAddress groupOwnerAddress, boolean isGroupOwner);

        /** The P2P connection was lost or torn down. */
        void onDisconnected();

        /** Something went wrong. */
        void onError(String message);
    }

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final Context context;
    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final WifiDirectCallback callback;

    private final BroadcastReceiver receiver;
    private final IntentFilter intentFilter;

    private boolean receiverRegistered = false;
    private boolean discovering         = false;

    // ─── Constructor ──────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    public WifiDirectManager(Context context, WifiDirectCallback callback) {
        this.context  = context.getApplicationContext();
        this.callback = callback;

        manager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(context, Looper.getMainLooper(), () -> {
            Log.w(TAG, "Wi-Fi Direct channel disconnected");
            callback.onDisconnected();
        });

        receiver     = new WifiDirectBroadcastReceiver();
        intentFilter = buildIntentFilter();
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    /** Register the BroadcastReceiver. Call from Activity.onResume(). */
    public void register() {
        if (!receiverRegistered) {
            context.registerReceiver(receiver, intentFilter);
            receiverRegistered = true;
            Log.d(TAG, "BroadcastReceiver registered");
        }
    }

    /** Unregister the BroadcastReceiver. Call from Activity.onPause(). */
    public void unregister() {
        if (receiverRegistered) {
            context.unregisterReceiver(receiver);
            receiverRegistered = false;
            Log.d(TAG, "BroadcastReceiver unregistered");
        }
    }

    // ─── Discovery ────────────────────────────────────────────────────────────

    /** Begin scanning for nearby Wi-Fi Direct peers. */
    @SuppressLint("MissingPermission")
    public void startDiscovery() {
        if (discovering) return;
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                discovering = true;
                Log.d(TAG, "Peer discovery started");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Peer discovery failed: " + reasonToString(reason));
                callback.onError("Peer discovery failed: " + reasonToString(reason));
            }
        });
    }

    /** Stop peer scanning to conserve battery. */
    public void stopDiscovery() {
        if (!discovering) return;
        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                discovering = false;
                Log.d(TAG, "Peer discovery stopped");
            }

            @Override
            public void onFailure(int reason) {
                Log.w(TAG, "stopPeerDiscovery failed: " + reasonToString(reason));
            }
        });
    }

    // ─── Connection ───────────────────────────────────────────────────────────

    /**
     * Initiate a P2P connection to the selected device.
     * Android will negotiate which device becomes the group owner automatically.
     *
     * @param device The peer selected by the user from the discovery list.
     */
    @SuppressLint("MissingPermission")
    public void connectTo(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Connection request sent to: " + device.deviceName);
                // Actual result arrives via WIFI_P2P_CONNECTION_CHANGED_ACTION broadcast
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Connect failed: " + reasonToString(reason));
                callback.onError("Could not connect: " + reasonToString(reason));
            }
        });
    }

    /** Remove the current P2P group (disconnects both sides). */
    public void disconnect() {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "P2P group removed");
                callback.onDisconnected();
            }

            @Override
            public void onFailure(int reason) {
                Log.w(TAG, "removeGroup failed: " + reasonToString(reason));
            }
        });
    }

    // ─── BroadcastReceiver ────────────────────────────────────────────────────

    private class WifiDirectBroadcastReceiver extends BroadcastReceiver {

        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {

                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    boolean enabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                    Log.d(TAG, "Wi-Fi Direct state: " + (enabled ? "ENABLED" : "DISABLED"));
                    callback.onWifiDirectAvailabilityChanged(enabled);
                    break;
                }

                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    // Request the updated peer list
                    manager.requestPeers(channel, (WifiP2pDeviceList peerList) -> {
                        List<WifiP2pDevice> devices =
                                new ArrayList<>(peerList.getDeviceList());
                        Log.d(TAG, "Peers discovered: " + devices.size());
                        callback.onPeersDiscovered(devices);
                    });
                    break;
                }

                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    NetworkInfo networkInfo =
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

                    if (networkInfo != null && networkInfo.isConnected()) {
                        // Request group info to get the group owner IP
                        manager.requestConnectionInfo(channel,
                                (WifiP2pInfo info) -> {
                                    if (info.groupFormed) {
                                        Log.d(TAG, "Group formed. Owner IP: "
                                                + info.groupOwnerAddress.getHostAddress()
                                                + " | isOwner: " + info.isGroupOwner);
                                        callback.onConnectionEstablished(
                                                info.groupOwnerAddress,
                                                info.isGroupOwner);
                                    }
                                });
                    } else {
                        Log.d(TAG, "P2P connection lost");
                        callback.onDisconnected();
                    }
                    break;
                }

                case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION:
                    // Could expose local device name changes — not needed for now
                    break;
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static IntentFilter buildIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        filter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        return filter;
    }

    private static String reasonToString(int reason) {
        switch (reason) {
            case WifiP2pManager.ERROR:         return "Internal error";
            case WifiP2pManager.P2P_UNSUPPORTED: return "P2P unsupported";
            case WifiP2pManager.BUSY:          return "Framework busy";
            case WifiP2pManager.NO_SERVICE_REQUESTS: return "No service requests";
            default: return "Unknown (" + reason + ")";
        }
    }
}
