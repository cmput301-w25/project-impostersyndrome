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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.EmojiUtils;
import com.example.impostersyndrom.model.MoodDataCache;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.model.MoodFilter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private ListView moodListView;
    private ImageButton addMoodButton;
    private ImageButton profileButton;
    private ImageButton filterButton;
    private ImageButton menuButton;
    private DrawerLayout drawerLayout;
    private NavigationView innerNavigationView;
    private LinearLayout logoutContainer;
    private TextView userEmailTextView;

    // Data
    private List<DocumentSnapshot> moodDocs = new ArrayList<>();
    private MoodAdapter moodAdapter;
    private String userId;
    private boolean filterByRecentWeek = false;
    private String selectedEmotionalState = "";

    // Repositories and Utilities
    private MoodDataManager moodDataManager;
    private MoodFilter moodFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        initializeViews();

        // Initialize repositories and utilities
        moodDataManager = new MoodDataManager();
        moodFilter = new MoodFilter();

        // Get userId from intent or FirebaseAuth
        userId = getIntent().getStringExtra("userId");
        if (userId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (userId == null) {
            showToast("User ID is missing!");
            redirectToLogin();
            return;
        }

        // Check if data was preloaded
        boolean dataPreloaded = getIntent().getBooleanExtra("dataPreloaded", false);

        if (dataPreloaded && MoodDataCache.getInstance().getMoodDocs() != null) {
            // Use pre-fetched data
            setupMoodAdapter(MoodDataCache.getInstance().getMoodDocs());
            MoodDataCache.getInstance().clearCache();
        } else {
            // Fetch data if not pre-loaded
            fetchMoods(userId);
        }

        // Set up button click listeners
        setupButtonListeners();

        // Set up navigation drawer with user information
        setupNavigationDrawer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh moods when returning to this activity
        if (userId != null) {
            fetchMoods(userId);
        }
    }

    /**
     * Initializes all UI components.
     */
    private void initializeViews() {
        moodListView = findViewById(R.id.moodListView);
        addMoodButton = findViewById(R.id.addMoodButton);
        profileButton = findViewById(R.id.profileButton);
        filterButton = findViewById(R.id.filterButton);
        menuButton = findViewById(R.id.menuButton);
        drawerLayout = findViewById(R.id.drawerLayout);

        // New navigation components
        innerNavigationView = findViewById(R.id.innerNavigationView);
        logoutContainer = findViewById(R.id.logoutContainer);
        userEmailTextView = findViewById(R.id.userEmailTextView);
    }

    /**
     * Sets up the navigation drawer with user information.
     */
    private void setupNavigationDrawer() {
        // Set up user email in the drawer header
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            if (email != null) {
                userEmailTextView.setText(email);
            }
        }

        // Set up navigation item click listener for the inner navigation view
        innerNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            // Handle navigation item clicks
            // Add your navigation handling code here

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // Setup the logout button
        logoutContainer.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            logoutUser();
        });
    }

    /**
     * Sets up button click listeners.
     */
    private void setupButtonListeners() {
        addMoodButton.setOnClickListener(v -> navigateToEmojiSelection());
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });
        filterButton.setOnClickListener(v -> showFilterDialog());
        menuButton.setOnClickListener(v -> {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    /**
     * Navigates to EmojiSelectionActivity.
     */
    private void navigateToEmojiSelection() {
        Intent intent = new Intent(MainActivity.this, EmojiSelectionActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
    }

    /**
     * Logs out the user and redirects to LoginActivity.
     */
    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        showToast("Logged out successfully!");
        redirectToLogin();
    }

    /**
     * Redirects to LoginActivity.
     */
    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Sets up the MoodAdapter with the provided list of mood documents.
     *
     * @param moodDocs The list of mood documents to display.
     */
    private void setupMoodAdapter(List<DocumentSnapshot> moodDocs) {
        if (moodDocs != null && !moodDocs.isEmpty()) {
            moodAdapter = new MoodAdapter(this, moodDocs);
            moodListView.setAdapter(moodAdapter);

            moodListView.setOnItemClickListener((parent, view, position, id) -> {
                DocumentSnapshot moodDoc = moodDocs.get(position);
                navigateToMoodDetail(moodDoc);
            });

            moodListView.setOnItemLongClickListener((parent, view, position, id) -> {
                DocumentSnapshot moodDoc = moodDocs.get(position);
                showBottomSheetDialog(moodDoc);
                return true;
            });
        } else {
            showToast("No moods found!");
        }
    }

    /**
     * Navigates to MoodDetailActivity with the selected mood data.
     *
     * @param moodDoc The Firestore document representing the mood.
     */
    private void navigateToMoodDetail(DocumentSnapshot moodDoc) {
        Map<String, Object> data = moodDoc.getData();
        if (data != null) {
            Intent intent = new Intent(MainActivity.this, MoodDetailActivity.class);
            intent.putExtra("emoji", (String) data.get("emotionalState"));
            intent.putExtra("timestamp", (Timestamp) data.get("timestamp"));
            intent.putExtra("reason", (String) data.get("reason"));
            intent.putExtra("group", (String) data.get("group"));
            intent.putExtra("color", ((Long) data.get("color")).intValue());
            intent.putExtra("imageUrl", (String) data.get("imageUrl"));
            intent.putExtra("emojiDescription", (String) data.get("emojiDescription"));
            startActivity(intent);
        }
    }

    /**
     * Fetches mood entries from Firestore for the current user.
     *
     * @param userId The ID of the current user.
     */
    private void fetchMoods(String userId) {
        moodDataManager.fetchMoods(userId, new MoodDataManager.OnMoodsFetchedListener() {
            @Override
            public void onMoodsFetched(List<DocumentSnapshot> moodDocs) {
                MainActivity.this.moodDocs = moodDocs;
                applyFilter(selectedEmotionalState);
            }

            @Override
            public void onError(String errorMessage) {
                showToast("Failed to fetch moods: " + errorMessage);
            }
        });
    }

    /**
     * Shows the filter dialog with options to filter moods.
     */
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

    /**
     * Applies the filter to the mood list based on the current filter settings.
     *
     * @param emotionalState The emotional state to filter by (empty string for no filter).
     */
    private void applyFilter(String emotionalState) {
        List<DocumentSnapshot> filteredMoods = moodFilter.applyFilter(moodDocs, filterByRecentWeek, emotionalState);
        setupMoodAdapter(filteredMoods);
    }

    /**
     * Displays a bottom sheet dialog with options to edit or delete a mood.
     *
     * @param moodDoc The Firestore document representing the mood.
     */
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
                    fetchMoods(userId); // Refresh the list after deletion
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

    /**
     * Displays a toast message.
     *
     * @param message The message to display.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns a list of emoji drawable resource IDs.
     *
     * @return A list of emoji drawable resource IDs.
     */
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
}