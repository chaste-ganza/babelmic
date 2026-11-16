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
 *
 * <p>Selection feedback:
 * <ul>
 *   <li>Selected row gets a light purple background (via {@code state_activated}).</li>
 *   <li>A purple checkmark appears on the right.</li>
 *   <li>Previously selected row is explicitly de-highlighted via {@link #notifyItemChanged}.</li>
 * </ul>
 * </p>
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

    /** Replace dataset (used by search filter). Clears selection. */
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
        Language lang    = languages.get(position);
        boolean selected = position == selectedPosition;

        h.tvFlag.setText(lang.getFlagEmoji());
        h.tvName.setText(lang.getDisplayName());
        h.tvNative.setText(lang.getNativeName());

        // Drive both the background selector and the checkmark
        h.itemView.setActivated(selected);
        h.ivSelected.setVisibility(selected ? View.VISIBLE : View.GONE);

        h.itemView.setOnClickListener(v -> {
            int prev        = selectedPosition;
            selectedPosition = h.getAdapterPosition();

            // Redraw the previously selected row to remove its highlight
            if (prev != -1 && prev != selectedPosition) {
                notifyItemChanged(prev);
            }
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
