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
import com.google.firebase.firestore.QuerySnapshot;
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
    private String currentProfileImageUrl;
    private String currentUsername; // Track the current username

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
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_image_picker, null);
        bottomSheetDialog.setContentView(bottomSheetView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bottomSheetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            bottomSheetDialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

        bottomSheetDialog.findViewById(R.id.option_gallery)
                .setOnClickListener(v -> {
                    imageHandler.openGallery(galleryLauncher);
                    bottomSheetDialog.dismiss();
                });

        bottomSheetDialog.findViewById(R.id.option_remove)
                .setOnClickListener(v -> {
                    removeProfileImage();
                    bottomSheetDialog.dismiss();
                });

        bottomSheetDialog.show();
    }

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> imageHandler.handleActivityResult(result.getResultCode(), result.getData())
    );

    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username"); // Store current username
                        String bio = documentSnapshot.getString("bio");
                        currentProfileImageUrl = documentSnapshot.getString("profileImageUrl");

                        usernameEditText.setText(currentUsername);
                        bioEditText.setText(bio);

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
        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
            StorageReference imageRef = storage.getReferenceFromUrl(currentProfileImageUrl);
            imageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Old profile image deleted from Storage");
                        profileImage.setImageResource(R.drawable.default_person);
                        currentProfileImageUrl = null;
                        Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete image from Storage: ", e);
                        Toast.makeText(this, "Failed to remove profile picture", Toast.LENGTH_SHORT).show();
                    });
        } else {
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

        // If the username hasn't changed, proceed directly to saving
        if (newUsername.equals(currentUsername)) {
            proceedWithSave(newUsername, newBio);
        } else {
            // Check if the new username is already taken
            checkUsernameAvailability(newUsername, newBio);
        }
    }

    private void checkUsernameAvailability(String newUsername, String newBio) {
        db.collection("users")
                .whereEqualTo("username", newUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Username already exists
                        Toast.makeText(this, "Username '" + newUsername + "' is already taken", Toast.LENGTH_SHORT).show();
                    } else {
                        // Username is available, proceed with saving
                        proceedWithSave(newUsername, newBio);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking username availability: ", e);
                    Toast.makeText(this, "Error checking username availability", Toast.LENGTH_SHORT).show();
                });
    }

    private void proceedWithSave(String newUsername, String newBio) {
        if (imageHandler.hasImage()) {
            if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                StorageReference oldImageRef = storage.getReferenceFromUrl(currentProfileImageUrl);
                oldImageRef.delete()
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Old image deleted before new upload"))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to delete old image: ", e));
            }

            imageHandler.uploadImageToFirebase(new ImageHandler.OnImageUploadListener() {
                @Override
                public void onImageUploadSuccess(String imageUrl) {
                    updateProfile(newUsername, newBio, imageUrl);
                    currentProfileImageUrl = imageUrl;
                }

                @Override
                public void onImageUploadFailure(Exception e) {
                    Log.e(TAG, "Failed to upload image: ", e);
                    Toast.makeText(EditProfileActivity.this, "Failed to upload profile picture", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
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
            updates.put("profileImageUrl", null);
        }

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    currentUsername = newUsername; // Update current username after successful save
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile: ", e);
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                });
    }
}