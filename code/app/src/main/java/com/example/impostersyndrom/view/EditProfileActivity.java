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
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_image_picker);

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
                    Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete image from Storage: ", e);
                        Toast.makeText(this, "Failed to remove profile picture", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // No image to remove, just set default locally
            profileImage.setImageResource(R.drawable.default_person);
            Toast.makeText(this, "No profile picture to remove", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfile() {
        String newUsername = usernameEditText.getText().toString().trim();
        String newBio = bioEditText.getText().toString().trim();

        if (newUsername.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(EditProfileActivity.this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile: ", e);
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }
}