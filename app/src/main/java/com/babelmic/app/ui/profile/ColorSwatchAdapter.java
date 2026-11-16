package com.babelmic.app.ui.profile;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;

/**
 * Horizontal RecyclerView adapter for the color swatch row on the avatar setup screen.
 * Swatches are large rounded squares; the selected one shows a white checkmark.
 */
public class ColorSwatchAdapter
        extends RecyclerView.Adapter<ColorSwatchAdapter.SwatchViewHolder> {

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    // The six colours shown in the mockup
    private static final int[] COLORS = {
        0xFF5B21F5, // Purple
        0xFFFFEB00, // Yellow
        0xFFFF3B82, // Pink
        0xFF4ADE80, // Green
        0xFF3B82F6, // Blue
        0xFFEF4444, // Red
    };

    private int selected = 0;
    private final OnColorSelectedListener listener;

    public ColorSwatchAdapter(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedColor() { return COLORS[selected]; }

    public void setSelectedColor(int color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == color) { selected = i; notifyDataSetChanged(); return; }
        }
    }

    @NonNull @Override
    public SwatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_swatch, parent, false);
        return new SwatchViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SwatchViewHolder h, int position) {
        int color = COLORS[position];

        // Tint the rounded square background
        GradientDrawable bg = (GradientDrawable)
                h.itemView.getBackground().mutate();
        bg.setColor(color);

        // Show checkmark on selected
        h.check.setVisibility(position == selected ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> {
            int prev = selected;
            selected = h.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selected);
            listener.onColorSelected(color);
        });
    }

    @Override public int getItemCount() { return COLORS.length; }

    static class SwatchViewHolder extends RecyclerView.ViewHolder {
        View check;
        SwatchViewHolder(@NonNull View v) {
            super(v);
            check = v.findViewById(R.id.iv_check);
        }
    }
}
