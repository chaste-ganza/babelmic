package com.babelmic.app.ui.discovery;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.babelmic.app.databinding.BottomSheetPendingBinding;
import com.babelmic.app.model.SessionConfig;
import com.babelmic.app.ui.connected.ConnectedSuccessfullyActivity;
import com.babelmic.app.util.ProfileManager;
import com.babelmic.app.util.LanguageManager;
import com.babelmic.app.model.Language;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet shown over DeviceDiscoveryActivity when WE initiated the connection.
 * Displays: your avatar >>> peer avatar, with a Revoke button.
 * Confirm navigates to ConnectedSuccessfullyActivity → ConversationActivity.
 */
public class PendingBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_PEER_NAME   = "peer_name";
    private static final String ARG_PEER_RES    = "peer_res";
    private static final String ARG_PEER_COLOR  = "peer_color";
    private static final String ARG_IS_OWNER    = "is_owner";
    private static final String ARG_PEER_IP     = "peer_ip";

    private BottomSheetPendingBinding binding;

    public static PendingBottomSheet newInstance(String peerName, int peerAvatarRes,
                                                  int peerColor, boolean isOwner, String peerIp) {
        PendingBottomSheet sheet = new PendingBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PEER_NAME,  peerName);
        args.putInt(ARG_PEER_RES,      peerAvatarRes);
        args.putInt(ARG_PEER_COLOR,    peerColor);
        args.putBoolean(ARG_IS_OWNER,  isOwner);
        args.putString(ARG_PEER_IP,    peerIp);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPendingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setCancelable(false);

        Bundle args     = requireArguments();
        String peerName = args.getString(ARG_PEER_NAME, "Partner");
        int peerRes     = args.getInt(ARG_PEER_RES);
        int peerColor   = args.getInt(ARG_PEER_COLOR);
        boolean isOwner = args.getBoolean(ARG_IS_OWNER);
        String peerIp   = args.getString(ARG_PEER_IP);

        // My avatar
        int myRes   = ProfileManager.getAvatarResId(requireContext());
        int myColor = ProfileManager.getIdentityColor(requireContext());
        binding.badgeMyAvatar.set(myRes != 0 ? myRes : com.babelmic.app.R.drawable.avatar_bird, myColor);
        binding.tvMyName.setText(ProfileManager.getDisplayName(requireContext()));

        // Peer avatar
        binding.badgePeerAvatar.set(peerRes, peerColor);
        binding.tvPeerName.setText(peerName);

        // Revoke
        binding.btnRevoke.setOnClickListener(v -> dismiss());

        // The user has already tapped "Connect peer" so we go to conversation
        // as soon as they confirm here (the Wi-Fi Direct group is already formed)
        // For now, navigate directly to Connected screen
        goToConnected(isOwner, peerIp);
    }

    private void goToConnected(boolean isOwner, String peerIp) {
        // Navigate via Connected screen which auto-advances to Conversation
        Intent intent = new Intent(requireContext(), ConnectedSuccessfullyActivity.class);
        intent.putExtra(SessionConfig.EXTRA_IS_GROUP_OWNER,   isOwner);
        intent.putExtra(SessionConfig.EXTRA_PEER_IP_ADDRESS,  peerIp);
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_TAG,
                ProfileManager.getLanguageTag(requireContext()));
        intent.putExtra(SessionConfig.EXTRA_MY_LANGUAGE_NAME,
                ProfileManager.getLanguageName(requireContext()));
        startActivity(intent);
        dismiss();
    }

    @Override public void onDestroyView() { super.onDestroyView(); binding = null; }
}
