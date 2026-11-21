package com.babelmic.app.ui.profile;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;

/**
 * Horizontal RecyclerView of circular colour swatches.
 * Selected swatch shows a light grey outer ring.
 */
public class ColorSwatchAdapter
        extends RecyclerView.Adapter<ColorSwatchAdapter.SwatchHolder> {

    public interface OnColorSelectedListener { void onColorSelected(int color); }

    public static final int[] COLORS = {
        0xFF1E40FF, // blue
        0xFFFFD600, // yellow
        0xFFFF2D78, // pink
        0xFF4ADE80, // green
        0xFFA855F7, // purple
        0xFFEF4444, // red
    };

    private int selected = 0;
    private final OnColorSelectedListener listener;

    public ColorSwatchAdapter(OnColorSelectedListener listener) { this.listener = listener; }

    public int getSelectedColor() { return COLORS[selected]; }

    public void setSelectedColor(int color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == color) { selected = i; notifyDataSetChanged(); return; }
        }
    }

    @NonNull @Override
    public SwatchHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_color_swatch, parent, false);
        return new SwatchHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SwatchHolder h, int pos) {
        // Tint the filled circle
        GradientDrawable circle = (GradientDrawable) h.swatch.getBackground().mutate();
        circle.setColor(COLORS[pos]);

        // Selection ring visibility
        h.ring.setVisibility(pos == selected ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> {
            int prev = selected;
            selected = h.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selected);
            listener.onColorSelected(COLORS[selected]);
        });
    }

    @Override public int getItemCount() { return COLORS.length; }

    static class SwatchHolder extends RecyclerView.ViewHolder {
        View swatch, ring;
        SwatchHolder(@NonNull View v) {
            super(v);
            swatch = v.findViewById(R.id.v_swatch);
            ring   = v.findViewById(R.id.v_selection_ring);
        }
    }
}
