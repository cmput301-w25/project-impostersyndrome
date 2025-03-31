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

/**
 * Handles image-related operations such as selection, capture, upload, and local storage.
 *
 * @author Rayan
 */
public class ImageHandler {
    private static final int MAX_IMAGE_SIZE = 65536; // Maximum allowed image size in bytes

    private final Activity activity;
    private final ImageView imagePreview;
    private final FirebaseStorage storage;
    private final StorageReference storageRef;
    private Bitmap selectedImageBitmap = null;

    /**
     * Listener interface for image loading events.
     */
    public interface OnImageLoadedListener {
        /**
         * Called when an image is successfully loaded.
         */
        void onImageLoaded();

        /**
         * Called when the image is cleared.
         */
        void onImageCleared();
    }

    /**
     * Listener interface for local image saving events.
     */
    public interface OnLocalImageSavedListener {
        /**
         * Called when an image is successfully saved locally.
         *
         * @param localUri The URI of the saved local image
         */
        void onLocalImageSaved(String localUri);
    }

    private OnImageLoadedListener imageLoadedListener;

    /**
     * Sets the listener for image loading events.
     *
     * @param listener The listener to be notified of image loading events
     */
    public void setOnImageLoadedListener(OnImageLoadedListener listener) {
        this.imageLoadedListener = listener;
    }

    /**
     * Constructs a new ImageHandler.
     *
     * @param activity The calling activity
     * @param imagePreview The ImageView to display the selected image
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
     * @param galleryLauncher The ActivityResultLauncher to handle the gallery intent
     */
    public void openGallery(ActivityResultLauncher<Intent> galleryLauncher) {
        // Implementation details omitted for brevity
    }

    /**
     * Opens the camera to allow the user to capture an image.
     *
     * @param cameraLauncher The ActivityResultLauncher to handle the camera intent
     */
    public void openCamera(ActivityResultLauncher<Intent> cameraLauncher) {
        // Implementation details omitted for brevity
    }

    /**
     * Processes the result of a gallery or camera intent.
     *
     * @param resultCode The result code from the activity result
     * @param data The intent data containing the selected or captured image
     */
    public void handleActivityResult(int resultCode, Intent data) {
        // Implementation details omitted for brevity
    }

    /**
     * Clears the currently selected image from the preview.
     */
    public void clearImage() {
        // Implementation details omitted for brevity
    }

    /**
     * Checks if an image is currently selected.
     *
     * @return True if an image is selected, false otherwise
     */
    public boolean hasImage() {
        return selectedImageBitmap != null;
    }

    /**
     * Uploads the selected image to Firebase Storage with compression.
     *
     * @param listener The listener to handle upload success or failure events
     */
    public void uploadImageToFirebase(OnImageUploadListener listener) {
        // Implementation details omitted for brevity
    }

    /**
     * Displays a Snackbar message to the user.
     *
     * @param message The message to display
     */
    private void showMessage(String message) {
        // Implementation details omitted for brevity
    }

    /**
     * Listener interface for image upload events.
     */
    public interface OnImageUploadListener {
        /**
         * Called when the image is successfully uploaded.
         *
         * @param imageUrl The URL of the uploaded image
         */
        void onImageUploadSuccess(String imageUrl);

        /**
         * Called when the image upload fails.
         *
         * @param e The exception that caused the failure
         */
        void onImageUploadFailure(Exception e);
    }

    /**
     * Uploads an image from a local URI to Firebase Storage.
     *
     * @param localUri The local URI of the image to upload
     * @param listener The listener to handle upload success or failure events
     */
    public void uploadImageFromLocalUri(String localUri, OnImageUploadListener listener) {
        // Implementation details omitted for brevity
    }

    /**
     * Retrieves the current bitmap displayed in the ImageView.
     *
     * @return The current bitmap, or null if none is set
     */
    public Bitmap getCurrentBitmap() {
        // Implementation details omitted for brevity
        return null;
    }

    /**
     * Saves the current image locally and returns its URI.
     *
     * @return The local URI of the saved image, or null if saving fails
     */
    public String saveImageLocally() {
        // Implementation details omitted for brevity
        return null;
    }

    /**
     * Saves an image from a remote URL to local storage synchronously.
     *
     * @param remoteUrl The remote URL of the image
     * @return The local URI of the saved image, or null if saving fails
     */
    public String saveImageLocallyFromRemote(String remoteUrl) {
        // Implementation details omitted for brevity
        return null;
    }

    /**
     * Saves an image from a remote URL to local storage asynchronously.
     *
     * @param remoteUrl The remote URL of the image
     * @param listener The listener to handle the save result
     */
    public void saveImageLocallyFromRemoteAsync(final String remoteUrl, final OnLocalImageSavedListener listener) {
        // Implementation details omitted for brevity
    }

    /**
     * Retrieves the local URI of the current image by saving it locally.
     *
     * @return The local URI of the saved image, or null if saving fails
     */
    public String getLocalImageUri() {
        return saveImageLocally();
    }
}
