package com.babelmic.app.ui.language;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;
import com.babelmic.app.model.Language;

import java.util.List;

/**
 * RecyclerView adapter for the language selection list.
 * Shows: flag emoji | language name (bold) | native name | check on selected row.
 */
public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LangViewHolder> {

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(Language language);
    }

    private List<Language>              languages;
    private final OnLanguageSelectedListener listener;
    private int                         selectedPosition = -1;

    public LanguageAdapter(List<Language> languages, OnLanguageSelectedListener listener) {
        this.languages = languages;
        this.listener  = listener;
    }

    public void updateList(List<Language> newList) {
        this.languages       = newList;
        this.selectedPosition = -1;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public LangViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new LangViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LangViewHolder h, int position) {
        Language lang = languages.get(position);
        h.tvFlag.setText(lang.getFlagEmoji());
        h.tvName.setText(lang.getDisplayName());
        h.tvNative.setText(lang.getNativeName());
        h.ivSelected.setVisibility(position == selectedPosition ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> {
            int prev        = selectedPosition;
            selectedPosition = h.getAdapterPosition();
            notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);
            listener.onLanguageSelected(lang);
        });
    }

    @Override public int getItemCount() { return languages.size(); }

    static class LangViewHolder extends RecyclerView.ViewHolder {
        TextView  tvFlag, tvName, tvNative;
        ImageView ivSelected;

        LangViewHolder(@NonNull View v) {
            super(v);
            tvFlag     = v.findViewById(R.id.tv_flag);
            tvName     = v.findViewById(R.id.tv_language_name);
            tvNative   = v.findViewById(R.id.tv_native_name);
            ivSelected = v.findViewById(R.id.iv_selected);
        }
    }
}
