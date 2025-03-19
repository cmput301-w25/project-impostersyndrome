package com.example.impostersyndrom.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MainViewPagerAdapter;
import com.example.impostersyndrom.model.EmojiUtils;
import com.example.impostersyndrom.model.MoodDataManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageButton addMoodButton, logoutButton, filterButton, searchButton, heartButton;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MainViewPagerAdapter viewPagerAdapter;
    private MoodDataManager moodDataManager;
    private String userId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "onCreate called");

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        initializeViews();
        moodDataManager = new MoodDataManager();

        // Get userId from FirebaseAuth
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }
        userId = auth.getCurrentUser().getUid();
        Log.d("MainActivity", "userId: " + userId);

        // Pass userId to fragments via Intent
        getIntent().putExtra("userId", userId);

        // Set up ViewPager and TabLayout
        setupViewPager();

        // Set up button click listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        addMoodButton = findViewById(R.id.addMoodButton);
        logoutButton = findViewById(R.id.profileButton);
        filterButton = findViewById(R.id.filterButton);
        searchButton = findViewById(R.id.searchButton);
        heartButton = findViewById(R.id.heartButton);
    }

    private void setupViewPager() {
        viewPagerAdapter = new MainViewPagerAdapter(this);
        viewPager.setAdapter(viewPagerAdapter);
        Log.d("MainActivity", "ViewPager adapter set");

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "My Moods" : "Following");
        }).attach();
    }

    private void setupButtonListeners() {
        addMoodButton.setOnClickListener(v -> navigateToEmojiSelection());
        logoutButton.setOnClickListener(v -> logoutUser());
        searchButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, SearchActivity.class)));
        heartButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, FollowingActivity.class)));
        filterButton.setOnClickListener(v -> showFilterDialog());
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

        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (currentFragment instanceof MyMoodsFragment) {
            checkboxRecentWeek.setChecked(((MyMoodsFragment) currentFragment).isFilterByRecentWeek());
            String selectedEmotionalState = ((MyMoodsFragment) currentFragment).getSelectedEmotionalState();
            if (!selectedEmotionalState.isEmpty()) {
                String selectedDescription = EmojiUtils.getDescription(selectedEmotionalState);
                int selectedPosition = emotionalStates.indexOf(selectedDescription);
                if (selectedPosition != -1) {
                    emotionalStateSpinner.setSelection(selectedPosition);
                }
            }
        } else if (currentFragment instanceof FollowingMoodsFragment) {
            checkboxRecentWeek.setChecked(((FollowingMoodsFragment) currentFragment).isFilterByRecentWeek());
            String selectedEmotionalState = ((FollowingMoodsFragment) currentFragment).getSelectedEmotionalState();
            if (!selectedEmotionalState.isEmpty()) {
                String selectedDescription = EmojiUtils.getDescription(selectedEmotionalState);
                int selectedPosition = emotionalStates.indexOf(selectedDescription);
                if (selectedPosition != -1) {
                    emotionalStateSpinner.setSelection(selectedPosition);
                }
            }
        }

        tickButton.setOnClickListener(v -> {
            boolean filterByRecentWeek = checkboxRecentWeek.isChecked();
            String selectedDescription = (String) emotionalStateSpinner.getSelectedItem();
            String selectedEmotionalState = selectedDescription.equals("All Moods") ? "" : EmojiUtils.getEmojiKey(selectedDescription);

            if (currentFragment instanceof MyMoodsFragment) {
                ((MyMoodsFragment) currentFragment).setFilterByRecentWeek(filterByRecentWeek);
                ((MyMoodsFragment) currentFragment).applyFilter(selectedEmotionalState);
            } else if (currentFragment instanceof FollowingMoodsFragment) {
                ((FollowingMoodsFragment) currentFragment).setFilterByRecentWeek(filterByRecentWeek);
                ((FollowingMoodsFragment) currentFragment).applyFilter(selectedEmotionalState);
            }
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

    public void showBottomSheetDialog(DocumentSnapshot moodDoc) {
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
                    refreshCurrentFragment();
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

    public void navigateToMoodDetail(DocumentSnapshot moodDoc) {
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
            intent.putExtra("isMyMoods", viewPager.getCurrentItem() == 0);
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

    private void refreshCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (currentFragment instanceof MyMoodsFragment) {
            ((MyMoodsFragment) currentFragment).fetchMyMoods();
            Log.d("MainActivity", "Refreshing MyMoodsFragment");
        } else if (currentFragment instanceof FollowingMoodsFragment) {
            ((FollowingMoodsFragment) currentFragment).fetchFollowingMoods();
            Log.d("MainActivity", "Refreshing FollowingMoodsFragment");
        } else {
            Log.d("MainActivity", "No valid fragment found to refresh");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume called");
        refreshCurrentFragment();
        // Force ViewPager to re-render
        viewPagerAdapter.notifyDataSetChanged();
        viewPager.setCurrentItem(viewPager.getCurrentItem(), false); // Trigger re-render without animation
    }
}