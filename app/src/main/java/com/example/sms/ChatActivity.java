package com.example.sms;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
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
        String recipientName = getIntent().getStringExtra("recipient_name");

        Toolbar toolbar = findViewById(R.id.toolbar_chat);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(recipientName);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recycler_view_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);

        // We use a dummy "1" for sender_id in adapter, but loadMessages handles the real data
        adapter = new MessageAdapter(messages, "1"); 
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
        if (recipientId == null) return;
        String token = new DeviceAuthManager(this).getToken();
        RetrofitClient.getApiService().getChats("Bearer " + token, recipientId).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> newMessages = response.body();
                    if (newMessages.size() != messages.size()) {
                        messages.clear();
                        messages.addAll(newMessages);
                        adapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messages.size() - 1);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                // Ignore silent failures for heartbeat
            }
        });
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty() || recipientId == null) return;

        Map<String, Object> body = new HashMap<>();
        body.put("receiver", recipientId);
        body.put("message", text);

        String token = new DeviceAuthManager(this).getToken();
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
