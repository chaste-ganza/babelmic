package com.babelmic.app.core;

/**
 * Contract for the device-to-device communication channel.
 * The real implementation uses Android Wi-Fi Direct P2P sockets.
 */
public interface PeerConnection {
    /**
     * Establish a connection to a remote device.
     *
     * @param deviceId  Wi-Fi Direct peer identifier or IP address
     * @return          true if the connection was successfully established
     */
    boolean connect(String deviceId);

    /**
     * Send a UTF-8 string payload to the connected peer.
     * Throws {@link IllegalStateException} if not connected.
     *
     * @param payload  The text to transmit (translated speech)
     */
    void sendData(String payload);

    /** Cleanly tear down the active connection. */
    void disconnect();

    /** @return true if a peer connection is currently active */
    boolean isConnected();
}
