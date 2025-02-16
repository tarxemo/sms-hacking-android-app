package com.example.sms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SMSAdapter extends RecyclerView.Adapter<SMSAdapter.SMSViewHolder> {

    private List<SMSData> smsDataList;

    public SMSAdapter(List<SMSData> smsDataList) {
        this.smsDataList = smsDataList;
    }

    @Override
    public SMSViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.sms_item, parent, false);
        return new SMSViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SMSViewHolder holder, int position) {
        SMSData smsData = smsDataList.get(position);
        holder.senderText.setText("From: " + smsData.getSender());
        holder.messageText.setText(smsData.getMessage());
        holder.timestampText.setText(smsData.getTimestamp());
    }

    @Override
    public int getItemCount() {
        return smsDataList.size();
    }

    public static class SMSViewHolder extends RecyclerView.ViewHolder {

        TextView senderText, messageText, timestampText;

        public SMSViewHolder(View itemView) {
            super(itemView);
            senderText = itemView.findViewById(R.id.senderText);
            messageText = itemView.findViewById(R.id.messageText);
            timestampText = itemView.findViewById(R.id.timestampText);
        }
    }
}
