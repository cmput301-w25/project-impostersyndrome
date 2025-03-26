package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserProfileActivity extends AppCompatActivity {

    private TextView usernameText, followersCountText, followingCountText, bioText;
    private ImageButton backButton;
    private ImageView profileImage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProfileDataManager profileDataManager;
    private String userId;
    private String username;

    private static final String TAG = "UserProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_activity);

        // Get the userId and username from the intent
        userId = getIntent().getStringExtra("userId");
        username = getIntent().getStringExtra("username");

        if (userId == null && username == null) {
            showMessage("Error: User information not provided");
            finish();
            return;
        }

        initializeViews();
        profileDataManager = new ProfileDataManager();

        // If we have a userId, fetch directly; otherwise, find userId from username
        if (userId != null) {
            fetchUserData(userId);
        } else {
            findUserIdByUsername(username);
        }

        backButton.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        usernameText = findViewById(R.id.usernameText);
        followersCountText = findViewById(R.id.followersCountText);
        followingCountText = findViewById(R.id.followingCountText);
        bioText = findViewById(R.id.bioText);
        backButton = findViewById(R.id.backButton);
        profileImage = findViewById(R.id.profileImage);
        profileImage.setImageResource(R.drawable.default_person);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        setupSwipeRefresh();
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (userId != null) {
                fetchUserData(userId);
            } else if (username != null) {
                findUserIdByUsername(username);
            }
        });
    }

    private void findUserIdByUsername(String username) {
        swipeRefreshLayout.setRefreshing(true);
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                        userId = document.getId();
                        fetchUserData(userId);
                    } else {
                        showMessage("User not found");
                        setDefaultProfileData();
                    }
                })
                .addOnFailureListener(e -> {
                    showMessage("Error finding user: " + e.getMessage());
                    setDefaultProfileData();
                });
    }

    private void fetchUserData(String userId) {
        swipeRefreshLayout.setRefreshing(true);

        // Fetch profile information
        profileDataManager.fetchUserProfile(userId, new ProfileDataManager.OnProfileFetchedListener() {
            @Override
            public void onProfileFetched(DocumentSnapshot profileDoc) {
                setProfileDataFromDocument(profileDoc);
            }

            @Override
            public void onError(String errorMessage) {
                showMessage(errorMessage);
            }
        });

        // Fetch followers count
        profileDataManager.fetchFollowersCount(userId, new ProfileDataManager.OnCountFetchedListener() {
            @Override
            public void onCountFetched(int count) {
                followersCountText.setText(String.valueOf(count));
            }

            @Override
            public void onError(String errorMessage) {
                followersCountText.setText("0");
                Log.e(TAG, "Followers count error: " + errorMessage);
            }
        });

        // Fetch following count
        profileDataManager.fetchFollowingCount(userId, new ProfileDataManager.OnCountFetchedListener() {
            @Override
            public void onCountFetched(int count) {
                followingCountText.setText(String.valueOf(count));
            }

            @Override
            public void onError(String errorMessage) {
                followingCountText.setText("0");
                Log.e(TAG, "Following count error: " + errorMessage);
            }
        });
    }

    private void setProfileDataFromDocument(DocumentSnapshot document) {
        usernameText.setText(document.getString("username") != null ? document.getString("username") : "username");
        bioText.setText(document.getString("bio") != null ? document.getString("bio") : "Exploring emotional awareness.");
        String profileImageUrl = document.getString("profileImageUrl");
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Glide.with(this).load(profileImageUrl).placeholder(R.drawable.default_person).error(R.drawable.default_person).into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.default_person);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void setDefaultProfileData() {
        usernameText.setText(username != null ? username : "username");
        bioText.setText("Exploring emotional awareness.");
        profileImage.setImageResource(R.drawable.default_person);
        followersCountText.setText("0");
        followingCountText.setText("0");
        swipeRefreshLayout.setRefreshing(false);
    }

    /**
     * Displays a Snackbar message and stops the refresh animation.
     *
     * @param message The message to display.
     */
    private void showMessage(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null && !isFinishing()) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}