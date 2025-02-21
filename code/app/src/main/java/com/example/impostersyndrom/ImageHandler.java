package com.example.impostersyndrom;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

    public void handleActivityResult(int resultCode, Intent data, OnImageUploadListener listener) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Bitmap imageBitmap = null;

            if (data.getData() != null) {
                Uri imageUri = data.getData();
                try {
                    imageBitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
                } catch (IOException e) {
                    showToast("Failed to load image");
                    return;
                }
            } else if (data.getExtras() != null) { // Camera result
                imageBitmap = (Bitmap) data.getExtras().get("data");
            }

            if (imageBitmap != null) {
                if (compressAndStoreImage(imageBitmap, listener)) {
                    imagePreview.setImageBitmap(imageBitmap);
                }
            }
        }
    }

    private boolean compressAndStoreImage(Bitmap bitmap, OnImageUploadListener listener) {
        int quality = 100;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);

        while (stream.toByteArray().length > MAX_IMAGE_SIZE && quality > 10) {
            stream.reset();
            quality -= 5;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        }

        if (stream.toByteArray().length > MAX_IMAGE_SIZE) {
            showToast("Image is too large even after compression. Please choose a smaller image.");
            return false;
        } else {
            uploadImageToFirebase(stream.toByteArray(), listener);
            return true;
        }
    }

    private void uploadImageToFirebase(byte[] imageData, OnImageUploadListener listener) {
        String imageName = UUID.randomUUID().toString() + ".jpg";
        StorageReference imageRef = storageRef.child("images/" + imageName);

        UploadTask uploadTask = imageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String imageUrl = uri.toString();
                listener.onImageUploadSuccess(imageUrl);
                showToast("Image uploaded successfully!");
            });
        }).addOnFailureListener(e -> {
            showToast("Failed to upload image: " + e.getMessage());
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