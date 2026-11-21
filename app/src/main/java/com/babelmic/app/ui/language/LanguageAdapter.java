package com.babelmic.app.ui.language;

import android.graphics.drawable.GradientDrawable;
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
 * Language list adapter.
 * Selected row gets a full blue-tinted pill background (bg_language_selected).
 * Previously selected row is explicitly cleared.
 */
public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LangHolder> {

    public interface OnLanguageSelectedListener {
        void onLanguageSelected(Language language);
    }

    private List<Language> languages;
    private final OnLanguageSelectedListener listener;
    private int selected = -1;

    public LanguageAdapter(List<Language> languages, OnLanguageSelectedListener listener) {
        this.languages = languages;
        this.listener  = listener;
    }

    public void updateList(List<Language> newList) {
        this.languages = newList;
        this.selected  = -1;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public LangHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_language, parent, false);
        return new LangHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LangHolder h, int pos) {
        Language lang = languages.get(pos);
        h.tvFlag.setText(lang.getFlagEmoji());
        h.tvName.setText(lang.getDisplayName());
        h.tvNative.setText(lang.getNativeName());

        // Pill highlight
        if (pos == selected) {
            h.root.setBackgroundResource(R.drawable.bg_language_selected);
        } else {
            h.root.setBackground(null);
        }

        h.itemView.setOnClickListener(v -> {
            int prev = selected;
            selected = h.getAdapterPosition();
            if (prev >= 0) notifyItemChanged(prev);
            notifyItemChanged(selected);
            listener.onLanguageSelected(lang);
        });
    }

    @Override public int getItemCount() { return languages.size(); }

    static class LangHolder extends RecyclerView.ViewHolder {
        View     root;
        TextView tvFlag, tvName, tvNative;
        LangHolder(@NonNull View v) {
            super(v);
            root     = v.findViewById(R.id.row_root);
            tvFlag   = v.findViewById(R.id.tv_flag);
            tvName   = v.findViewById(R.id.tv_language_name);
            tvNative = v.findViewById(R.id.tv_native_name);
        }
    }
}
