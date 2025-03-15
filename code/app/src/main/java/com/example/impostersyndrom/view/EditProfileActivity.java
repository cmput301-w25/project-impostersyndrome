package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.impostersyndrom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText bioEditText;
    private ImageButton saveButton;

    private FirebaseFirestore db;
    private String userId;

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

        // Load current user data
        loadUserData();

        // Set up save button listener
        saveButton.setOnClickListener(v -> saveProfile());
    }

    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");

                        usernameEditText.setText(username);
                        bioEditText.setText(bio);
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

        // Fetch the current username from Firestore
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String currentUsername = documentSnapshot.getString("username");

                        // Check if the username has changed
                        if (newUsername.equals(currentUsername)) {
                            // Username hasn't changed, just update the bio
                            updateProfile(newUsername, newBio);
                        } else {
                            // Username has changed, check if it's already taken
                            checkUsernameAvailability(newUsername, newBio);
                        }
                    } else {
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user data: ", e);
                    Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUsernameAvailability(String newUsername, String newBio) {
        db.collection("users")
                .whereEqualTo("username", newUsername)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Username already exists
                        Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                    } else {
                        // Username is available, update the profile
                        updateProfile(newUsername, newBio);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking username: ", e);
                    Toast.makeText(this, "Failed to check username", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateProfile(String newUsername, String newBio) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newUsername);
        updates.put("bio", newBio);

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