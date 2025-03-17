package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.ImageHandler;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText bioEditText;
    private ImageButton saveButton;
    private ImageButton backButton;
    private ImageView profileImage;
    private ImageButton changeProfileImageButton;

    private FirebaseFirestore db;
    private String userId;
    private ImageHandler imageHandler;

    private static final String TAG = "EditProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
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
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_image_picker);

        // Find the "Choose from Gallery" option in the bottom sheet
        bottomSheetDialog.findViewById(R.id.option_gallery)
                .setOnClickListener(v -> {
                    imageHandler.openGallery(galleryLauncher);
                    bottomSheetDialog.dismiss(); // Close the bottom sheet after selection
                });

        // Show the bottom sheet
        bottomSheetDialog.show();
    }

    // ActivityResultLauncher for gallery
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> imageHandler.handleActivityResult(result.getResultCode(), result.getData())
    );

    // ActivityResultLauncher for camera (not used now, but kept for future expansion)
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
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
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        usernameEditText.setText(username);
                        bioEditText.setText(bio);

                        // Load profile image using Glide
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this).load(profileImageUrl).into(profileImage);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data: ", e);
                    Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                });
    }

    private void saveProfile() {
        String newUsername = usernameEditText.getText().toString().trim();
        String newBio = bioEditText.getText().toString().trim();

        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if an image is selected
        if (imageHandler.hasImage()) {
            imageHandler.uploadImageToFirebase(new ImageHandler.OnImageUploadListener() {
                @Override
                public void onImageUploadSuccess(String imageUrl) {
                    // Update profile with the new image URL
                    updateProfile(newUsername, newBio, imageUrl);
                }

                @Override
                public void onImageUploadFailure(Exception e) {
                    Log.e(TAG, "Failed to upload image: ", e);
                    Toast.makeText(EditProfileActivity.this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // No image selected, update profile without changing the image
            updateProfile(newUsername, newBio, null);
        }
    }

    private void updateProfile(String newUsername, String newBio, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);
        updates.put("bio", newBio);
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile: ", e);
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }
}