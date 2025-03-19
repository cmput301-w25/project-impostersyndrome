package com.example.impostersyndrom.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.model.EmojiUtils;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.model.MoodFilter;
import com.example.impostersyndrom.model.MoodItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private ListView moodListView;
    private ImageButton addMoodButton, logoutButton, filterButton, searchButton, heartButton;
    private TextView myMoodsButton, followingButton;

    // Data
    private MoodAdapter moodAdapter;
    private MoodDataManager moodDataManager;

    private String userId;
    private FirebaseFirestore db;
    private boolean isMyMoods = true; // To track which tab is active
    private boolean filterByRecentWeek = false;
    private String selectedEmotionalState = "";
    private MoodFilter moodFilter;
    private List<DocumentSnapshot> moodDocs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initializeViews();
        moodDataManager = new MoodDataManager();


        // Initialize utilities
        moodFilter = new MoodFilter();

        // Get userId from FirebaseAuth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }
        userId = auth.getCurrentUser().getUid();

        // Set up button click listeners
        setupButtonListeners();

        // Load initial data
        fetchMyMoods();
    }

    private void initializeViews() {
        myMoodsButton = findViewById(R.id.myMoodsButton);
        followingButton = findViewById(R.id.followingButton);
        moodListView = findViewById(R.id.moodListView);
        addMoodButton = findViewById(R.id.addMoodButton);
        logoutButton = findViewById(R.id.profileButton);
        filterButton = findViewById(R.id.filterButton);
        searchButton = findViewById(R.id.searchButton);
        heartButton = findViewById(R.id.heartButton);
    }

    private void setupButtonListeners() {
        followingButton.setOnClickListener(v -> {
            isMyMoods = false;
            fetchFollowingMoods();
        });

        myMoodsButton.setOnClickListener(v -> {
            isMyMoods = true;
            fetchMyMoods();
        });

        addMoodButton.setOnClickListener(v -> navigateToEmojiSelection());
        logoutButton.setOnClickListener(v -> logoutUser());
        searchButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));
        heartButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FollowingActivity.class)));
        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    private void fetchMyMoods() {
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshot -> {
                    moodDocs = snapshot.getDocuments();
                    setupMoodAdapter(moodDocs, false);
                })
                .addOnFailureListener(e -> showToast("Failed to fetch your moods: " + e.getMessage()));
    }

    private void fetchFollowingMoods() {
        db.collection("following")
                .whereEqualTo("followerId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> followingIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String followingId = doc.getString("followingId");
                        if (followingId != null) {
                            followingIds.add(followingId);
                        }
                    }

                    if (followingIds.isEmpty()) {
                        showToast("You're not following anyone!");
                        moodListView.setAdapter(null);
                        return;
                    }

                    fetchLatestMoodsFromFollowedUsers(followingIds);
                })
                .addOnFailureListener(e -> showToast("Failed to fetch following list: " + e.getMessage()));
    }

    private void setupMoodAdapter(List<DocumentSnapshot> moodDocs, boolean showUsername) {
        // Create a list with the exact size needed
        final List<MoodItem> moodItems = new ArrayList<>(Collections.nCopies(moodDocs.size(), null));
        final int[] completedQueries = {0};

        for (int i = 0; i < moodDocs.size(); i++) {
            final int position = i;  // Create a final copy for the callback
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
                        // Add the mood item at the SAME index as in the original list
                        moodItems.set(position, new MoodItem(moodDoc, showUsername ? "@" + username : ""));

                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocs.size()) {
                            // Remove any null entries that might exist if some queries failed
                            moodItems.removeIf(item -> item == null);

                            if (moodItems.isEmpty()) {
                                showToast("No valid moods to display");
                                moodListView.setAdapter(null);
                            } else {
                                moodAdapter = new MoodAdapter(MainActivity.this, moodItems, showUsername);
                                moodListView.setAdapter(moodAdapter);

                                // Handle clicks
                                moodListView.setOnItemClickListener((parent, view, pos, id) -> {
                                    // Find the corresponding document
                                    DocumentSnapshot selectedMood = moodDocs.get(pos);
                                    navigateToMoodDetail(selectedMood);
                                });

                                // Handle long press (only for My Moods)
                                moodListView.setOnItemLongClickListener((parent, view, pos, id) -> {
                                    if (isMyMoods) {
                                        DocumentSnapshot selectedMood = moodDocs.get(pos);
                                        showBottomSheetDialog(selectedMood);
                                    }
                                    return true;
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        showToast("Error fetching user details: " + e.getMessage());
                        e.printStackTrace();
                        completedQueries[0]++;
                    });
        }
    }


    private void fetchLatestMoodsFromFollowedUsers(List<String> followingIds) {
        List<DocumentSnapshot> allMoods = new ArrayList<>();
        final int[] completedQueries = {0};

        for (String followedUserId : followingIds) {
            db.collection("moods")
                    .whereEqualTo("userId", followedUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(3)  // ✅ Fetch max 3 moods per user
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        allMoods.addAll(snapshot.getDocuments());

                        completedQueries[0]++;
                        if (completedQueries[0] == followingIds.size()) {
                            // ✅ FINAL Global Sorting After Fetching ALL Users' Moods
                            allMoods.sort((m1, m2) -> {
                                Timestamp t1 = m1.getTimestamp("timestamp");
                                Timestamp t2 = m2.getTimestamp("timestamp");

                                if (t1 == null || t2 == null) return 0; // Prevent crashes

                                return Long.compare(t2.toDate().getTime(), t1.toDate().getTime()); // ✅ Sort latest first
                            });

                            // ✅ Debugging: Print out sorted timestamps to check order
                            for (DocumentSnapshot mood : allMoods) {
                                Timestamp timestamp = mood.getTimestamp("timestamp");
                                System.out.println("Mood Timestamp: " + timestamp);
                            }

                            // ✅ Send properly sorted list to UI
                            setupMoodAdapter(allMoods, true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedQueries[0]++;
                        if (completedQueries[0] == followingIds.size() && !allMoods.isEmpty()) {
                            allMoods.sort((m1, m2) -> {
                                Timestamp t1 = m1.getTimestamp("timestamp");
                                Timestamp t2 = m2.getTimestamp("timestamp");

                                if (t1 == null || t2 == null) return 0;

                                return Long.compare(t2.toDate().getTime(), t1.toDate().getTime()); // ✅ Sort latest first
                            });

                            setupMoodAdapter(allMoods, true);
                        }
                    });
        }
    }

    private void showFilterDialog() {
        Dialog filterDialog = new Dialog(this);
        filterDialog.setContentView(R.layout.filter_mood_dialog);

        Window window = filterDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.CENTER);
        }

        CheckBox checkboxRecentWeek = filterDialog.findViewById(R.id.checkboxRecentWeek);
        Spinner emotionalStateSpinner = filterDialog.findViewById(R.id.emotionalStateSpinner);
        ImageButton tickButton = filterDialog.findViewById(R.id.tickButton);

        List<String> emotionalStates = new ArrayList<>();
        emotionalStates.add("All Moods");
        emotionalStates.addAll(List.of(EmojiUtils.getEmojiDescriptions()));

        EmojiSpinnerAdapter spinnerAdapter = new EmojiSpinnerAdapter(this, emotionalStates, getEmojiDrawables());
        emotionalStateSpinner.setAdapter(spinnerAdapter);

        checkboxRecentWeek.setChecked(filterByRecentWeek);

        if (!selectedEmotionalState.isEmpty()) {
            String selectedDescription = EmojiUtils.getDescription(selectedEmotionalState);
            int selectedPosition = emotionalStates.indexOf(selectedDescription);
            if (selectedPosition != -1) {
                emotionalStateSpinner.setSelection(selectedPosition);
            }
        }

        tickButton.setOnClickListener(v -> {
            filterByRecentWeek = checkboxRecentWeek.isChecked();
            String selectedDescription = (String) emotionalStateSpinner.getSelectedItem();
            selectedEmotionalState = selectedDescription.equals("All Moods") ? "" : EmojiUtils.getEmojiKey(selectedDescription);
            applyFilter(selectedEmotionalState);
            filterDialog.dismiss();
        });

        filterDialog.show();
    }

    private List<Integer> getEmojiDrawables() {
        List<Integer> emojiDrawables = new ArrayList<>();
        emojiDrawables.add(R.drawable.emoji_happy);
        emojiDrawables.add(R.drawable.emoji_confused);
        emojiDrawables.add(R.drawable.emoji_disgust);
        emojiDrawables.add(R.drawable.emoji_angry);
        emojiDrawables.add(R.drawable.emoji_sad);
        emojiDrawables.add(R.drawable.emoji_fear);
        emojiDrawables.add(R.drawable.emoji_shame);
        emojiDrawables.add(R.drawable.emoji_surprised);
        return emojiDrawables;
    }

    private void showBottomSheetDialog(DocumentSnapshot moodDoc) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_mood_options, null);

        TextView editMood = bottomSheetView.findViewById(R.id.editMoodOption);
        TextView deleteMood = bottomSheetView.findViewById(R.id.deleteMoodOption);

        editMood.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditMoodActivity.class);
            intent.putExtra("moodId", moodDoc.getId());
            intent.putExtra("emoji", (String) moodDoc.get("emotionalState"));
            intent.putExtra("timestamp", moodDoc.getTimestamp("timestamp"));
            intent.putExtra("reason", (String) moodDoc.get("reason"));
            intent.putExtra("imageUrl", (String) moodDoc.get("imageUrl"));
            intent.putExtra("color", ((Long) moodDoc.get("color")).intValue());
            intent.putExtra("group", (String) moodDoc.get("group"));
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });

        deleteMood.setOnClickListener(v -> {
            moodDataManager.deleteMood(moodDoc.getId(), new MoodDataManager.OnMoodDeletedListener() {
                @Override
                public void onMoodDeleted() {
                    showToast("Mood deleted!");
                    onResume(); // Refresh the list after deletion
                }

                @Override
                public void onError(String errorMessage) {
                    showToast("Failed to delete mood: " + errorMessage);
                }
            });
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }
    private void navigateToEmojiSelection() {
        Intent intent = new Intent(MainActivity.this, EmojiSelectionActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    private void applyFilter(String emotionalState) {
        List<DocumentSnapshot> filteredMoods = moodFilter.applyFilter(moodDocs, filterByRecentWeek, emotionalState);
        setupMoodAdapter(filteredMoods, !isMyMoods);
    }

    private void navigateToMoodDetail(DocumentSnapshot moodDoc) {
        if (moodDoc == null || !moodDoc.exists()) {
            showToast("Mood details unavailable.");
            return;
        }

        Map<String, Object> data = moodDoc.getData();
        if (data != null) {
            Intent intent = new Intent(MainActivity.this, MoodDetailActivity.class);
            intent.putExtra("emoji", (String) data.getOrDefault("emotionalState", ""));
            intent.putExtra("timestamp", (Timestamp) data.getOrDefault("timestamp", null));
            intent.putExtra("reason", (String) data.getOrDefault("reason", "No reason provided"));
            intent.putExtra("group", (String) data.getOrDefault("group", "No group"));
            intent.putExtra("color", ((Long) data.getOrDefault("color", 0L)).intValue());
            intent.putExtra("imageUrl", (String) data.getOrDefault("imageUrl", ""));
            intent.putExtra("emojiDescription", (String) data.getOrDefault("emojiDescription", "No description"));

            // ✅ Pass the current tab state
            intent.putExtra("isMyMoods", isMyMoods);

            startActivity(intent);
        } else {
            showToast("Error loading mood details.");
        }
    }


    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        showToast("Logged out successfully!");
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showToast(String message) {
        if (!isFinishing()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMyMoods) fetchMyMoods();
    }
}