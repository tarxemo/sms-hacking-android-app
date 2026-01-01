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
    private String currentDeviceId;

    public MessageAdapter(List<Map<String, Object>> messages, String currentDeviceId) {
        this.messages = messages;
        this.currentDeviceId = currentDeviceId;
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

        String timestamp = (String) msg.get("timestamp");
        if (timestamp != null && timestamp.length() > 10) {
            // Usually 'yyyy-MM-dd HH:mm:ss', extract HH:mm
            try {
                holder.tvTimestamp.setText(timestamp.substring(11, 16));
            } catch (Exception e) {
                holder.tvTimestamp.setText(timestamp);
            }
        } else {
            holder.tvTimestamp.setText(timestamp);
        }

        // Check if message was sent by THIS device
        boolean isMe = currentDeviceId != null && currentDeviceId.equals(String.valueOf(msg.get("device_id")));
        
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.bubbleLayout.getLayoutParams();
        if (isMe) {
            holder.container.setGravity(Gravity.END);
            holder.bubbleLayout.setBackgroundResource(R.drawable.bg_chat_bubble_sent);
            holder.tvMessage.setTextColor(android.graphics.Color.WHITE);
            holder.tvTimestamp.setTextColor(android.graphics.Color.parseColor("#CCFFFFFF"));
            holder.tvDeviceName.setVisibility(View.GONE);
            holder.ivStatus.setVisibility(View.VISIBLE);
            
            boolean isRead = Boolean.TRUE.equals(msg.get("is_read"));
            if (isRead) {
                holder.ivStatus.setImageResource(R.drawable.ic_tick_read);
            } else {
                holder.ivStatus.setImageResource(R.drawable.ic_tick_sent);
            }
        } else {
            holder.container.setGravity(Gravity.START);
            holder.bubbleLayout.setBackgroundResource(R.drawable.bg_chat_bubble_received);
            holder.tvMessage.setTextColor(android.graphics.Color.BLACK);
            holder.tvTimestamp.setTextColor(android.graphics.Color.parseColor("#88000000"));
            holder.ivStatus.setVisibility(View.GONE);
            
            String deviceName = (String) msg.get("device_name");
            if (deviceName != null && !deviceName.isEmpty()) {
                holder.tvDeviceName.setText(deviceName);
                holder.tvDeviceName.setVisibility(View.VISIBLE);
            } else {
                holder.tvDeviceName.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvDeviceName, tvTimestamp;
        android.widget.ImageView ivStatus;
        android.view.View bubbleLayout;
        LinearLayout container;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message_text);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            ivStatus = itemView.findViewById(R.id.iv_status);
            bubbleLayout = itemView.findViewById(R.id.bubble_layout);
            container = itemView.findViewById(R.id.chat_bubble_container);
        }
    }
}
