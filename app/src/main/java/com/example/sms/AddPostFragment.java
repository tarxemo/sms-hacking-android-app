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
    private MaterialButton btnShare, btnCamera, btnGallery;
    private CardView cardImageContainer;
    private Uri selectedImageUri;
    private Uri cameraImageUri; // To store the URI when taking a photo

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    openCamera();
                } else {
                    Toast.makeText(getContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success) {
                    selectedImageUri = cameraImageUri;
                    updateImagePreview(selectedImageUri);
                }
            });

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    updateImagePreview(selectedImageUri);
                }
            });

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (cameraImageUri != null) {
            outState.putParcelable("camera_image_uri", cameraImageUri);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            cameraImageUri = savedInstanceState.getParcelable("camera_image_uri");
        }

        View view = inflater.inflate(R.layout.fragment_add_post, container, false);

        ivPostImage = view.findViewById(R.id.ivPostImage);
        etCaption = view.findViewById(R.id.etCaption);
        btnShare = view.findViewById(R.id.btnShare);
        cardImageContainer = view.findViewById(R.id.cardImageContainer);
        btnCamera = view.findViewById(R.id.btnCamera);
        btnGallery = view.findViewById(R.id.btnGallery);

        btnCamera.setOnClickListener(v -> checkCameraPermissionAndOpen());
        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnShare.setOnClickListener(v -> uploadPost());

        return view;
    }

    private void checkCameraPermissionAndOpen() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void openCamera() {
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                cameraImageUri = androidx.core.content.FileProvider.getUriForFile(requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureLauncher.launch(cameraImageUri);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error creating image file", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws java.io.IOException {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void updateImagePreview(Uri uri) {
        ivPostImage.setImageTintList(null); // Clear the placeholder tint so the image is visible
        ivPostImage.setImageURI(uri);
        ivPostImage.setPadding(0, 0, 0, 0);
        ivPostImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
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

        DeviceAuthManager authManager = new DeviceAuthManager(getContext());
        String token = authManager.getToken();
        String deviceId = authManager.getDeviceId();

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image_file", imageFile.getName(), requestFile);
        RequestBody captionPart = RequestBody.create(MediaType.parse("text/plain"), caption);
        RequestBody deviceIdPart = RequestBody.create(MediaType.parse("text/plain"), deviceId);

        RetrofitClient.getApiService().uploadPost("Bearer " + token, body, captionPart, deviceIdPart).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Post shared successfully!", Toast.LENGTH_SHORT).show();
                    // Clear inputs
                    ivPostImage.setImageResource(R.drawable.ic_placeholder_image); // Use placeholder
                    ivPostImage.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B0B0B5"))); // Restore tint
                    ivPostImage.setPadding(80, 80, 80, 80);
                    ivPostImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
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
            // If it's already a file URI we control (from camera), we might be able to use it directly, 
            // but copying to cache is safer for consistency with content:// URIs
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            File tempFile = new File(getContext().getCacheDir(), "upload_" + System.currentTimeMillis() + ".jpg");
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
