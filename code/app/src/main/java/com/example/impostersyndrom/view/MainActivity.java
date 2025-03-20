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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.model.EmojiUtils;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.model.MoodFilter;
import com.example.impostersyndrom.model.MoodItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
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
    private ImageButton profileButton;
    private ImageButton menuButton;
    private DrawerLayout drawerLayout;
    private NavigationView innerNavigationView;
    private LinearLayout logoutContainer;
    private TextView userEmailTextView;
    private TextView myMoodsButton;
    private TextView followingButton;

    // Data
    private MoodAdapter moodAdapter;
    private MoodDataManager moodDataManager;

    private String userId;
    private FirebaseFirestore db;
    private boolean isMyMoods = true;
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

        // Set up navigation drawer with user information
        setupNavigationDrawer();

        // Load initial data
        fetchMyMoods();
    }

    private void initializeViews() {
        myMoodsButton = findViewById(R.id.myMoodsButton);
        followingButton = findViewById(R.id.followingButton);
        moodListView = findViewById(R.id.moodListView);
        ImageButton addMoodButton = findViewById(R.id.addMoodButton);
        profileButton = findViewById(R.id.profileButton);
        ImageButton filterButton = findViewById(R.id.filterButton);
        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton heartButton = findViewById(R.id.heartButton);
        menuButton = findViewById(R.id.menuButton);
        drawerLayout = findViewById(R.id.drawerLayout);
        innerNavigationView = findViewById(R.id.innerNavigationView);
        logoutContainer = findViewById(R.id.logoutContainer);
        userEmailTextView = findViewById(R.id.userEmailTextView);
    }

    private void setupNavigationDrawer() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if (email != null) {
                userEmailTextView.setText(email);
            }
        }

        innerNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        logoutContainer.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            logoutUser();
        });
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

        ImageButton addMoodButton = findViewById(R.id.addMoodButton);
        addMoodButton.setOnClickListener(v -> navigateToEmojiSelection());

        profileButton.setOnClickListener(v -> navigateToProfile());

        ImageButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterDialog());

        ImageButton searchButton = findViewById(R.id.searchButton);
        searchButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class); // Replace with your actual Search Activity class
            startActivity(intent);
        });

        ImageButton heartButton = findViewById(R.id.heartButton);
        heartButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FollowingActivity.class);
            startActivity(intent);
        });

        menuButton.setOnClickListener(v -> toggleNavigationDrawer());
    }

    private void navigateToProfile() {
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void toggleNavigationDrawer() {
        if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void navigateToEmojiSelection() {
        Intent intent = new Intent(MainActivity.this, EmojiSelectionActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
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
        final List<MoodItem> moodItems = new ArrayList<>(Collections.nCopies(moodDocs.size(), null));
        final int[] completedQueries = {0};

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
                        moodItems.set(position, new MoodItem(moodDoc, showUsername ? "@" + username : ""));

                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocs.size()) {
                            moodItems.removeIf(item -> item == null);

                            if (moodItems.isEmpty()) {
                                showToast("No valid moods to display");
                                moodListView.setAdapter(null);
                            } else {
                                moodAdapter = new MoodAdapter(MainActivity.this, moodItems, showUsername);
                                moodListView.setAdapter(moodAdapter);

                                moodListView.setOnItemClickListener((parent, view, pos, id) -> {
                                    DocumentSnapshot selectedMood = moodDocs.get(pos);
                                    navigateToMoodDetail(selectedMood);
                                });

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
                    .limit(3)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        allMoods.addAll(snapshot.getDocuments());

                        completedQueries[0]++;
                        if (completedQueries[0] == followingIds.size()) {
                            allMoods.sort((m1, m2) -> {
                                Timestamp t1 = m1.getTimestamp("timestamp");
                                Timestamp t2 = m2.getTimestamp("timestamp");
                                if (t1 == null || t2 == null) return 0;
                                return Long.compare(t2.toDate().getTime(), t1.toDate().getTime());
                            });

                            for (DocumentSnapshot mood : allMoods) {
                                Timestamp timestamp = mood.getTimestamp("timestamp");
                                System.out.println("Mood Timestamp: " + timestamp);
                            }

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
                                return Long.compare(t2.toDate().getTime(), t1.toDate().getTime());
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
            boolean isPrivateMood = moodDoc.contains("privateMood") && (Boolean) moodDoc.get("privateMood");
            intent.putExtra("privateMood", isPrivateMood);
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });

        deleteMood.setOnClickListener(v -> {
            moodDataManager.deleteMood(moodDoc.getId(), new MoodDataManager.OnMoodDeletedListener() {
                @Override
                public void onMoodDeleted() {
                    showToast("Mood deleted!");
                    onResume();
                }

                @Override
                public void onError(String errorMessage) {
                    showToast("Failed to delete mood: " + errorMessage);
                }
            });
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(bottomSheetView);  // ✅ Corrected to bottomSheetView
        bottomSheetDialog.show();
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