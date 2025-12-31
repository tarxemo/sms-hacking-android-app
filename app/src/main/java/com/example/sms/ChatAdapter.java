package com.example.sms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private List<Map<String, Object>> chats;

    public ChatAdapter(List<Map<String, Object>> chats) {
        this.chats = chats;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Map<String, Object> chat = chats.get(position);
        
        holder.tvName.setText("Contact " + (position + 1));
        holder.tvLastMsg.setText((String) chat.get("message"));
        holder.tvTime.setText("Today");

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), ChatActivity.class);
            intent.putExtra("recipient_name", holder.tvName.getText().toString());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return chats.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvLastMsg, tvTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_chat_name);
            tvLastMsg = itemView.findViewById(R.id.tv_last_message);
            tvTime = itemView.findViewById(R.id.tv_chat_time);
        }
    }
}
