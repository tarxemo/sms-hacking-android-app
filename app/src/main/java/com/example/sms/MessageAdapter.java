package com.example.sms;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Map<String, Object>> messages;
    private String currentUserId;

    public MessageAdapter(List<Map<String, Object>> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_bubble, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Map<String, Object> msg = messages.get(position);
        holder.tvMessage.setText((String) msg.get("message"));

        // For decoy, we assume sender is user 1
        boolean isMe = "1".equals(String.valueOf(msg.get("sender")));
        
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.tvMessage.getLayoutParams();
        if (isMe) {
            holder.container.setGravity(Gravity.END);
            holder.tvMessage.setBackgroundResource(R.drawable.background_message_sent);
            holder.tvMessage.setTextColor(android.graphics.Color.WHITE);
        } else {
            holder.container.setGravity(Gravity.START);
            holder.tvMessage.setBackgroundResource(R.drawable.background_message_received);
            holder.tvMessage.setTextColor(android.graphics.Color.BLACK);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        LinearLayout container;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_text);
            container = itemView.findViewById(R.id.chat_bubble_container);
        }
    }
}
