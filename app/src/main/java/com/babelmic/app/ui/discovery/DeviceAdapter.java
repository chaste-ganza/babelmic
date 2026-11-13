package com.babelmic.app.ui.discovery;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter that shows the list of discovered Wi-Fi Direct peers.
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(WifiP2pDevice device);
    }

    private final List<WifiP2pDevice> devices = new ArrayList<>();
    private final OnDeviceSelectedListener listener;

    public DeviceAdapter(OnDeviceSelectedListener listener) {
        this.listener = listener;
    }

    /** Replace the device list with a fresh scan result. */
    public void updateDevices(List<WifiP2pDevice> newDevices) {
        devices.clear();
        devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        WifiP2pDevice device = devices.get(position);

        holder.tvDeviceName.setText(
                device.deviceName.isEmpty() ? "Unknown Device" : device.deviceName);
        holder.tvDeviceAddress.setText(device.deviceAddress);
        holder.tvStatus.setText(statusToString(device.status));

        holder.itemView.setOnClickListener(v -> listener.onDeviceSelected(device));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        TextView tvDeviceAddress;
        TextView tvStatus;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName    = itemView.findViewById(R.id.tv_device_name);
            tvDeviceAddress = itemView.findViewById(R.id.tv_device_address);
            tvStatus        = itemView.findViewById(R.id.tv_device_status);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String statusToString(int status) {
        switch (status) {
            case WifiP2pDevice.CONNECTED:    return "Connected";
            case WifiP2pDevice.INVITED:      return "Invited";
            case WifiP2pDevice.FAILED:       return "Failed";
            case WifiP2pDevice.AVAILABLE:    return "Available";
            case WifiP2pDevice.UNAVAILABLE:  return "Unavailable";
            default:                         return "Unknown";
        }
    }
}
