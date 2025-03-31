package com.example.impostersyndrom.model;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

    public interface OnLocalImageSavedListener {
        void onLocalImageSaved(String localUri);
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
                    showMessage("Failed to load image");
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
     * Displays a Snackbar message.
     *
     * @param message The message to display.
     */
    private void showMessage(String message) {
        Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                .setAction("OK", null)
                .show();
    }

    /**
     * Listener interface for image upload events.
     */
    public interface OnImageUploadListener {
        void onImageUploadSuccess(String imageUrl); // Called when the image is successfully uploaded
        void onImageUploadFailure(Exception e); // Called when the image upload fails
    }

    public void uploadImageFromLocalUri(String localUri, OnImageUploadListener listener) {
        Uri fileUri = Uri.parse(localUri);
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference imageRef = storageRef.child("mood_images/" + System.currentTimeMillis() + ".png");

        imageRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            if (listener != null) {
                                listener.onImageUploadSuccess(uri.toString());
                            }
                        })
                )
                .addOnFailureListener(e -> {
                    if (listener != null) {
                        listener.onImageUploadFailure(e);
                    }
                });
    }

    public Bitmap getCurrentBitmap() {
        if (imagePreview != null && imagePreview.getDrawable() instanceof BitmapDrawable) {
            return ((BitmapDrawable) imagePreview.getDrawable()).getBitmap();
        }
        return null;
    }

    public String saveImageLocally() {
        Bitmap bitmap = getCurrentBitmap();
        if (bitmap == null) {
            Log.e("ImageHandler", "saveImageLocally: getCurrentBitmap() returned null");
            return null;
        }
        Log.d("ImageHandler", "saveImageLocally: Bitmap retrieved with width = " + bitmap.getWidth() + ", height = " + bitmap.getHeight());
        try {
            File file = new File(activity.getFilesDir(), "offline_image_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            String localUri = "file://" + file.getAbsolutePath();
            Log.d("ImageHandler", "saveImageLocally: Image saved locally at " + localUri);
            return localUri;
        } catch (IOException e) {
            Log.e("ImageHandler", "saveImageLocally: Error saving image locally: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public String saveImageLocallyFromRemote(String remoteUrl) {
        try {
            java.net.URL url = new java.net.URL(remoteUrl);
            Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            if (bitmap == null) {
                return null;
            }
            // Save the bitmap to the internal storage of the activity.
            File file = new File(activity.getFilesDir(), "offline_image_" + System.currentTimeMillis() + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
            return "file://" + file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public void saveImageLocallyFromRemoteAsync(final String remoteUrl, final OnLocalImageSavedListener listener) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(remoteUrl);
                Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                if (bitmap == null) {
                    activity.runOnUiThread(() -> listener.onLocalImageSaved(null));
                    return;
                }
                // Save the bitmap to internal storage.
                File file = new File(activity.getFilesDir(), "offline_image_" + System.currentTimeMillis() + ".png");
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
                String localUri = "file://" + file.getAbsolutePath();
                activity.runOnUiThread(() -> listener.onLocalImageSaved(localUri));
            } catch (IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> listener.onLocalImageSaved(null));
            }
        }).start();
    }

    public String getLocalImageUri() {
        return saveImageLocally();
    }
}