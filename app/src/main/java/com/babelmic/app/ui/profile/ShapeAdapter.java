package com.babelmic.app.ui.profile;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;
import com.babelmic.app.model.AvatarShape;
import com.babelmic.app.ui.widget.AvatarVisualizerView;

/**
 * Horizontal RecyclerView adapter for the avatar shape picker.
 * Each item shows a mini live {@link AvatarVisualizerView} preview of the shape.
 */
public class ShapeAdapter extends RecyclerView.Adapter<ShapeAdapter.ShapeViewHolder> {

    public interface OnShapeSelectedListener {
        void onShapeSelected(AvatarShape shape);
    }

    private final AvatarShape[]        shapes   = AvatarShape.values();
    private       int                  selected = 0;
    private       int                  colorTop    = Color.parseColor("#1A73E8");
    private       int                  colorBottom = Color.parseColor("#4FC3F7");
    private final OnShapeSelectedListener listener;

    public ShapeAdapter(OnShapeSelectedListener listener) {
        this.listener = listener;
    }

    /** Update the preview colours without changing the selected shape. */
    public void setPreviewColors(int colorTop, int colorBottom) {
        this.colorTop    = colorTop;
        this.colorBottom = colorBottom;
        notifyDataSetChanged();
    }

    public AvatarShape getSelectedShape() {
        return shapes[selected];
    }

    public void setSelectedShape(AvatarShape shape) {
        for (int i = 0; i < shapes.length; i++) {
            if (shapes[i] == shape) { selected = i; notifyDataSetChanged(); return; }
        }
    }

    @NonNull
    @Override
    public ShapeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_avatar_shape, parent, false);
        return new ShapeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ShapeViewHolder holder, int position) {
        AvatarShape shape = shapes[position];
        holder.preview.setAvatar(shape, colorTop, colorBottom);
        // Show a gentle idle pulse so the shape is visible even without speaking
        holder.preview.setState(AvatarVisualizerView.VisualizerState.IDLE);
        holder.tvName.setText(shape.displayName());
        holder.itemView.setSelected(position == selected);
        holder.itemView.setOnClickListener(v -> {
            int prev = selected;
            selected = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selected);
            listener.onShapeSelected(shape);
        });
    }

    @Override
    public int getItemCount() { return shapes.length; }

    static class ShapeViewHolder extends RecyclerView.ViewHolder {
        AvatarVisualizerView preview;
        TextView             tvName;

        ShapeViewHolder(@NonNull View v) {
            super(v);
            preview = v.findViewById(R.id.avatar_preview);
            tvName  = v.findViewById(R.id.tv_shape_name);
        }
    }
}
