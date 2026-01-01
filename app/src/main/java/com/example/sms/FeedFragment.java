package com.example.sms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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

public class FeedFragment extends Fragment {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private PostAdapter adapter;
    private List<Map<String, Object>> posts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        DeviceAuthManager authManager = new DeviceAuthManager(getContext());
        adapter = new PostAdapter(posts, authManager.getDeviceId());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadPosts);

        loadPosts();
        return view;
    }

    private void loadPosts() {
        if (!isNetworkAvailable()) {
            loadPostsFromCache();
            swipeRefresh.setRefreshing(false);
            Toast.makeText(getContext(), "Offline: Loading from cache", Toast.LENGTH_SHORT).show();
            return;
        }

        swipeRefresh.setRefreshing(true);
        String token = new DeviceAuthManager(getContext()).getToken();
        RetrofitClient.getApiService().getPosts("Bearer " + token).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    posts.clear();
                    posts.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    savePostsToCache(response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                loadPostsFromCache();
                Toast.makeText(getContext(), "Network error: Loading from cache", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePostsToCache(List<Map<String, Object>> posts) {
        new Thread(() -> {
            try {
                org.json.JSONArray array = new org.json.JSONArray();
                for (Map<String, Object> post : posts) {
                    org.json.JSONObject obj = new org.json.JSONObject(post);
                    array.put(obj);
                }
                java.io.File cacheFile = new java.io.File(getContext().getFilesDir(), "posts_cache.json");
                java.io.FileWriter writer = new java.io.FileWriter(cacheFile);
                writer.write(array.toString());
                writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadPostsFromCache() {
        try {
            java.io.File cacheFile = new java.io.File(getContext().getFilesDir(), "posts_cache.json");
            if (!cacheFile.exists()) return;

            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(cacheFile));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            org.json.JSONArray array = new org.json.JSONArray(sb.toString());
            posts.clear();
            for (int i = 0; i < array.length(); i++) {
                posts.add(jsonToMap(array.getJSONObject(i)));
            }
            adapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> jsonToMap(org.json.JSONObject json) throws org.json.JSONException {
        Map<String, Object> map = new java.util.HashMap<>();
        java.util.Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object value = json.get(key);
            if (value instanceof org.json.JSONArray) {
                value = jsonToList((org.json.JSONArray) value);
            } else if (value instanceof org.json.JSONObject) {
                value = jsonToMap((org.json.JSONObject) value);
            } else if (value == org.json.JSONObject.NULL) {
                value = null;
            }
            map.put(key, value);
        }
        return map;
    }

    private List<Object> jsonToList(org.json.JSONArray array) throws org.json.JSONException {
        List<Object> list = new java.util.ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if (value instanceof org.json.JSONArray) {
                value = jsonToList((org.json.JSONArray) value);
            } else if (value instanceof org.json.JSONObject) {
                value = jsonToMap((org.json.JSONObject) value);
            } else if (value == org.json.JSONObject.NULL) {
                value = null;
            }
            list.add(value);
        }
        return list;
    }

    private boolean isNetworkAvailable() {
        if (getContext() == null) return false;
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
