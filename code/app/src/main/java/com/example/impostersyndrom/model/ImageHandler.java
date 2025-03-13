package com.example.impostersyndrom.model;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ImageHandler {
    private static final int MAX_IMAGE_SIZE = 65536; // Maximum allowed image size in bytes

    private final Activity activity; // Reference to the calling activity
    private final ImageView imagePreview; // ImageView to display the selected image
    private final FirebaseStorage storage; // Firebase Storage instance
    private final StorageReference storageRef; // Reference to the Firebase Storage root
    private Bitmap selectedImageBitmap = null; // Bitmap of the selected image

    // Listener interface for image loading events
    public interface OnImageLoadedListener {
        void onImageLoaded(); // Called when an image is successfully loaded
        void onImageCleared(); // Called when the image is cleared
    }

    private OnImageLoadedListener imageLoadedListener; // Listener for image loading events

    /**
     * Sets the listener for image loading events.
     *
     * @param listener The listener to be notified of image loading events.
     */
    public void setOnImageLoadedListener(OnImageLoadedListener listener) {
        this.imageLoadedListener = listener;
    }

    /**
     * Constructor for ImageHandler.
     *
     * @param activity     The calling activity.
     * @param imagePreview The ImageView to display the selected image.
     */
    public ImageHandler(Activity activity, ImageView imagePreview) {
        this.activity = activity;
        this.imagePreview = imagePreview;
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
    }

    /**
     * Opens the gallery to allow the user to select an image.
     *
     * @param galleryLauncher The ActivityResultLauncher to handle the gallery intent.
     */
    public void openGallery(ActivityResultLauncher<Intent> galleryLauncher) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    /**
     * Opens the camera to allow the user to capture an image.
     *
     * @param cameraLauncher The ActivityResultLauncher to handle the camera intent.
     */
    public void openCamera(ActivityResultLauncher<Intent> cameraLauncher) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }

    /**
     * Handles the result of the gallery or camera intent.
     *
     * @param resultCode The result code from the activity result.
     * @param data       The intent data containing the selected or captured image.
     */
    public void handleActivityResult(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = null;

            // Check if the result is from the gallery
            if (data.getData() != null) {
                imageUri = data.getData();
                Log.d("ImageHandler", "Gallery result: " + imageUri.toString());
            }
            // Check if the result is from the camera
            else if (data.getExtras() != null && data.getExtras().get("data") != null) {
                selectedImageBitmap = (Bitmap) data.getExtras().get("data");
                Log.d("ImageHandler", "Camera result: Bitmap received");
            }

            // Load the image if a valid URI or Bitmap is available
            if (imageUri != null) {
                try {
                    selectedImageBitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);
                    Log.d("ImageHandler", "Image loaded from gallery");
                } catch (IOException e) {
                    Log.e("ImageHandler", "Failed to load image: " + e.getMessage());
                    showToast("Failed to load image");
                    clearImage();
                    return;
                }
            }

            if (selectedImageBitmap != null) {
                imagePreview.setImageBitmap(selectedImageBitmap);
                // Notify listener that an image has been loaded
                if (imageLoadedListener != null) {
                    imageLoadedListener.onImageLoaded();
                }
            } else {
                Log.d("ImageHandler", "No image selected or loaded");
                clearImage();
            }
        } else {
            Log.d("ImageHandler", "Activity result not OK or data is null");
            clearImage();
        }
    }

    /**
     * Clears the selected image and updates the UI.
     */
    public void clearImage() {
        selectedImageBitmap = null;
        imagePreview.setImageBitmap(null);
        // Notify listener that the image has been cleared
        if (imageLoadedListener != null) {
            imageLoadedListener.onImageCleared();
        }
    }

    /**
     * Checks if an image is currently selected.
     *
     * @return True if an image is selected, false otherwise.
     */
    public boolean hasImage() {
        return selectedImageBitmap != null;
    }

    /**
     * Uploads the selected image to Firebase Storage.
     *
     * @param listener The listener to handle upload success or failure events.
     */
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

    /**
     * Displays a toast message.
     *
     * @param message The message to display.
     */
    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Listener interface for image upload events.
     */
    public interface OnImageUploadListener {
        void onImageUploadSuccess(String imageUrl); // Called when the image is successfully uploaded
        void onImageUploadFailure(Exception e); // Called when the image upload fails
    }
}