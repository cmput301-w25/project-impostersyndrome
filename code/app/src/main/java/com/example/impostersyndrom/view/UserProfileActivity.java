package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.model.MoodItem;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.google.android.material.snackbar.Snackbar;
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
    private ScrollView scrollView;
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
            showMessage("Error: User information not provided");
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
        scrollView = findViewById(R.id.scrollView);

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
        setupListViewScrollListener();
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

    private void setupListViewScrollListener() {
        moodListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Not needed for this use case
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                // Check if the ListView is at the top
                boolean isAtTop = firstVisibleItem == 0 && (view.getChildCount() == 0 || view.getChildAt(0).getTop() >= 0);
                // Enable SwipeRefreshLayout only when the ListView is at the top
                swipeRefreshLayout.setEnabled(isAtTop);
                Log.d(TAG, "ListView scroll - isAtTop: " + isAtTop + ", SwipeRefreshLayout enabled: " + swipeRefreshLayout.isEnabled());
            }
        });

        // Also monitor the ScrollView to ensure SwipeRefreshLayout is enabled when scrolling the outer layout
        if (scrollView != null) {
            scrollView.setOnScrollChangeListener((View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) -> {
                // Enable SwipeRefreshLayout when the ScrollView is at the top
                boolean isScrollViewAtTop = scrollY == 0;
                swipeRefreshLayout.setEnabled(isScrollViewAtTop);
                Log.d(TAG, "ScrollView scrollY: " + scrollY + ", SwipeRefreshLayout enabled: " + swipeRefreshLayout.isEnabled());
            });
        }
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

    private void fetchRecentMoods(String userId) {
        // Fetch all moods for the user
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<DocumentSnapshot> allMoods = queryDocumentSnapshots.getDocuments();
                    Log.d(TAG, "Fetched " + allMoods.size() + " total moods for userId: " + userId);

                    // Filter for public (non-private) moods
                    List<DocumentSnapshot> publicMoods = new ArrayList<>();
                    for (DocumentSnapshot doc : allMoods) {
                        Boolean privateMood = doc.getBoolean("privateMood");
                        // Treat null or false as public
                        if (privateMood == null || !privateMood) {
                            publicMoods.add(doc);
                        }
                    }

                    // Log the number of public moods found
                    Log.d(TAG, "Filtered to " + publicMoods.size() + " public moods");

                    // If there are more than 3 public moods, take the top 3 (already sorted by timestamp)
                    if (publicMoods.size() > 3) {
                        publicMoods = publicMoods.subList(0, 3);
                        Log.d(TAG, "Limited to the 3 most recent public moods");
                    }

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