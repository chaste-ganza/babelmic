package com.babelmic.app.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;

/**
 * Horizontal RecyclerView adapter for the avatar image picker.
 * Shows circular avatar thumbnails; tapping one selects it with a purple ring.
 */
public class AvatarPickerAdapter
        extends RecyclerView.Adapter<AvatarPickerAdapter.AvatarViewHolder> {

    public interface OnAvatarSelectedListener {
        void onAvatarSelected(int avatarResId, int adapterPosition);
    }

    /** All available avatar drawables in order. */
    public static final int[] AVATARS = {
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6,
    };

    private int selected = 0;
    private final OnAvatarSelectedListener listener;

    public AvatarPickerAdapter(OnAvatarSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedResId() {
        return AVATARS[selected];
    }

    public void setSelectedByResId(int resId) {
        for (int i = 0; i < AVATARS.length; i++) {
            if (AVATARS[i] == resId) { selected = i; notifyDataSetChanged(); return; }
        }
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar_picker, parent, false);
        return new AvatarViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder h, int position) {
        h.image.setImageResource(AVATARS[position]);
        h.itemView.setSelected(position == selected);
        h.itemView.setOnClickListener(v -> {
            int prev = selected;
            selected = h.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selected);
            listener.onAvatarSelected(AVATARS[selected], selected);
        });
    }

    @Override
    public int getItemCount() { return AVATARS.length; }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        AvatarViewHolder(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.iv_avatar);
        }
    }
}
