package com.babelmic.app.ui.conversation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the conversation dialogue feed.
 *
 * <p>Each entry is a {@link DialogueLine}: a speaker name (in their identity colour)
 * and the text they said/the translation they received.</p>
 *
 * <p>Older items (further from the bottom) are progressively faded out to grey
 * to visually indicate recency — matching the Spotify-lyrics-style described in
 * the design brief. The two most recent lines are shown at full opacity;
 * older lines fade toward {@code #CCCCCC}.</p>
 */
public class DialogueAdapter extends RecyclerView.Adapter<DialogueAdapter.LineHolder> {

    public static class DialogueLine {
        public final String speakerName;
        public final int    nameColor;    // identity colour of the speaker
        public final String text;

        public DialogueLine(String speakerName, int nameColor, String text) {
            this.speakerName = speakerName;
            this.nameColor   = nameColor;
            this.text        = text;
        }
    }

    private final List<DialogueLine> lines = new ArrayList<>();

    public void addLine(DialogueLine line) {
        lines.add(line);
        notifyItemInserted(lines.size() - 1);
        // Re-draw the previous items so their fade level updates
        if (lines.size() > 1) notifyItemChanged(lines.size() - 2);
    }

    public void clear() {
        lines.clear();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public LineHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dialogue_line, parent, false);
        return new LineHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LineHolder h, int pos) {
        DialogueLine line = lines.get(pos);
        h.tvName.setText(line.speakerName);
        h.tvName.setTextColor(line.nameColor);
        h.tvText.setText(line.text);

        // Fade older items: last 2 items full opacity, older items fade to grey
        int distFromEnd = lines.size() - 1 - pos;
        if (distFromEnd == 0) {
            // Current / most recent — full opacity, full colour
            h.tvText.setTextColor(0xFF111111);
            h.tvText.setAlpha(1f);
            h.tvName.setAlpha(1f);
        } else if (distFromEnd == 1) {
            // One back — slightly faded
            h.tvText.setTextColor(0xFF666666);
            h.tvText.setAlpha(0.75f);
            h.tvName.setAlpha(0.75f);
        } else {
            // Older — greyed out
            float alpha = Math.max(0.2f, 0.6f - (distFromEnd - 1) * 0.15f);
            h.tvText.setTextColor(0xFFAAAAAA);
            h.tvText.setAlpha(alpha);
            h.tvName.setAlpha(alpha);
        }
    }

    @Override public int getItemCount() { return lines.size(); }

    static class LineHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvText;
        LineHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tv_speaker_name);
            tvText = v.findViewById(R.id.tv_line_text);
        }
    }
}
