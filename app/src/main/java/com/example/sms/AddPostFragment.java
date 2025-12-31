package com.example.sms;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddPostFragment extends Fragment {

    private ImageView ivPostImage;
    private TextInputEditText etCaption;
    private MaterialButton btnShare;
    private CardView cardImageContainer;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    ivPostImage.setImageURI(selectedImageUri);
                    ivPostImage.setPadding(0, 0, 0, 0);
                    ivPostImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_post, container, false);

        ivPostImage = view.findViewById(R.id.ivPostImage);
        etCaption = view.findViewById(R.id.etCaption);
        btnShare = view.findViewById(R.id.btnShare);
        cardImageContainer = view.findViewById(R.id.cardImageContainer);

        cardImageContainer.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        btnShare.setOnClickListener(v -> uploadPost());

        return view;
    }

    private void uploadPost() {
        if (selectedImageUri == null) {
            Toast.makeText(getContext(), "Please select an image first", Toast.LENGTH_SHORT).show();
            return;
        }

        String caption = etCaption.getText().toString();
        File imageFile = getFileFromUri(selectedImageUri);
        if (imageFile == null) return;

        btnShare.setEnabled(false);
        btnShare.setText("Uploading...");

        String token = new DeviceAuthManager(getContext()).getToken();

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image_file", imageFile.getName(), requestFile);
        RequestBody captionPart = RequestBody.create(MediaType.parse("text/plain"), caption);

        RetrofitClient.getApiService().uploadPost("Bearer " + token, body, captionPart).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Post shared successfully!", Toast.LENGTH_SHORT).show();
                    // Clear inputs
                    ivPostImage.setImageResource(R.drawable.ic_add);
                    ivPostImage.setPadding(80, 80, 80, 80);
                    etCaption.setText("");
                    selectedImageUri = null;
                } else {
                    Toast.makeText(getContext(), "Upload failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
                btnShare.setEnabled(true);
                btnShare.setText("Share Post");
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                btnShare.setEnabled(true);
                btnShare.setText("Share Post");
            }
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            File tempFile = new File(getContext().getCacheDir(), "temp_post_image_" + System.currentTimeMillis() + ".jpg");
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
