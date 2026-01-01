package com.example.sms;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

public class PostDetailActivity extends AppCompatActivity {
    public static java.util.List<java.util.Map<String, Object>> postList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        int position = getIntent().getIntExtra("position", 0);

        Toolbar toolbar = findViewById(R.id.toolbar_post);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.view_pager_detail);
        if (postList != null) {
            viewPager.setAdapter(new PostDetailPagerAdapter(postList));
            viewPager.setCurrentItem(position, false);

            viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    java.util.Map<String, Object> post = postList.get(position);
                    java.util.Map<String, Object> user = (java.util.Map<String, Object>) post.get("user");
                    if (user != null) {
                        getSupportActionBar().setTitle((String) user.get("username"));
                    }
                }
            });

            // Set initial title
            java.util.Map<String, Object> currentPost = postList.get(position);
            java.util.Map<String, Object> user = (java.util.Map<String, Object>) currentPost.get("user");
            if (user != null) {
                getSupportActionBar().setTitle((String) user.get("username"));
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clear the static reference to prevent memory leaks if necessary, 
        // but often we want it to persist while the activity is finishing.
    }
}
