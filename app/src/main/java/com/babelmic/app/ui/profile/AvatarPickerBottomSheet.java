package com.babelmic.app.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;

import com.babelmic.app.databinding.BottomSheetAvatarPickerBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Bottom sheet showing a 3-column grid of 16 animal avatars.
 * Dismissed with the selected avatar on Confirm.
 */
public class AvatarPickerBottomSheet extends BottomSheetDialogFragment {

    public interface OnAvatarConfirmedListener {
        void onAvatarConfirmed(int drawableResId, String avatarName);
    }

    private BottomSheetAvatarPickerBinding binding;
    private AvatarGridAdapter gridAdapter;
    private OnAvatarConfirmedListener confirmListener;
    private int ringColor = 0xFFFF2D78;
    private String preselectedName = null;

    public static AvatarPickerBottomSheet newInstance(int ringColor, String currentAvatarName) {
        AvatarPickerBottomSheet sheet = new AvatarPickerBottomSheet();
        sheet.ringColor       = ringColor;
        sheet.preselectedName = currentAvatarName;
        return sheet;
    }

    public void setOnAvatarConfirmedListener(OnAvatarConfirmedListener l) {
        this.confirmListener = l;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetAvatarPickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gridAdapter = new AvatarGridAdapter((resId, name) -> { /* selection tracked in adapter */ });
        gridAdapter.setRingColor(ringColor);
        if (preselectedName != null) gridAdapter.setSelectedByName(preselectedName);

        binding.rvAvatars.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.rvAvatars.setAdapter(gridAdapter);

        binding.btnConfirm.setOnClickListener(v -> {
            int pos = gridAdapter.getSelectedPosition();
            if (pos >= 0 && confirmListener != null) {
                confirmListener.onAvatarConfirmed(
                        AvatarGridAdapter.AVATAR_RES[pos],
                        AvatarGridAdapter.AVATAR_NAMES[pos]);
            }
            dismiss();
        });
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
