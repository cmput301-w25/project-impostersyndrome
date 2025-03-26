package com.example.impostersyndrom.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.ImageHandler;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText bioEditText;
    private ImageButton saveButton;
    private ImageButton backButton;
    private ImageView profileImage;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private String userId;
    private ImageHandler imageHandler;
    private String currentProfileImageUrl; // Track the current profile image URL

    private static final String TAG = "EditProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firestore and Storage
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        usernameEditText = findViewById(R.id.usernameEditText);
        bioEditText = findViewById(R.id.bioEditText);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        profileImage = findViewById(R.id.profileImage);

        // Initialize ImageHandler
        imageHandler = new ImageHandler(this, profileImage);

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Set up change profile image button to show bottom sheet
        profileImage.setOnClickListener(v -> showBottomSheetDialog());

        // Set up save button
        saveButton.setOnClickListener(v -> saveProfile());

        // Load current user data
        loadUserData();
    }

    private void showBottomSheetDialog() {
        // Create the bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);

        // Inflate the bottom sheet layout
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image_picker, null);
        bottomSheetDialog.setContentView(bottomSheetView); // Note: This should be bottomSheetView, not bottomSheetView

        // Set edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bottomSheetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Remove grey background
            bottomSheetDialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }
        // Choose from Gallery option
        bottomSheetDialog.findViewById(R.id.option_gallery)
                .setOnClickListener(v -> {
                    imageHandler.openGallery(galleryLauncher);
                    bottomSheetDialog.dismiss();
                });

        // Remove Image option
        bottomSheetDialog.findViewById(R.id.option_remove)
                .setOnClickListener(v -> {
                    removeProfileImage();
                    bottomSheetDialog.dismiss();
                });

        // Show the bottom sheet
        bottomSheetDialog.show();
    }

    // ActivityResultLauncher for gallery
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> imageHandler.handleActivityResult(result.getResultCode(), result.getData())
    );

    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");
                        currentProfileImageUrl = documentSnapshot.getString("profileImageUrl");

                        usernameEditText.setText(username);
                        bioEditText.setText(bio);

                        // Load profile image using Glide, or set default if none
                        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.default_person)
                                    .error(R.drawable.default_person)
                                    .into(profileImage);
                        } else {
                            profileImage.setImageResource(R.drawable.default_person);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data: ", e);
                    showMessage("Failed to load profile data");
                });
    }

    private void removeProfileImage() {
        // If there’s an existing image URL, delete it from Firebase Storage
        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
            StorageReference imageRef = storage.getReferenceFromUrl(currentProfileImageUrl);
            imageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Old profile image deleted from Storage");
                        // Clear the local image and set default
                        profileImage.setImageResource(R.drawable.default_person);
                        currentProfileImageUrl = null; // Reset the URL
                        showMessage("Profile picture removed");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete image from Storage: ", e);
                        showMessage("Failed to remove profile picture");
                    });
        } else {
            // No image to remove, just set default locally
            profileImage.setImageResource(R.drawable.default_person);
            showMessage("No profile picture to remove");
        }
    }

    private void saveProfile() {
        String newUsername = usernameEditText.getText().toString().trim();
        String newBio = bioEditText.getText().toString().trim();

        if (newUsername.isEmpty()) {
            showMessage("Username cannot be empty");
            return;
        }

        // Check if a new image is selected
        if (imageHandler.hasImage()) {
            // If there’s an old image, delete it before uploading the new one
            if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                StorageReference oldImageRef = storage.getReferenceFromUrl(currentProfileImageUrl);
                oldImageRef.delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Old image deleted before new upload"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete old image: ", e));
            }

            // Upload the new image
            imageHandler.uploadImageToFirebase(new ImageHandler.OnImageUploadListener() {
                @Override
                public void onImageUploadSuccess(String imageUrl) {
                    // Update profile with the new image URL
                    updateProfile(newUsername, newBio, imageUrl);
                    currentProfileImageUrl = imageUrl; // Update the current URL
                }

                @Override
                public void onImageUploadFailure(Exception e) {
                    Log.e(TAG, "Failed to upload image: ", e);
                    showMessage("Failed to upload profile picture");
                }
            });
        } else {
            // No new image selected, update profile with null image URL if removed
            updateProfile(newUsername, newBio, currentProfileImageUrl);
        }
    }

    private void updateProfile(String newUsername, String newBio, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);
        updates.put("bio", newBio);
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        } else {
            updates.put("profileImageUrl", null); // Explicitly set to null if no image
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showMessage("Profile updated successfully");
                    finish(); // Close the activity
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile: ", e);
                    showMessage("Failed to update profile");
                });
    }

    private void showMessage(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
    }
}