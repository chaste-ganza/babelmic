package com.babelmic.app.network;

import android.util.Log;

import com.babelmic.app.core.PeerConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Real implementation of {@link PeerConnection} using TCP sockets over a
 * Wi-Fi Direct P2P group.
 *
 * <p><b>How it works:</b>
 * <ul>
 *   <li>The Wi-Fi Direct <em>group owner</em> acts as the TCP server and calls
 *       {@link #startServer(DataReceivedCallback)}.</li>
 *   <li>The other device (client) calls {@link #connect(String)} with the group
 *       owner's IP address.</li>
 *   <li>Once connected, either side can call {@link #sendData(String)} to stream
 *       translated text to the peer in real time.</li>
 *   <li>Incoming messages are delivered via {@link DataReceivedCallback}.</li>
 * </ul>
 * </p>
 *
 * <p>Messages are newline-delimited UTF-8 strings — simple and effective for
 * translated text payloads of a few hundred characters.</p>
 */
public class WifiDirectPeerConnection implements PeerConnection {

    private static final String TAG = "WifiDirectP2P";

    /** Called whenever a complete message arrives from the peer. */
    public interface DataReceivedCallback {
        void onDataReceived(String data);
        void onConnectionLost(String reason);
    }

    // ─── Fields ───────────────────────────────────────────────────────────────

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private volatile Socket         socket;
    private volatile ServerSocket   serverSocket;
    private volatile PrintWriter    writer;
    private volatile BufferedReader reader;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private volatile DataReceivedCallback dataCallback;

    private String peerAddress = null;

    /** Set the callback that receives incoming messages and connection-lost events. */
    public void setDataCallback(DataReceivedCallback callback) {
        this.dataCallback = callback;
    }

    /** @return the currently registered {@link DataReceivedCallback}. */
    public DataReceivedCallback getDataCallback() {
        return dataCallback;
    }

    // ─── Server side (group owner) ────────────────────────────────────────────

    /**
     * Start a TCP server on {@link WifiDirectManager#BABELMIC_PORT} and wait for
     * the client to connect. Blocking — must be called from a background thread.
     * Set the callback first via {@link #setDataCallback(DataReceivedCallback)}.
     */
    public void startServer() {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Starting server on port " + WifiDirectManager.BABELMIC_PORT);
                serverSocket = new ServerSocket(WifiDirectManager.BABELMIC_PORT);
                socket       = serverSocket.accept(); // blocks until client connects

                peerAddress = socket.getInetAddress().getHostAddress();
                Log.d(TAG, "Client connected from: " + peerAddress);

                initStreams();
                connected.set(true);
                startReading();

            } catch (IOException e) {
                if (!connected.get()) {
                    // Normal close — server was shut down before a client arrived
                    Log.d(TAG, "Server socket closed");
                } else {
                    Log.e(TAG, "Server accept error", e);
                    handleDisconnect("Server error: " + e.getMessage());
                }
            }
        });
    }

    // ─── Client side ─────────────────────────────────────────────────────────

    /**
     * Connect to the group owner's TCP server. Blocking — must be called from a
     * background thread.
     *
     * @param deviceId The group owner's IP address (from {@link WifiDirectManager.WifiDirectCallback}).
     * @return true if the TCP connection was successfully established.
     */
    @Override
    public boolean connect(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device IP address cannot be empty.");
        }

        try {
            Log.d(TAG, "Connecting to server: " + deviceId
                    + ":" + WifiDirectManager.BABELMIC_PORT);

            // Retry up to 5 times — the server might not be listening yet
            int attempts = 0;
            while (attempts < 5) {
                try {
                    socket = new Socket();
                    socket.connect(
                            new java.net.InetSocketAddress(deviceId,
                                    WifiDirectManager.BABELMIC_PORT),
                            3000 /* 3s timeout per attempt */);
                    break; // success
                } catch (IOException e) {
                    attempts++;
                    Log.w(TAG, "Connect attempt " + attempts + " failed — retrying…");
                    Thread.sleep(1000);
                }
            }

            if (!socket.isConnected()) {
                Log.e(TAG, "Could not connect after 5 attempts");
                return false;
            }

            peerAddress = deviceId;
            initStreams();
            connected.set(true);
            startReading();

            Log.d(TAG, "Connected to server: " + deviceId);
            return true;

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "connect() failed", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    // ─── Send ─────────────────────────────────────────────────────────────────

    /**
     * Send a UTF-8 string to the connected peer.
     * Thread-safe; can be called from any thread.
     *
     * @param payload The translated text to transmit.
     */
    @Override
    public void sendData(String payload) {
        if (!connected.get()) {
            throw new IllegalStateException(
                    "Cannot send data — no active P2P connection.");
        }
        if (payload == null || payload.trim().isEmpty()) return;

        executor.execute(() -> {
            if (writer != null) {
                writer.println(payload); // println appends '\n' — our delimiter
                writer.flush();
                Log.d(TAG, "Sent -> " + payload);
            }
        });
    }

    // ─── Disconnect ───────────────────────────────────────────────────────────

    @Override
    public void disconnect() {
        if (!connected.getAndSet(false)) return;

        Log.d(TAG, "Disconnecting from: " + peerAddress);
        closeResources();
        peerAddress = null;
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    // ─── Incoming message loop ────────────────────────────────────────────────

    /**
     * Starts a background thread that continuously reads newline-delimited
     * messages from the peer and delivers them via {@link DataReceivedCallback}.
     */
    private void startReading() {
        executor.execute(() -> {
            try {
                String line;
                while (connected.get() && (line = reader.readLine()) != null) {
                    final String message = line.trim();
                    if (!message.isEmpty() && dataCallback != null) {
                        Log.d(TAG, "Received <- " + message);
                        dataCallback.onDataReceived(message);
                    }
                }
            } catch (SocketException e) {
                // Expected when the socket is closed deliberately
                Log.d(TAG, "Socket closed — read loop ending");
            } catch (IOException e) {
                Log.e(TAG, "Read error", e);
                handleDisconnect("Read error: " + e.getMessage());
            }
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void initStreams() throws IOException {
        writer = new PrintWriter(socket.getOutputStream(), true);
        reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "UTF-8"));
    }

    private void handleDisconnect(String reason) {
        connected.set(false);
        closeResources();
        if (dataCallback != null) {
            dataCallback.onConnectionLost(reason);
        }
    }

    private void closeResources() {
        try { if (writer       != null) writer.close();       } catch (Exception ignored) {}
        try { if (reader       != null) reader.close();       } catch (Exception ignored) {}
        try { if (socket       != null) socket.close();       } catch (Exception ignored) {}
        try { if (serverSocket != null) serverSocket.close(); } catch (Exception ignored) {}
        writer       = null;
        reader       = null;
        socket       = null;
        serverSocket = null;
    }
}
