package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.model.MoodItem;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserProfileActivity extends AppCompatActivity {

    private TextView usernameText, followersCountText, followingCountText, bioText, noMoodsText;
    private ImageButton backButton;
    private ImageView profileImage;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView moodListView;
    private MoodAdapter moodAdapter;
    private ProfileDataManager profileDataManager;
    private String userId;
    private String username;
    private FirebaseFirestore db;
    private List<DocumentSnapshot> moodDocs = new ArrayList<>();

    private static final String TAG = "UserProfileActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_profile_activity);

        db = FirebaseFirestore.getInstance();
        // Get the userId and username from the intent
        userId = getIntent().getStringExtra("userId");
        username = getIntent().getStringExtra("username");

        if (userId == null && username == null) {
            Toast.makeText(this, "Error: User information not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views and check for null
        if (!initializeViews()) {
            return; // Stop execution if views failed to initialize
        }

        profileDataManager = new ProfileDataManager();

        // If we have a userId, fetch directly; otherwise, find userId by username
        if (userId != null) {
            fetchUserData(userId);
            fetchRecentMoods(userId);
        } else {
            findUserIdByUsername(username);
        }

        backButton.setOnClickListener(v -> finish());
    }

    private boolean initializeViews() {
        usernameText = findViewById(R.id.usernameText);
        followersCountText = findViewById(R.id.followersCountText);
        followingCountText = findViewById(R.id.followingCountText);
        bioText = findViewById(R.id.bioText);
        noMoodsText = findViewById(R.id.noMoodsText);
        backButton = findViewById(R.id.backButton);
        profileImage = findViewById(R.id.profileImage);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        moodListView = findViewById(R.id.moodListView);

        // Null checks
        if (usernameText == null || followersCountText == null || followingCountText == null ||
                bioText == null || noMoodsText == null || backButton == null || profileImage == null ||
                swipeRefreshLayout == null || moodListView == null) {
            Toast.makeText(this, "Error: Unable to initialize views. Please check the layout file.", Toast.LENGTH_LONG).show();
            finish();
            return false;
        }

        profileImage.setImageResource(R.drawable.default_person);
        setupSwipeRefresh();
        return true;
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (userId != null) {
                fetchUserData(userId);
                fetchRecentMoods(userId);
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
                        fetchRecentMoods(userId);
                    } else {
                        showErrorMessage("User not found");
                        setDefaultProfileData();
                    }
                })
                .addOnFailureListener(e -> {
                    showErrorMessage("Error finding user: " + e.getMessage());
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

    private void fetchRecentMoods(String userId) {
        // Fetch up to 5 moods to ensure we get at least 3 non-private ones
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> moodDocs = queryDocumentSnapshots.getDocuments();
                    Log.d(TAG, "Fetched " + moodDocs.size() + " moods for userId: " + userId);

                    // Filter for public (non-private) moods
                    List<DocumentSnapshot> publicMoods = new ArrayList<>();
                    for (DocumentSnapshot doc : moodDocs) {
                        Boolean privateMood = doc.getBoolean("privateMood");
                        // Treat null or false as public
                        if (privateMood == null || !privateMood) {
                            publicMoods.add(doc);
                            if (publicMoods.size() >= 3) break; // Stop once we have 3 public moods
                        }
                    }

                    Log.d(TAG, "Filtered to " + publicMoods.size() + " public moods");
                    this.moodDocs = publicMoods;
                    setupMoodAdapter(publicMoods);
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching recent moods: " + e.getMessage(), e);
                    Toast.makeText(UserProfileActivity.this, "Failed to load recent moods: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    moodListView.setAdapter(null);
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void setupMoodAdapter(List<DocumentSnapshot> moodDocs) {
        List<MoodItem> moodItems = new ArrayList<>(Collections.nCopies(moodDocs.size(), null));
        final int[] completedQueries = {0};

        if (moodDocs.isEmpty()) {
            moodListView.setAdapter(null);
            if (noMoodsText != null) {
                noMoodsText.setVisibility(View.VISIBLE);
            }
            Log.d(TAG, "No moods to display, clearing adapter");
            return;
        }

        if (noMoodsText != null) {
            noMoodsText.setVisibility(View.GONE);
        }

        Log.d(TAG, "Setting up adapter with " + moodDocs.size() + " items");
        for (int i = 0; i < moodDocs.size(); i++) {
            final int position = i;
            DocumentSnapshot moodDoc = moodDocs.get(i);
            String moodUserId = moodDoc.getString("userId");
            if (moodUserId == null) {
                completedQueries[0]++;
                continue;
            }

            db.collection("users").document(moodUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        String username = userDoc.getString("username");
                        moodItems.set(position, new MoodItem(moodDoc, "@" + username));

                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocs.size()) {
                            moodItems.removeIf(item -> item == null);
                            if (moodItems.isEmpty()) {
                                moodListView.setAdapter(null);
                                if (noMoodsText != null) {
                                    noMoodsText.setVisibility(View.VISIBLE);
                                }
                                Log.d(TAG, "All items null, clearing adapter");
                            } else {
                                moodAdapter = new MoodAdapter(this, moodItems, true);
                                moodListView.setAdapter(moodAdapter);
                                Log.d(TAG, "Adapter set with " + moodItems.size() + " items");
                                moodListView.invalidate(); // Force redraw

                                moodListView.setOnItemClickListener((parent, view, pos, id) -> {
                                    DocumentSnapshot selectedMood = moodDocs.get(pos);
                                    navigateToMoodDetail(selectedMood);
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error fetching user details: " + e.getMessage());
                        Toast.makeText(UserProfileActivity.this, "Error fetching user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocs.size()) {
                            moodItems.removeIf(item -> item == null);
                            moodListView.setAdapter(null);
                            if (noMoodsText != null) {
                                noMoodsText.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }
    }

    private void navigateToMoodDetail(DocumentSnapshot moodDoc) {
        if (moodDoc == null || !moodDoc.exists()) {
            Toast.makeText(this, "Mood details unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, MoodDetailActivity.class);
        intent.putExtra("emoji", moodDoc.getString("emotionalState"));
        intent.putExtra("timestamp", moodDoc.getTimestamp("timestamp"));
        intent.putExtra("reason", moodDoc.getString("reason"));
        intent.putExtra("group", moodDoc.getString("group"));
        intent.putExtra("color", moodDoc.getLong("color") != null ? moodDoc.getLong("color").intValue() : 0);
        intent.putExtra("imageUrl", moodDoc.getString("imageUrl"));
        intent.putExtra("emojiDescription", moodDoc.getString("emojiDescription"));
        intent.putExtra("isMyMoods", false); // Since this is a user profile, not "My Moods"
        startActivity(intent);
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
        moodListView.setAdapter(null);
        if (noMoodsText != null) {
            noMoodsText.setVisibility(View.VISIBLE);
        }
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showErrorMessage(String message) {
        Log.e(TAG, message);
        Toast.makeText(UserProfileActivity.this, message, Toast.LENGTH_SHORT).show();
        swipeRefreshLayout.setRefreshing(false);
    }
}