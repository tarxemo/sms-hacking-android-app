package com.example.sms;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private List<Map<String, Object>> messages = new ArrayList<>();
    private EditText etMessage;
    private ImageButton btnSend;
    private String recipientId;
    private String recipientDeviceId;

    private Handler pollHandler = new Handler();
    private Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages();
            pollHandler.postDelayed(this, 5000); // 5 seconds polling
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recipientId = getIntent().getStringExtra("recipient_id");
        recipientDeviceId = getIntent().getStringExtra("recipient_device_id");
        String recipientName = getIntent().getStringExtra("recipient_name");

        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(recipientName);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_view_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        TextView tvTypingIndicator = findViewById(R.id.tv_typing_indicator);

        Handler typingHandler = new Handler();
        Runnable typingRunnable = () -> tvTypingIndicator.setVisibility(View.GONE);

        etMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    tvTypingIndicator.setVisibility(View.VISIBLE);
                    typingHandler.removeCallbacks(typingRunnable);
                    typingHandler.postDelayed(typingRunnable, 3000);
                } else {
                    tvTypingIndicator.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        DeviceAuthManager authManager = new DeviceAuthManager(this);
        String currentDeviceId = authManager.getDeviceId();

        // Pass the actual deviceId to the adapter for correct alignment
        adapter = new MessageAdapter(messages, currentDeviceId); 
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendMessage());

        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void loadMessages() {
        if (recipientDeviceId == null && recipientId == null) return;
        String token = new DeviceAuthManager(this).getToken();
        RetrofitClient.getApiService().getChats("Bearer " + token, null, recipientDeviceId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> newMessages = response.body();
                    if (newMessages.size() != messages.size() || messages.isEmpty()) {
                        messages.clear();
                        messages.addAll(newMessages);
                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messages.size() - 1);
                        markMessagesAsRead(); // Mark incoming messages as read
                    } else {
                        // Check for status updates (e.g. read receipts changed) even if count is same
                         // Simple check: replace all and notify (inefficient but safe for status updates)
                         messages.clear();
                         messages.addAll(newMessages);
                         adapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                // Ignore silent failures for heartbeat
            }
        });
    }

    private void markMessagesAsRead() {
        if (recipientDeviceId == null) return;
        
        DeviceAuthManager authManager = new DeviceAuthManager(this);
        String token = authManager.getToken();
        Map<String, String> body = new HashMap<>();
        body.put("sender_device_id", recipientDeviceId);

        RetrofitClient.getApiService().markAsRead("Bearer " + token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // Read receipt sent successfully
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Fail silently
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        if (recipientDeviceId == null && recipientId == null) return;

        DeviceAuthManager authManager = new DeviceAuthManager(this);
        Map<String, Object> body = new HashMap<>();
        if (recipientId != null) body.put("receiver", recipientId);

        body.put("receiver_device_id", recipientDeviceId);
        body.put("message", text);
        body.put("device_id", authManager.getDeviceId());

        String token = authManager.getToken();
        RetrofitClient.getApiService().sendMessage("Bearer " + token, body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    etMessage.setText("");
                    loadMessages(); // Force immediate update
                } else {
                    Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
