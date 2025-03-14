package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.impostersyndrom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameText;
    private TextView followersCountText;
    private TextView followingCountText;
    private TextView bioText;
    private ImageButton backButton;
    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_activity);

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize components
        initializeViews();

        // Fetch user data from Firestore
        fetchUserData();

        // Set button listeners
        backButton.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        usernameText = findViewById(R.id.usernameText);
        followersCountText = findViewById(R.id.followersCountText);
        followingCountText = findViewById(R.id.followingCountText);
        bioText = findViewById(R.id.bioText);
        backButton = findViewById(R.id.backButton);
    }

    private void fetchUserData() {
        // Default data in case of error
        String defaultUsername = "username";

        // Get current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            // No user logged in
            usernameText.setText(defaultUsername);
            setDefaultProfileData();
            return;
        }

        // Reference to Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get user document from the users collection
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Extract username from document
                        String username = documentSnapshot.getString("username");

                        // Set the username in the UI
                        if (username != null && !username.isEmpty()) {
                            usernameText.setText(username);
                        } else {
                            usernameText.setText(defaultUsername);
                        }

                        // Set other profile data
                        setProfileDataFromDocument(documentSnapshot);
                    } else {
                        // Document doesn't exist
                        usernameText.setText(defaultUsername);
                        setDefaultProfileData();
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    Log.e(TAG, "Error fetching user data: ", e);
                    Toast.makeText(ProfileActivity.this,
                            "Failed to load profile data",
                            Toast.LENGTH_SHORT).show();
                    usernameText.setText(defaultUsername);
                    setDefaultProfileData();
                });
    }

    private void setProfileDataFromDocument(DocumentSnapshot document) {
        // Set other profile data - using document fields if available, otherwise defaults
        followersCountText.setText("127");  // Hardcoded for now - you could add these fields to your database
        followingCountText.setText("256");  // Hardcoded for now

        // Get bio from document if it exists, otherwise use default
        String bio = document.getString("bio");
        if (bio != null && !bio.isEmpty()) {
            bioText.setText(bio);
        } else {
            bioText.setText("Exploring emotional awareness through daily reflections. Sharing my mood journey and connecting with like-minded individuals.");
        }
    }

    private void setDefaultProfileData() {
        followersCountText.setText("127");
        followingCountText.setText("256");
        bioText.setText("Exploring emotional awareness through daily reflections. Sharing my mood journey and connecting with like-minded individuals.");
    }
}