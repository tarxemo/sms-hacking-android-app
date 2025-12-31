package com.example.sms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Map<String, Object>> posts;

    public PostAdapter(List<Map<String, Object>> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Map<String, Object> post = posts.get(position);
        
        // Handle User
        Map<String, Object> user = (Map<String, Object>) post.get("user");
        if (user != null) {
            holder.tvUsername.setText((String) user.get("username"));
        }

        holder.tvCaption.setText((String) post.get("caption"));

        // Load Image using Glide
        String imageUrl = (String) post.get("image_file");
        if (imageUrl != null) {
            // Adjust URL if it's relative
            if (!imageUrl.startsWith("http")) {
                imageUrl = "https://api.tarxemo.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .into(holder.ivPostImage);

            final String finalImageUrl = imageUrl;
            holder.ivPostImage.setOnClickListener(v -> {
                android.content.Intent intent = new android.content.Intent(v.getContext(), PostDetailActivity.class);
                intent.putExtra("image_url", finalImageUrl);
                intent.putExtra("caption", (String) post.get("caption"));
                if (user != null) {
                    intent.putExtra("username", (String) user.get("username"));
                }
                v.getContext().startActivity(intent);
            });
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvCaption;
        ImageView ivPostImage, ivUserAvatar;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvCaption = itemView.findViewById(R.id.tv_caption);
            ivPostImage = itemView.findViewById(R.id.iv_post_image);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
        }
    }
}
