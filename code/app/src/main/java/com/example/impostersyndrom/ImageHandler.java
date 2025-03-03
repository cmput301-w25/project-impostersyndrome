package com.example.impostersyndrom;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ImageHandler {
    private static final int MAX_IMAGE_SIZE = 65536;

    private final Activity activity;
    private final ImageView imagePreview;
    private final FirebaseStorage storage;
    private final StorageReference storageRef;
    private Bitmap selectedImageBitmap = null;

    // Add listener interface
    public interface OnImageLoadedListener {
        void onImageLoaded();
        void onImageCleared();
    }

    private OnImageLoadedListener imageLoadedListener;

    public void setOnImageLoadedListener(OnImageLoadedListener listener) {
        this.imageLoadedListener = listener;
    }

    public ImageHandler(Activity activity, ImageView imagePreview) {
        this.activity = activity;
        this.imagePreview = imagePreview;
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    public void openGallery(ActivityResultLauncher<Intent> galleryLauncher) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.READ_MEDIA_IMAGES}, 101);
        } else {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        }
    }

    public void openCamera(ActivityResultLauncher<Intent> cameraLauncher) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.CAMERA}, 100);
        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        }
    }

    public void handleActivityResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (data.getData() != null) { // Gallery
                Uri imageUri = data.getData();
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
                } catch (IOException e) {
                    showToast("Failed to load image");
                    clearImage();
                    return;
                }
            } else if (data.getExtras() != null) { // Camera
                selectedImageBitmap = (Bitmap) data.getExtras().get("data");
            }

            if (selectedImageBitmap != null) {
                imagePreview.setImageBitmap(selectedImageBitmap);
                // Trigger onImageLoaded
                if (imageLoadedListener != null) {
                    imageLoadedListener.onImageLoaded();
                }
            } else {
                clearImage();
            }
        } else {
            clearImage();
        }
    }

    public void clearImage() {
        selectedImageBitmap = null;
        imagePreview.setImageBitmap(null);
        // Trigger onImageCleared
        if (imageLoadedListener != null) {
            imageLoadedListener.onImageCleared();
        }
    }

    public boolean hasImage() {
        return selectedImageBitmap != null;
    }

    public void uploadImageToFirebase(OnImageUploadListener listener) {
        if (selectedImageBitmap == null) {
            listener.onImageUploadFailure(new Exception("No image selected"));
            return;
        }

        // Compress the image
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        int quality = 100;
        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);

        // Reduce quality until the image size is below MAX_IMAGE_SIZE
        while (stream.toByteArray().length > MAX_IMAGE_SIZE && quality > 10) {
            stream.reset();
            quality -= 5;
            selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        }

        // Check if the image is still too large
        if (stream.toByteArray().length > MAX_IMAGE_SIZE) {
            listener.onImageUploadFailure(new Exception("Image is too large even after compression"));
            return;
        }

        // Upload the compressed image to Firebase Storage
        String imageName = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child("images/" + imageName);

        UploadTask uploadTask = imageRef.putBytes(stream.toByteArray());
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                listener.onImageUploadSuccess(imageUrl);
            });
        }).addOnFailureListener(e -> {
            listener.onImageUploadFailure(e);
        });
    }

    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    public interface OnImageUploadListener {
        void onImageUploadSuccess(String imageUrl);
        void onImageUploadFailure(Exception e);
    }
}