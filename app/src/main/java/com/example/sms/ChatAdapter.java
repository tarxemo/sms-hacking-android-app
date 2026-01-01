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
    private String currentDeviceId;

    public ChatAdapter(List<Map<String, Object>> chats, String currentDeviceId) {
        this.chats = chats;
        this.currentDeviceId = currentDeviceId;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Map<String, Object> device = chats.get(position);
        String deviceId = (String) device.get("device_id");
        String deviceName = (String) device.get("device_name");
        
        if (currentDeviceId != null && currentDeviceId.equals(deviceId)) {
            holder.tvName.setText(deviceName + " (This Device)");
        } else {
            holder.tvName.setText(deviceName != null ? deviceName : "Unknown Device");
        }
        
        holder.tvLastMsg.setText(deviceId);
        holder.tvTime.setText("");

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(v.getContext(), ChatActivity.class);
            intent.putExtra("recipient_name", holder.tvName.getText().toString());
            intent.putExtra("recipient_device_id", deviceId);
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
