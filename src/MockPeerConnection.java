public class MockPeerConnection implements PeerConnection {

    private boolean isConnected = false;
    private String connectedDeviceId = null;

    public boolean isConnected(){
        return isConnected;
    }

    @Override
    public boolean connect(String deviceId){
        if (deviceId == null || deviceId.trim().isEmpty()){
            throw new IllegalArgumentException("Device ID cannot be empty.");
        }

        this.connectedDeviceId = deviceId;
        this.isConnected = true;

        System.out.println("[P2P Link]: Connected to device -> " + deviceId);
        return true;
    }

    @Override public void sendData(String payload){
        if(!isConnected){
            throw new IllegalStateException("Cannot send data without an active P2P connection.");
        }
        System.out.println("[P2P Sent to " + connectedDeviceId + "]: " + payload);
    }

    @Override
    public void disconnect(){
        if (isConnected){
            System.out.println("[P2P Link]: Disconnected from device -> " + connectedDeviceId);
            this.isConnected = false;
            this.connectedDeviceId = null;
        }
    }
}
