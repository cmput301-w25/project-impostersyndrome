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

/**
 * Activity for editing user profile information including username, bio and profile picture.
 */
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
    private String currentUsername;

    private static final String TAG = "EditProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase services
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Initialize views
        usernameEditText = findViewById(R.id.usernameEditText);
        bioEditText = findViewById(R.id.bioEditText);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);
        profileImage = findViewById(R.id.profileImage);

        imageHandler = new ImageHandler(this, profileImage);

        // Set click listeners
        backButton.setOnClickListener(v -> finish());
        profileImage.setOnClickListener(v -> showBottomSheetDialog());
        saveButton.setOnClickListener(v -> saveProfile());

        // Load existing user data
        loadUserData();
    }

    // Shows bottom sheet dialog for image selection options
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

    // Handles gallery selection result
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> imageHandler.handleActivityResult(result.getResultCode(), result.getData())
    );

    // Loads user data from Firestore
    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");
                        currentProfileImageUrl = documentSnapshot.getString("profileImageUrl");

                        usernameEditText.setText(currentUsername);
                        bioEditText.setText(bio);

                        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.img_default_person)
                                    .error(R.drawable.img_default_person)
                                    .into(profileImage);
                        } else {
                            profileImage.setImageResource(R.drawable.img_default_person);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data: ", e);
                    showMessage("Failed to load profile data");
                });
    }

    // Removes current profile image
    private void removeProfileImage() {
        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
            StorageReference imageRef = storage.getReferenceFromUrl(currentProfileImageUrl);
            imageRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Old profile image deleted from Storage");
                        profileImage.setImageResource(R.drawable.img_default_person);
                        currentProfileImageUrl = null;
                        showMessage("Profile picture removed");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete image from Storage: ", e);
                        showMessage("Failed to remove profile picture");
                    });
        } else {
            profileImage.setImageResource(R.drawable.img_default_person);
            showMessage("No profile picture to remove");
        }
    }

    // Validates and saves profile changes
    private void saveProfile() {
        String newUsername = usernameEditText.getText().toString().trim();
        String newBio = bioEditText.getText().toString().trim();

        if (newUsername.isEmpty()) {
            showMessage("Username cannot be empty");
            return;
        }

        if (newUsername.equals(currentUsername)) {
            proceedWithSave(newUsername, newBio);
        } else {
            checkUsernameAvailability(newUsername, newBio);
        }
    }

    // Checks if username is available before saving
    private void checkUsernameAvailability(String newUsername, String newBio) {
        db.collection("users")
                .whereEqualTo("username", newUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        showMessage("Username '" + newUsername + "' is already taken");
                    } else {
                        proceedWithSave(newUsername, newBio);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking username availability: ", e);
                    showMessage("Error checking username availability");
                });
    }

    // Handles the profile saving process
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
                    showMessage("Failed to upload profile picture");
                }
            });
        } else {
            updateProfile(newUsername, newBio, currentProfileImageUrl);
        }
    }

    // Updates profile data in Firestore
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
                    currentUsername = newUsername;
                    showMessage("Profile updated successfully");
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile: ", e);
                    showMessage("Failed to update profile");
                });
    }

    // Shows a snackbar message
    private void showMessage(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
    }
}