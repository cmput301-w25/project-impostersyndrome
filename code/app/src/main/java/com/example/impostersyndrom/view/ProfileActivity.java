package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.EmojiUtils;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private TextView usernameText, followersCountText, followingCountText, bioText;
    private ImageButton backButton, homeButton, searchButton, addMoodButton, heartButton, profileButton, editButton;
    private ImageView profileImage;
    private ImageView mood1, mood2, mood3;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProfileDataManager profileDataManager;
    private MoodDataManager moodDataManager;
    private String userId; // The user whose profile is being viewed
    private FirebaseFirestore db;

    private static final String TAG = "ProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        profileDataManager = new ProfileDataManager();
        moodDataManager = new MoodDataManager();

        // Get the userId from the Intent (if viewing another user's profile)
        Intent intent = getIntent();
        userId = intent.getStringExtra("userId");
        if (userId == null) {
            // Fallback to the logged-in user if no userId is provided
            userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                    FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        }

        if (userId == null) {
            setDefaultProfileData();
            return;
        }

        fetchUserData();
        fetchRecentMoods();
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
        mood1 = findViewById(R.id.mood1);
        mood2 = findViewById(R.id.mood2);
        mood3 = findViewById(R.id.mood3);
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
        swipeRefreshLayout.setOnRefreshListener(() -> {
            fetchUserData();
            fetchRecentMoods();
        });
    }

    private void navigateTo(Class<?> activityClass) {
        Intent intent = new Intent(ProfileActivity.this, activityClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void fetchUserData() {
        swipeRefreshLayout.setRefreshing(true);

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

    private void fetchRecentMoods() {
        // Query the moods collection for the user's most recent non-private moods
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .whereEqualTo("privateMood", false) // Only non-private moods
                .orderBy("timestamp", Query.Direction.DESCENDING) // Most recent first
                .limit(3) // Limit to 3 moods
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> moodDocs = queryDocumentSnapshots.getDocuments();
                    displayRecentMoods(moodDocs);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching recent moods: " + e.getMessage());
                    Toast.makeText(ProfileActivity.this, "Failed to load recent moods", Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void displayRecentMoods(List<DocumentSnapshot> moodDocs) {
        // Reset visibility of mood ImageViews
        mood1.setVisibility(View.GONE);
        mood2.setVisibility(View.GONE);
        mood3.setVisibility(View.GONE);

        // Map to convert emoji keys to drawable resources
        List<ImageView> moodViews = new ArrayList<>();
        moodViews.add(mood1);
        moodViews.add(mood2);
        moodViews.add(mood3);

        for (int i = 0; i < moodDocs.size() && i < 3; i++) {
            DocumentSnapshot moodDoc = moodDocs.get(i);
            String emotionalState = moodDoc.getString("emotionalState");
            if (emotionalState != null) {
                int drawableId = EmojiUtils.getEmojiDrawableId(emotionalState);
                if (drawableId != 0) {
                    moodViews.get(i).setImageResource(drawableId);
                    moodViews.get(i).setVisibility(View.VISIBLE);
                }
            }
        }
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
        mood1.setVisibility(View.GONE);
        mood2.setVisibility(View.GONE);
        mood3.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showErrorMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show();
        swipeRefreshLayout.setRefreshing(false);
    }
}