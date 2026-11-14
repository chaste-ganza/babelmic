package com.babelmic.app.ui.language;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;
import com.babelmic.app.model.Language;

import java.util.List;

/**
 * RecyclerView adapter for the language selection list.
 * Highlights the currently selected item and fires {@link OnLanguageSelectedListener}
 * on tap.
 */
public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(Language language);
    }

    private List<Language> languages;
    private final OnLanguageSelectedListener listener;
    private int selectedPosition = -1;

    public LanguageAdapter(List<Language> languages, OnLanguageSelectedListener listener) {
        this.languages = languages;
        this.listener  = listener;
    }

    /** Replace the current dataset (used by search filtering). */
    public void updateList(List<Language> newList) {
        this.languages = newList;
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        Language lang = languages.get(position);
        holder.tvFlag.setText(lang.getFlagEmoji());
        holder.tvDisplayName.setText(lang.getDisplayName());
        holder.tvNativeName.setText(lang.getNativeName());

        // Highlight selected row
        holder.itemView.setSelected(position == selectedPosition);

        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onLanguageSelected(lang);
        });
    }

    @Override
    public int getItemCount() {
        return languages.size();
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class LanguageViewHolder extends RecyclerView.ViewHolder {
        TextView tvFlag;
        TextView tvDisplayName;
        TextView tvNativeName;

        LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFlag        = itemView.findViewById(R.id.tv_flag);
            tvDisplayName = itemView.findViewById(R.id.tv_language_name);
            tvNativeName  = itemView.findViewById(R.id.tv_native_name);
        }
    }
}
