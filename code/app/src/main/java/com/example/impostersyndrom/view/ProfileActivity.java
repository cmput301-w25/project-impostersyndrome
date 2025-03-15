package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.impostersyndrom.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    // UI Components
    private TextView usernameText;
    private TextView followersCountText;
    private TextView followingCountText;
    private TextView bioText;
    private ImageButton backButton;
    private ImageView profileImage;

    // Bottom Navigation Buttons
    private ImageButton homeButton;
    private ImageButton searchButton;
    private ImageButton addMoodButton;
    private ImageButton heartButton;
    private ImageButton profileButton;
    private ImageButton editButton;

    // SwipeRefreshLayout
    private SwipeRefreshLayout swipeRefreshLayout;

    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        // Initialize components
        initializeViews();

        // Fetch user data from Firestore
        fetchUserData();

        // Set button listeners
        backButton.setOnClickListener(v -> finish());

        // Set up bottom navigation button listeners
        setupBottomNavigation();

        // Set up SwipeRefreshLayout
        setupSwipeRefresh();
    }

    private void initializeViews() {
        // Initialize profile views
        usernameText = findViewById(R.id.usernameText);
        followersCountText = findViewById(R.id.followersCountText);
        followingCountText = findViewById(R.id.followingCountText);
        bioText = findViewById(R.id.bioText);
        backButton = findViewById(R.id.backButton);

        // Initialize profile image
        profileImage = findViewById(R.id.profileImage);
        profileImage.setImageResource(R.drawable.default_person); // Set default profile picture

        // Initialize bottom navigation buttons
        homeButton = findViewById(R.id.homeButton);
        searchButton = findViewById(R.id.searchButton);
        addMoodButton = findViewById(R.id.addMoodButton);
        heartButton = findViewById(R.id.heartButton);
        profileButton = findViewById(R.id.profileButton);
        editButton = findViewById(R.id.editButton);

        // Initialize SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupBottomNavigation() {
        // Highlight the profile button since we're in ProfileActivity
        profileButton.setImageResource(R.drawable.white_profile); // Use a different drawable for the active state

        // Set click listeners for each button
        homeButton.setOnClickListener(v -> navigateToHome());
        addMoodButton.setOnClickListener(v -> navigateToAddMood());
        profileButton.setOnClickListener(v -> navigateToProfile());
        editButton.setOnClickListener(v -> navigateToEditProfile());

        profileButton.setOnClickListener(null); // Remove any existing click listener
        profileButton.setClickable(false); // Make the button unclickable
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // Fetch user data again when the user pulls to refresh
            fetchUserData();
        });
    }

    private void navigateToHome() {
        Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT); // Bring existing activity to the front if it exists
        startActivity(intent);
    }

    private void navigateToEditProfile() {
        Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
        startActivity(intent);
    }

    private void navigateToAddMood() {
        Intent intent = new Intent(ProfileActivity.this, EmojiSelectionActivity.class);
        startActivity(intent);
    }

    private void navigateToProfile() {
        // Already in ProfileActivity, do nothing or refresh the activity
        Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void fetchUserData() {
        // Show the refresh indicator
        swipeRefreshLayout.setRefreshing(true);

        // Default data in case of error
        String defaultUsername = "username";

        // Get current user ID
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            // No user logged in
            usernameText.setText(defaultUsername);
            setDefaultProfileData();
            swipeRefreshLayout.setRefreshing(false); // Stop the refresh indicator
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
                    swipeRefreshLayout.setRefreshing(false); // Stop the refresh indicator
                })
                .addOnFailureListener(e -> {
                    // Handle error
                    Log.e(TAG, "Error fetching user data: ", e);
                    Toast.makeText(ProfileActivity.this,
                            "Failed to load profile data",
                            Toast.LENGTH_SHORT).show();
                    usernameText.setText(defaultUsername);
                    setDefaultProfileData();
                    swipeRefreshLayout.setRefreshing(false); // Stop the refresh indicator
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