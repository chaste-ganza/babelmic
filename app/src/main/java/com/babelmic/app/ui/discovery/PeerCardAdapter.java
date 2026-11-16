package com.babelmic.app.ui.discovery;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;
import com.babelmic.app.ui.widget.AvatarVisualizerView;
import com.babelmic.app.util.ProfileManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter that shows discovered Wi-Fi Direct peers as avatar cards.
 *
 * <p>Peer profile data (name, language, avatar shape/colours) is provided via
 * {@link #updatePeerProfile(String, ProfileManager.PeerProfile)} as handshake
 * data arrives over the socket. Until a profile arrives the card shows the
 * device's Wi-Fi Direct name and a default avatar.</p>
 */
public class PeerCardAdapter extends RecyclerView.Adapter<PeerCardAdapter.PeerViewHolder> {

    public interface OnPeerSelectedListener {
        void onPeerSelected(WifiP2pDevice device);
    }

    private final List<WifiP2pDevice>                       devices  = new ArrayList<>();
    private final Map<String, ProfileManager.PeerProfile>   profiles = new HashMap<>();
    private final OnPeerSelectedListener                     listener;

    public PeerCardAdapter(OnPeerSelectedListener listener) {
        this.listener = listener;
    }

    public void updateDevices(List<WifiP2pDevice> newDevices) {
        devices.clear();
        devices.addAll(newDevices);
        notifyDataSetChanged();
    }

    /** Call when a profile handshake packet arrives for a specific device address. */
    public void updatePeerProfile(String deviceAddress, ProfileManager.PeerProfile profile) {
        profiles.put(deviceAddress, profile);
        for (int i = 0; i < devices.size(); i++) {
            if (devices.get(i).deviceAddress.equals(deviceAddress)) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_peer_card, parent, false);
        return new PeerViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        WifiP2pDevice device = devices.get(position);
        ProfileManager.PeerProfile profile = profiles.get(device.deviceAddress);

        if (profile != null) {
            holder.tvName.setText(profile.name);
            holder.tvLanguage.setText(
                    getLanguageFlag(profile.languageTag) + "  " + profile.languageName);
            holder.avatar.setAvatar(profile.avatarShape, profile.colorTop, profile.colorBottom);
        } else {
            // No handshake yet — show device name and default avatar
            String name = device.deviceName.isEmpty() ? device.deviceAddress : device.deviceName;
            holder.tvName.setText(name);
            holder.tvLanguage.setText("Connecting…");
            holder.avatar.setAvatar(
                    com.babelmic.app.model.AvatarShape.CIRCLE,
                    0xFF9E9E9E, 0xFF616161);
        }

        holder.avatar.setState(AvatarVisualizerView.VisualizerState.IDLE);
        holder.itemView.setOnClickListener(v -> listener.onPeerSelected(device));
    }

    @Override
    public int getItemCount() { return devices.size(); }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String getLanguageFlag(String tag) {
        com.babelmic.app.model.Language lang =
                com.babelmic.app.util.LanguageManager.findByTag(tag);
        return lang != null ? lang.getFlagEmoji() : "🌐";
    }

    static class PeerViewHolder extends RecyclerView.ViewHolder {
        AvatarVisualizerView avatar;
        TextView             tvName;
        TextView             tvLanguage;

        PeerViewHolder(@NonNull View v) {
            super(v);
            avatar     = v.findViewById(R.id.peer_avatar);
            tvName     = v.findViewById(R.id.tv_peer_name);
            tvLanguage = v.findViewById(R.id.tv_peer_language);
        }
    }
}
