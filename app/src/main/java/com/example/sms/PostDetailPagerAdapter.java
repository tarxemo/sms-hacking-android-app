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

public class PostDetailPagerAdapter extends RecyclerView.Adapter<PostDetailPagerAdapter.DetailViewHolder> {

    private List<Map<String, Object>> posts;

    public PostDetailPagerAdapter(List<Map<String, Object>> posts) {
        this.posts = posts;
    }

    @NonNull
    @Override
    public DetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post_detail_page, parent, false);
        return new DetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DetailViewHolder holder, int position) {
        Map<String, Object> post = posts.get(position);
        holder.tvCaption.setText((String) post.get("caption"));

        String imageUrl = (String) post.get("image_file");
        String postId = (String) post.get("id");
        java.io.File vaultFile = new java.io.File(holder.itemView.getContext().getFilesDir(), "vault/" + postId + ".jpg");

        if (vaultFile.exists()) {
            Glide.with(holder.itemView.getContext())
                    .load(vaultFile)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .into(holder.ivPostImage);
        } else if (imageUrl != null) {
            if (!imageUrl.startsWith("http")) {
                imageUrl = "https://messages.tarxemo.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .into(holder.ivPostImage);
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class DetailViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostImage;
        TextView tvCaption;

        public DetailViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostImage = itemView.findViewById(R.id.iv_pager_image);
            tvCaption = itemView.findViewById(R.id.tv_pager_caption);
        }
    }
}
