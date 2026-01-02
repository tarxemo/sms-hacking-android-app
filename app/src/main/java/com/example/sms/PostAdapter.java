package com.example.sms;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Map<String, Object>> posts;
    private String currentDeviceId;

    public PostAdapter(List<Map<String, Object>> posts, String currentDeviceId) {
        this.posts = posts;
        this.currentDeviceId = currentDeviceId;
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
        
        // Handle User and Device Identification
        Map<String, Object> user = (Map<String, Object>) post.get("user");
        String deviceId = (String) post.get("device_id");
        String deviceName = (String) post.get("device_name");
        
        if (user != null) {
            String username = (String) user.get("username");
            if (currentDeviceId != null && currentDeviceId.equals(deviceId)) {
                username += " (Me)";
            } else if (deviceName != null && !deviceName.isEmpty()) {
                username = deviceName;
            } else if (deviceId != null) {
                // Determine if it's the current user on a different device
                // This logic might need refinement based on exact reqs, but works for now
            }
            holder.tvUsername.setText(username);
        }

        // Timestamp
        String createdAt = (String) post.get("created_at");
        holder.tvTimestamp.setText(getTimeAgo(createdAt));

        holder.tvCaption.setText((String) post.get("caption"));

        // Handle Likes
        Object likesCountObj = post.get("likes_count");
        int likesCount = likesCountObj instanceof Number ? ((Number) likesCountObj).intValue() : 0;
        holder.tvLikesCount.setText(likesCount + " likes");

        boolean isLiked = Boolean.TRUE.equals(post.get("is_liked"));
        holder.ivLike.setColorFilter(isLiked ? android.graphics.Color.RED : android.graphics.Color.BLACK);

        holder.ivLike.setOnClickListener(v -> {
            String token = new DeviceAuthManager(v.getContext()).getToken();
            String postId = (String) post.get("id");
            RetrofitClient.getApiService().toggleLikePost("Bearer " + token, postId).enqueue(new retrofit2.Callback<Map<String, Object>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> res = response.body();
                        post.put("is_liked", res.get("liked"));
                        post.put("likes_count", res.get("likes_count"));
                        notifyItemChanged(holder.getAdapterPosition());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                    // Fail silently or toast
                }
            });
        });

        // Load Image using Glide (Check Vault first)
        String imageUrl = (String) post.get("image_file");
        String postId = (String) post.get("id");
        java.io.File vaultFile = new java.io.File(holder.itemView.getContext().getFilesDir(), "vault/" + postId + ".jpg");

        if (vaultFile.exists()) {
            Glide.with(holder.itemView.getContext())
                    .load(vaultFile)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .into(holder.ivPostImage);
            holder.ivDownload.setColorFilter(android.graphics.Color.GREEN);
        } else if (imageUrl != null) {
            if (!imageUrl.startsWith("http")) {
                imageUrl = "https://messages.tarxemo.com" + (imageUrl.startsWith("/") ? "" : "/") + imageUrl;
            }
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .into(holder.ivPostImage);
            holder.ivDownload.setColorFilter(android.graphics.Color.BLACK);
        }

        final String finalImageUrl = imageUrl;
        holder.ivPostImage.setOnClickListener(v -> {
            PostDetailActivity.postList = posts;
            android.content.Intent intent = new android.content.Intent(v.getContext(), PostDetailActivity.class);
            intent.putExtra("position", holder.getAdapterPosition());
            v.getContext().startActivity(intent);
        });

        holder.ivDownload.setOnClickListener(v -> {
            if (finalImageUrl != null) {
                downloadImage(v.getContext(), finalImageUrl, (String) post.get("id"));
            }
        });

        holder.ivShare.setOnClickListener(v -> {
            if (finalImageUrl != null) {
                sharePost(v.getContext(), finalImageUrl, (String) post.get("caption"));
            }
        });
    }

    private String getTimeAgo(String timestamp) {
        if (timestamp == null) return "";
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", java.util.Locale.getDefault());
            sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            long time = sdf.parse(timestamp).getTime();
            long now = System.currentTimeMillis();
            long diff = now - time;

            if (diff < 60000) return "Just now";
            if (diff < 3600000) return (diff / 60000) + "m ago";
            if (diff < 86400000) return (diff / 3600000) + "h ago";
            return (diff / 86400000) + "d ago";
        } catch (Exception e) {
            // Fallback for simpler format if seconds fraction is missing
             try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault());
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
                long time = sdf.parse(timestamp).getTime();
                long now = System.currentTimeMillis();
                long diff = now - time;
                if (diff < 60000) return "Just now";
                if (diff < 3600000) return (diff / 60000) + "m ago";
                if (diff < 86400000) return (diff / 3600000) + "h ago";
                return (diff / 86400000) + "d ago";
             } catch (Exception ex) {
                 return "";
             }
        }
    }

    private void downloadImage(android.content.Context context, String url, String postId) {
        android.widget.Toast.makeText(context, "Saving to vault...", android.widget.Toast.LENGTH_SHORT).show();
        Glide.with(context)
                .asBitmap()
                .load(url)
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                        saveImageToVault(context, resource, postId);
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                });
    }

    private void saveImageToVault(android.content.Context context, android.graphics.Bitmap bitmap, String postId) {
        java.io.File vaultDir = new java.io.File(context.getFilesDir(), "vault");
        if (!vaultDir.exists()) vaultDir.mkdirs();

        java.io.File file = new java.io.File(vaultDir, postId + ".jpg");
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out);
            android.widget.Toast.makeText(context, "Saved to private vault", android.widget.Toast.LENGTH_SHORT).show();
            notifyDataSetChanged(); // Refresh to show 'offline' status if needed
        } catch (java.io.IOException e) {
            e.printStackTrace();
            android.widget.Toast.makeText(context, "Vault save failed", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void sharePost(android.content.Context context, String url, String caption) {
        android.widget.Toast.makeText(context, "Preparing image...", android.widget.Toast.LENGTH_SHORT).show();
        
        Glide.with(context)
                .asBitmap()
                .load(url)
                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull android.graphics.Bitmap resource, @Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                        shareImageBitmap(context, resource, caption);
                    }

                    @Override
                    public void onLoadCleared(@Nullable android.graphics.drawable.Drawable placeholder) {}
                });
    }

    private void shareImageBitmap(android.content.Context context, android.graphics.Bitmap bitmap, String caption) {
        try {
            // Save to cache directory for sharing
            java.io.File cachePath = new java.io.File(context.getCacheDir(), "shared_images");
            cachePath.mkdirs();
            java.io.File imageFile = new java.io.File(cachePath, "share_" + System.currentTimeMillis() + ".jpg");
            
            java.io.FileOutputStream stream = new java.io.FileOutputStream(imageFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();
            
            // Get content URI using FileProvider
            android.net.Uri contentUri = androidx.core.content.FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", imageFile);
            
            // Create share intent with image
            android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, contentUri);
            if (caption != null && !caption.isEmpty()) {
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, caption);
            }
            shareIntent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share image via"));
        } catch (Exception e) {
            e.printStackTrace();
            android.widget.Toast.makeText(context, "Failed to share image", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvCaption, tvLikesCount, tvTimestamp;
        ImageView ivPostImage, ivUserAvatar, ivLike, ivDownload, ivShare;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvCaption = itemView.findViewById(R.id.tv_caption);
            tvLikesCount = itemView.findViewById(R.id.tv_likes_count);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            ivPostImage = itemView.findViewById(R.id.iv_post_image);
            ivUserAvatar = itemView.findViewById(R.id.iv_user_avatar);
            ivLike = itemView.findViewById(R.id.iv_like);
            ivDownload = itemView.findViewById(R.id.iv_download);
            ivShare = itemView.findViewById(R.id.iv_share);
        }
    }
}
