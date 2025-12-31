package com.example.sms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatsFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private ChatAdapter adapter;
    private List<Map<String, Object>> chats = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chats, container, false);
        recyclerView = view.findViewById(R.id.recycler_view_chats);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_chats);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new ChatAdapter(chats);
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadChats);

        loadChats();
        return view;
    }

    private void loadChats() {
        swipeRefresh.setRefreshing(true);
        String token = new DeviceAuthManager(getContext()).getToken();
        // Passing null for recipientId fetches all recent chats for the account
        RetrofitClient.getApiService().getChats("Bearer " + token, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    chats.clear();
                    chats.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }
}
