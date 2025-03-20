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

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameText, followersCountText, followingCountText, bioText;
    private ImageButton backButton, homeButton, searchButton, addMoodButton, heartButton, profileButton, editButton;
    private ImageView profileImage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProfileDataManager profileDataManager;

    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        initializeViews();
        profileDataManager = new ProfileDataManager();
        fetchUserData();
        setupBottomNavigation();
        setupSwipeRefresh();
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
        homeButton = findViewById(R.id.homeButton);
        searchButton = findViewById(R.id.searchButton);
        addMoodButton = findViewById(R.id.addMoodButton);
        heartButton = findViewById(R.id.heartButton);
        profileButton = findViewById(R.id.profileButton);
        editButton = findViewById(R.id.editButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupBottomNavigation() {
        profileButton.setImageResource(R.drawable.white_profile);
        homeButton.setOnClickListener(v -> navigateTo(MainActivity.class));
        addMoodButton.setOnClickListener(v -> navigateTo(EmojiSelectionActivity.class));
        editButton.setOnClickListener(v -> navigateTo(EditProfileActivity.class));
        searchButton.setOnClickListener(v -> navigateTo(SearchActivity.class));
        heartButton.setOnClickListener(v -> navigateTo(FollowingActivity.class));
        profileButton.setClickable(false);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::fetchUserData);
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(ProfileActivity.this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void fetchUserData() {
        swipeRefreshLayout.setRefreshing(true);
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            setDefaultProfileData();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        // Fetch profile information
        profileDataManager.fetchUserProfile(userId, new ProfileDataManager.OnProfileFetchedListener() {
            @Override
            public void onProfileFetched(DocumentSnapshot profileDoc) {
                setProfileDataFromDocument(profileDoc);
            }

            @Override
            public void onError(String errorMessage) {
                showErrorMessage(errorMessage);
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
        usernameText.setText("username");
        bioText.setText("Exploring emotional awareness.");
        profileImage.setImageResource(R.drawable.default_person);
        followersCountText.setText("0");
        followingCountText.setText("0");
    }

    private void showErrorMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
        swipeRefreshLayout.setRefreshing(false);
    }
}