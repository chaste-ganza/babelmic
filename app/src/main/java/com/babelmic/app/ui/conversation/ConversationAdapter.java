package com.babelmic.app.ui.conversation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.babelmic.app.R;
import com.babelmic.app.model.TranslationMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for the live conversation feed.
 *
 * <p>Uses two view types — {@code VIEW_TYPE_OUTGOING} and {@code VIEW_TYPE_INCOMING}
 * — to render chat-bubble-style rows on the right and left sides respectively.</p>
 */
public class ConversationAdapter
        extends RecyclerView.Adapter<ConversationAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_OUTGOING = 0;
    private static final int VIEW_TYPE_INCOMING = 1;

    private final List<TranslationMessage> messages = new ArrayList<>();

    /** Append a message to the bottom of the feed. */
    public void addMessage(TranslationMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isOutgoing()
                ? VIEW_TYPE_OUTGOING
                : VIEW_TYPE_INCOMING;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = (viewType == VIEW_TYPE_OUTGOING)
                ? R.layout.item_message_outgoing
                : R.layout.item_message_incoming;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        TranslationMessage msg = messages.get(position);
        holder.tvOriginal.setText(msg.getOriginalText());
        holder.tvTranslated.setText(msg.getTranslatedText());
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    // ─── ViewHolder ───────────────────────────────────────────────────────────

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvOriginal;
        TextView tvTranslated;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOriginal   = itemView.findViewById(R.id.tv_original_text);
            tvTranslated = itemView.findViewById(R.id.tv_translated_text);
        }
    }
}
