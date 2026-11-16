package com.babelmic.app.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;
import com.babelmic.app.model.AvatarShape;
import com.babelmic.app.ui.widget.AvatarVisualizerView;

/**
 * Horizontal RecyclerView adapter for the shape picker row on the avatar setup screen.
 * Each item is a small square tile containing a live AvatarVisualizerView preview.
 */
public class ShapePickerAdapter
        extends RecyclerView.Adapter<ShapePickerAdapter.ShapeViewHolder> {

    public interface OnShapeSelectedListener {
        void onShapeSelected(AvatarShape shape);
    }

    private final AvatarShape[]       shapes   = AvatarShape.values();
    private int                        selected = 0; // HEART by default
    private int                        previewColor = 0xFF5B21F5;
    private final OnShapeSelectedListener listener;

    public ShapePickerAdapter(OnShapeSelectedListener listener) {
        this.listener = listener;
    }

    public void setPreviewColor(int color) {
        this.previewColor = color;
        notifyDataSetChanged();
    }

    public AvatarShape getSelectedShape() { return shapes[selected]; }

    public void setSelectedShape(AvatarShape shape) {
        for (int i = 0; i < shapes.length; i++) {
            if (shapes[i] == shape) { selected = i; notifyDataSetChanged(); return; }
        }
    }

    @NonNull @Override
    public ShapeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_shape_picker, parent, false);
        return new ShapeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ShapeViewHolder h, int position) {
        AvatarShape shape = shapes[position];
        h.preview.setAvatar(shape, previewColor, previewColor);
        h.preview.setState(AvatarVisualizerView.VisualizerState.IDLE);
        h.itemView.setSelected(position == selected);
        h.itemView.setOnClickListener(v -> {
            int prev = selected;
            selected = h.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selected);
            listener.onShapeSelected(shape);
        });
    }

    @Override public int getItemCount() { return shapes.length; }

    static class ShapeViewHolder extends RecyclerView.ViewHolder {
        AvatarVisualizerView preview;
        ShapeViewHolder(@NonNull View v) {
            super(v);
            preview = v.findViewById(R.id.shape_preview);
        }
    }
}
