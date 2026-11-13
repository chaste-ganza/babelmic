package com.babelmic.app.mock;

import com.babelmic.app.core.PeerConnection;

/**
 * Mock implementation of {@link PeerConnection} for unit testing.
 * Simulates connection state and data transmission with console output.
 */
public class MockPeerConnection implements PeerConnection {

    private boolean connected = false;
    private String connectedDeviceId = null;

    @Override
    public boolean connect(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be empty.");
        }
        this.connectedDeviceId = deviceId;
        this.connected = true;
        System.out.println("[MockP2P]: Connected to -> " + deviceId);
        return true;
    }

    @Override
    public void sendData(String payload) {
        if (!connected) {
            throw new IllegalStateException("Cannot send data without an active P2P connection.");
        }
        System.out.println("[MockP2P -> " + connectedDeviceId + "]: " + payload);
    }

    @Override
    public void disconnect() {
        if (connected) {
            System.out.println("[MockP2P]: Disconnected from -> " + connectedDeviceId);
            connected = false;
            connectedDeviceId = null;
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
