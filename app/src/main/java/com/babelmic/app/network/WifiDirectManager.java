package com.babelmic.app.network;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Looper;
import android.util.Log;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * Wi-Fi Direct discovery using DNS-SD service registration and discovery.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Each device registers a local DNS-SD service named {@code _babelmic._tcp}
 *       carrying the user's display name, language tag, and TCP port as TXT records.</li>
 *   <li>Each device scans for {@code _babelmic._tcp} services specifically — not every
 *       Wi-Fi Direct device in range, only other BabelMic users.</li>
 *   <li>When a peer service is found we receive their device reference and TXT records
 *       (name, language) in a single callback — no handshake needed for profile data
 *       at the discovery level.</li>
 *   <li>When the user selects a peer and taps Confirm, we call {@link #connectTo}
 *       which triggers the standard Wi-Fi Direct group negotiation.</li>
 *   <li>Once the group forms, {@link WifiDirectCallback#onConnectionEstablished} fires
 *       with the group-owner IP. The TCP socket layer takes over from there.</li>
 * </ol>
 *
 * <h3>Why service discovery over discoverPeers()</h3>
 * {@code discoverPeers()} returns every P2P-capable device within range regardless
 * of app. It is also unreliable on many OEM Android builds. DNS-SD service discovery
 * only surfaces devices running BabelMic, provides the peer's profile data for free,
 * and is significantly more reliable across manufacturers.
 */
public class WifiDirectManager {

    private static final String TAG          = "WifiDirectManager";
    public  static final int    BABELMIC_PORT = 8988;
    private static final String SERVICE_TYPE  = "_babelmic._tcp";
    private static final String SERVICE_INSTANCE = "babelmic";

    // ─── TXT record keys ─────────────────────────────────────────────────────
    private static final String TXT_NAME  = "name";
    private static final String TXT_LANG  = "lang";
    private static final String TXT_PORT  = "port";

    // ─── Callback ─────────────────────────────────────────────────────────────

    public interface WifiDirectCallback {
        void onWifiDirectAvailabilityChanged(boolean available);

        /**
         * Called each time a BabelMic peer service is discovered.
         *
         * @param device    The peer's Wi-Fi Direct device (use for {@link #connectTo})
         * @param peerName  Display name from TXT record
         * @param langTag   BCP-47 language tag from TXT record
         */
        void onPeerServiceFound(WifiP2pDevice device, String peerName, String langTag);

        /** Called when the P2P group is formed and ready for TCP socket connection. */
        void onConnectionEstablished(InetAddress groupOwnerAddress, boolean isGroupOwner);

        void onDisconnected();
        void onError(String message);
    }

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final Context            context;
    private final WifiP2pManager     manager;
    private final WifiP2pManager.Channel channel;
    private final WifiDirectCallback callback;

    private final BroadcastReceiver receiver;
    private final IntentFilter       intentFilter;

    private boolean receiverRegistered = false;

    // Cache device address → WifiP2pDevice so we can pass the right device to connectTo()
    private final Map<String, WifiP2pDevice> deviceCache = new HashMap<>();

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

        receiver     = new P2pBroadcastReceiver();
        intentFilter = buildIntentFilter();
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    public void register() {
        if (!receiverRegistered) {
            context.registerReceiver(receiver, intentFilter);
            receiverRegistered = true;
        }
    }

    public void unregister() {
        if (receiverRegistered) {
            context.unregisterReceiver(receiver);
            receiverRegistered = false;
        }
    }

    // ─── Service registration ─────────────────────────────────────────────────

    /**
     * Register this device as a BabelMic service so peers can discover it.
     * Call once after Wi-Fi Direct becomes available.
     *
     * @param displayName  User's display name (sent in TXT record)
     * @param languageTag  BCP-47 language tag (sent in TXT record)
     */
    @SuppressLint("MissingPermission")
    public void registerService(String displayName, String languageTag) {
        Map<String, String> record = new HashMap<>();
        record.put(TXT_NAME, displayName);
        record.put(TXT_LANG, languageTag);
        record.put(TXT_PORT, String.valueOf(BABELMIC_PORT));

        WifiP2pDnsSdServiceInfo serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(
                        SERVICE_INSTANCE, SERVICE_TYPE, record);

        manager.clearLocalServices(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {
                manager.addLocalService(channel, serviceInfo,
                        new WifiP2pManager.ActionListener() {
                            @Override public void onSuccess() {
                                Log.d(TAG, "BabelMic service registered");
                                startServiceDiscovery();
                            }
                            @Override public void onFailure(int reason) {
                                Log.e(TAG, "addLocalService failed: " + reason);
                                // Fall back to plain peer discovery
                                startPeerDiscoveryFallback();
                            }
                        });
            }
            @Override public void onFailure(int reason) {
                Log.w(TAG, "clearLocalServices failed: " + reason);
                startServiceDiscovery();
            }
        });
    }

    // ─── Service discovery ────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private void startServiceDiscovery() {
        // DNS-SD TXT record listener — fires when a peer's TXT records arrive
        WifiP2pManager.DnsSdTxtRecordListener txtListener =
                (fullDomainName, txtRecordMap, device) -> {
                    if (device == null) return;
                    deviceCache.put(device.deviceAddress, device);

                    String peerName = txtRecordMap.containsKey(TXT_NAME)
                            ? txtRecordMap.get(TXT_NAME) : device.deviceName;
                    String langTag  = txtRecordMap.containsKey(TXT_LANG)
                            ? txtRecordMap.get(TXT_LANG) : "en";

                    Log.d(TAG, "BabelMic peer found: " + peerName + " (" + langTag + ")");
                    callback.onPeerServiceFound(device, peerName, langTag);
                };

        // DNS-SD service response listener — fires when the service itself is found
        WifiP2pManager.DnsSdServiceResponseListener serviceListener =
                (instanceName, registrationType, device) -> {
                    if (device == null) return;
                    deviceCache.put(device.deviceAddress, device);
                    Log.d(TAG, "Service response: " + instanceName + " from " + device.deviceName);
                };

        manager.setDnsSdResponseListeners(channel, serviceListener, txtListener);

        // Add a request specifically for our service type
        WifiP2pDnsSdServiceRequest serviceRequest =
                WifiP2pDnsSdServiceRequest.newInstance(SERVICE_TYPE);

        manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {
                manager.addServiceRequest(channel, serviceRequest,
                        new WifiP2pManager.ActionListener() {
                            @Override public void onSuccess() {
                                discoverServices();
                            }
                            @Override public void onFailure(int reason) {
                                Log.e(TAG, "addServiceRequest failed: " + reason);
                                startPeerDiscoveryFallback();
                            }
                        });
            }
            @Override public void onFailure(int reason) {
                Log.w(TAG, "clearServiceRequests failed: " + reason);
                discoverServices();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void discoverServices() {
        manager.discoverServices(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {
                Log.d(TAG, "Service discovery started");
            }
            @Override public void onFailure(int reason) {
                Log.e(TAG, "discoverServices failed: " + reasonToString(reason));
                // Fall back to plain peer discovery
                startPeerDiscoveryFallback();
            }
        });
    }

    /**
     * Fallback: if service discovery fails (some OEMs don't support DNS-SD properly),
     * use the classic discoverPeers() approach. The peer list won't carry profile data
     * but the connection mechanism still works.
     */
    @SuppressLint("MissingPermission")
    private void startPeerDiscoveryFallback() {
        Log.w(TAG, "Falling back to discoverPeers()");
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {
                Log.d(TAG, "Fallback peer discovery started");
            }
            @Override public void onFailure(int reason) {
                Log.e(TAG, "discoverPeers fallback failed: " + reasonToString(reason));
                callback.onError("Could not start discovery: " + reasonToString(reason));
            }
        });
    }

    public void stopDiscovery() {
        manager.stopPeerDiscovery(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() { Log.d(TAG, "Discovery stopped"); }
            @Override public void onFailure(int r) {}
        });
        manager.clearServiceRequests(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {}
            @Override public void onFailure(int r) {}
        });
    }

    // ─── Connection ───────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    public void connectTo(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {
                Log.d(TAG, "Connect request sent to: " + device.deviceName);
            }
            @Override public void onFailure(int reason) {
                Log.e(TAG, "Connect failed: " + reasonToString(reason));
                callback.onError("Connection failed: " + reasonToString(reason));
            }
        });
    }

    public void disconnect() {
        manager.removeGroup(channel, new WifiP2pManager.ActionListener() {
            @Override public void onSuccess() {
                Log.d(TAG, "P2P group removed");
                callback.onDisconnected();
            }
            @Override public void onFailure(int r) {}
        });
    }

    // ─── BroadcastReceiver ────────────────────────────────────────────────────

    private class P2pBroadcastReceiver extends BroadcastReceiver {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
                    int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                    boolean enabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED);
                    callback.onWifiDirectAvailabilityChanged(enabled);
                    break;
                }

                case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
                    // Fallback path: if discoverPeers() was used, surface peers here
                    manager.requestPeers(channel, peerList -> {
                        for (WifiP2pDevice device : peerList.getDeviceList()) {
                            deviceCache.put(device.deviceAddress, device);
                            // Emit with unknown profile — activity will show default avatar
                            callback.onPeerServiceFound(device,
                                    device.deviceName.isEmpty()
                                            ? device.deviceAddress : device.deviceName,
                                    "en");
                        }
                    });
                    break;
                }

                case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
                    NetworkInfo netInfo =
                            intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                    if (netInfo != null && netInfo.isConnected()) {
                        manager.requestConnectionInfo(channel, (WifiP2pInfo info) -> {
                            if (info.groupFormed) {
                                Log.d(TAG, "Group formed. Owner IP: "
                                        + info.groupOwnerAddress.getHostAddress()
                                        + " isOwner: " + info.isGroupOwner);
                                callback.onConnectionEstablished(
                                        info.groupOwnerAddress, info.isGroupOwner);
                            }
                        });
                    } else {
                        callback.onDisconnected();
                    }
                    break;
                }
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static IntentFilter buildIntentFilter() {
        IntentFilter f = new IntentFilter();
        f.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        f.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        f.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        f.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        return f;
    }

    private static String reasonToString(int r) {
        switch (r) {
            case WifiP2pManager.ERROR:              return "Internal error";
            case WifiP2pManager.P2P_UNSUPPORTED:    return "P2P unsupported";
            case WifiP2pManager.BUSY:               return "Framework busy";
            case WifiP2pManager.NO_SERVICE_REQUESTS:return "No service requests";
            default:                                return "Unknown (" + r + ")";
        }
    }
}
