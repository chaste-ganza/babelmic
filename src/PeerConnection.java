public interface PeerConnection {
    boolean connect(String deviceId);
    void sendData(String payload);
    void disconnect();
}
