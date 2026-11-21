package com.babelmic.app.ui.profile;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;

/**
 * 3-column grid adapter for the avatar picker bottom sheet.
 * Selected avatar gets the user's identity colour ring.
 */
public class AvatarGridAdapter
        extends RecyclerView.Adapter<AvatarGridAdapter.AvatarHolder> {

    public interface OnAvatarSelectedListener {
        void onAvatarSelected(int drawableResId, String avatarName);
    }

    public static final int[] AVATAR_RES = {
        R.drawable.avatar_cat,
        R.drawable.avatar_goat,
        R.drawable.avatar_rabbit,
        R.drawable.avatar_panda,
        R.drawable.avatar_donkey,
        R.drawable.avatar_bear_bow,
        R.drawable.avatar_elephant,
        R.drawable.avatar_bird,
        R.drawable.avatar_lion,
        R.drawable.avatar_cow,
        R.drawable.avatar_bear,
        R.drawable.avatar_fox_top,
        R.drawable.avatar_fox,
        R.drawable.avatar_owl,
        R.drawable.avatar_tiger,
        R.drawable.avatar_sheep,
    };

    public static final String[] AVATAR_NAMES = {
        "avatar_cat", "avatar_goat", "avatar_rabbit", "avatar_panda",
        "avatar_donkey", "avatar_bear_bow", "avatar_elephant", "avatar_bird",
        "avatar_lion", "avatar_cow", "avatar_bear", "avatar_fox_top",
        "avatar_fox", "avatar_owl", "avatar_tiger", "avatar_sheep",
    };

    private int selected = -1;
    private int ringColor = 0xFFFF2D78; // default pink until user picks
    private final OnAvatarSelectedListener listener;

    public AvatarGridAdapter(OnAvatarSelectedListener listener) {
        this.listener = listener;
    }

    public void setRingColor(int color) {
        this.ringColor = color;
        notifyDataSetChanged();
    }

    public void setSelectedByName(String name) {
        for (int i = 0; i < AVATAR_NAMES.length; i++) {
            if (AVATAR_NAMES[i].equals(name)) { selected = i; notifyDataSetChanged(); return; }
        }
    }

    public int getSelectedPosition() { return selected; }

    @NonNull @Override
    public AvatarHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar_grid, parent, false);
        return new AvatarHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarHolder h, int pos) {
        h.image.setImageResource(AVATAR_RES[pos]);

        boolean isSelected = pos == selected;
        h.ring.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        if (isSelected) {
            android.graphics.drawable.Drawable d = h.ring.getBackground();
            if (d != null) {
                d = d.mutate();
                d.setColorFilter(ringColor, android.graphics.PorterDuff.Mode.SRC_IN);
                h.ring.setBackground(d);
            }
        }

        h.itemView.setOnClickListener(v -> {
            int prev = selected;
            selected = h.getAdapterPosition();
            if (prev >= 0) notifyItemChanged(prev);
            notifyItemChanged(selected);
            listener.onAvatarSelected(AVATAR_RES[selected], AVATAR_NAMES[selected]);
        });
    }

    @Override public int getItemCount() { return AVATAR_RES.length; }

    static class AvatarHolder extends RecyclerView.ViewHolder {
        ImageView image;
        View ring;
        AvatarHolder(@NonNull View v) {
            super(v);
            image = v.findViewById(R.id.iv_avatar);
            ring  = v.findViewById(R.id.v_selection_ring);
        }
    }
}
