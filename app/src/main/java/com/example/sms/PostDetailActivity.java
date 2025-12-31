package com.example.sms;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;

public class PostDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        String imageUrl = getIntent().getStringExtra("image_url");
        String caption = getIntent().getStringExtra("caption");
        String username = getIntent().getStringExtra("username");

        Toolbar toolbar = findViewById(R.id.toolbar_post);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(username);
        toolbar.setNavigationOnClickListener(v -> finish());

        ImageView imageView = findViewById(R.id.iv_full_post_image);
        TextView tvCaption = findViewById(R.id.tv_full_caption);

        tvCaption.setText(caption);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder_image)
                .into(imageView);
    }
}
