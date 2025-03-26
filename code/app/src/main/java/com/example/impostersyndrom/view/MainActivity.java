package com.example.impostersyndrom.view;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MainViewPagerAdapter;
import com.example.impostersyndrom.model.EmojiUtils;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.spotify.SpotifyManager;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ImageButton addMoodButton, profileButton, filterButton, searchButton, heartButton, menuButton;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private MainViewPagerAdapter viewPagerAdapter;
    private DrawerLayout drawerLayout;
    private NavigationView innerNavigationView;
    private LinearLayout logoutContainer;
    private MoodDataManager moodDataManager;
    private ProfileDataManager profileDataManager;
    private String userId;
    private FirebaseFirestore db;


    // Spotify Authentication
    private static final String CLIENT_ID = "ae52ad97cfd5446299f8883b4a6a6236";
    private static final String CLIENT_SECRET = "b40c6d9bfabd4f6592f7fb3210ca2f59";
    private String savedReasonFilter = "";
    private boolean savedRecentWeekFilter = false;
    private int savedEmotionalStatePosition = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        initializeViews();
        moodDataManager = new MoodDataManager();
        profileDataManager = new ProfileDataManager();

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            redirectToLogin();
            return;
        }
        userId = auth.getCurrentUser().getUid();
        Log.d("MainActivity", "userId: " + userId);

        getIntent().putExtra("userId", userId);

        // Initialize SpotifyManager
        SpotifyManager.getInstance().initialize(CLIENT_ID, CLIENT_SECRET);

        // Set up ViewPager and TabLayout

        setupViewPager();
        setupButtonListeners();
        setupNavigationDrawer();
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        addMoodButton = findViewById(R.id.addMoodButton);
        profileButton = findViewById(R.id.profileButton);
        filterButton = findViewById(R.id.filterButton);
        searchButton = findViewById(R.id.searchButton);
        heartButton = findViewById(R.id.heartButton);
        menuButton = findViewById(R.id.menuButton);
        drawerLayout = findViewById(R.id.drawerLayout);
        innerNavigationView = findViewById(R.id.innerNavigationView);
    }

    private void setupNavigationDrawer() {
        View navigationLayout = findViewById(R.id.navigation_layout);
        if (navigationLayout == null) {
            Log.e("MainActivity", "Navigation layout not found! Check activity_main.xml <include> ID.");
            return;
        }

        ImageView profileImage = navigationLayout.findViewById(R.id.profileImage);
        TextView userNameTextView = navigationLayout.findViewById(R.id.userNameTextView);
        TextView userEmailTextView = navigationLayout.findViewById(R.id.userEmailTextView);
        logoutContainer = navigationLayout.findViewById(R.id.logoutContainer);

        if (profileImage == null || userNameTextView == null || userEmailTextView == null) {
            Log.e("MainActivity", "One or more header views not found in navigation_layout!");
            return;
        }
        if (logoutContainer == null) {
            Log.e("MainActivity", "Logout container not found in navigation_layout!");
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            userNameTextView.setText("Guest");
            userEmailTextView.setText("Not logged in");
            profileImage.setImageResource(R.drawable.white_profile);
        } else {
            String email = auth.getCurrentUser().getEmail();
            userEmailTextView.setText(email != null ? email : "No email available");

            profileDataManager.fetchUserProfile(userId, new ProfileDataManager.OnProfileFetchedListener() {
                @Override
                public void onProfileFetched(DocumentSnapshot profileDoc) {
                    String username = profileDoc.getString("username");
                    Log.d("MainActivity", "Fetched username: " + username);
                    userNameTextView.setText(username != null && !username.isEmpty() ? username : "Anonymous");

                    String profileImageUrl = profileDoc.getString("profileImageUrl");
                    Log.d("MainActivity", "Fetched profileImageUrl: " + profileImageUrl);
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(MainActivity.this)
                                .load(profileImageUrl)
                                .circleCrop()
                                .placeholder(R.drawable.white_profile)
                                .error(R.drawable.white_profile)
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.white_profile);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e("MainActivity", "Error fetching profile: " + errorMessage);
                    userNameTextView.setText("Anonymous");
                    profileImage.setImageResource(R.drawable.white_profile);
                    Toast.makeText(MainActivity.this, "Error loading profile: " + errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        }

        logoutContainer.setOnClickListener(v -> {
            Log.d("MainActivity", "Logout clicked");
            drawerLayout.closeDrawer(GravityCompat.START);
            logoutUser();
        });

        innerNavigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.maps) {
                // Handle maps action
            } else if (id == R.id.nav_settings) {
                // Handle settings action
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
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
        profileButton.setOnClickListener(v -> navigateToProfile());
        filterButton.setOnClickListener(v -> showFilterDialog());
        searchButton.setOnClickListener(v -> startActivity(new Intent(this, SearchActivity.class)));
        heartButton.setOnClickListener(v -> startActivity(new Intent(this, FollowingActivity.class)));
        menuButton.setOnClickListener(v -> toggleNavigationDrawer());
    }

    private void navigateToProfile() {
        startActivity(new Intent(this, ProfileActivity.class));
    }

    private void toggleNavigationDrawer() {
        if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void navigateToEmojiSelection() {
        Intent intent = new Intent(this, EmojiSelectionActivity.class);
        intent.putExtra("userId", userId);
        startActivity(intent);
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
        EditText reasonInput = filterDialog.findViewById(R.id.reasonInput);
        ImageButton tickButton = filterDialog.findViewById(R.id.tickButton);

        checkboxRecentWeek.setChecked(savedRecentWeekFilter);
        reasonInput.setText(savedReasonFilter);

        List<String> emotionalStates = new ArrayList<>();
        emotionalStates.add("All Moods");
        emotionalStates.addAll(List.of(EmojiUtils.getEmojiDescriptions()));

        EmojiSpinnerAdapter spinnerAdapter = new EmojiSpinnerAdapter(this, emotionalStates, getEmojiDrawables());
        emotionalStateSpinner.setAdapter(spinnerAdapter);
        emotionalStateSpinner.setSelection(savedEmotionalStatePosition);

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
            // Save filter values
            savedRecentWeekFilter = checkboxRecentWeek.isChecked();
            savedReasonFilter = reasonInput.getText().toString().trim();
            savedEmotionalStatePosition = emotionalStateSpinner.getSelectedItemPosition();

            boolean filterByRecentWeek = checkboxRecentWeek.isChecked();
            String selectedDescription = (String) emotionalStateSpinner.getSelectedItem();
            String selectedEmotionalState = selectedDescription.equals("All Moods") ? "" : EmojiUtils.getEmojiKey(selectedDescription);
            String reasonFilter = reasonInput.getText().toString().trim();

            if (currentFragment instanceof MyMoodsFragment) {
                ((MyMoodsFragment) currentFragment).setFilterByRecentWeek(filterByRecentWeek);
                ((MyMoodsFragment) currentFragment).applyFilter(selectedEmotionalState, reasonFilter);
            } else if (currentFragment instanceof FollowingMoodsFragment) {
                ((FollowingMoodsFragment) currentFragment).setFilterByRecentWeek(filterByRecentWeek);
                ((FollowingMoodsFragment) currentFragment).applyFilter(selectedEmotionalState, reasonFilter);
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
        bottomSheetDialog.setContentView(bottomSheetView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bottomSheetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            bottomSheetDialog.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            );
        }

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
            boolean isPrivateMood = moodDoc.contains("privateMood") && Boolean.TRUE.equals(moodDoc.getBoolean("privateMood"));
            intent.putExtra("privateMood", isPrivateMood);
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
            intent.putExtra("accessToken", SpotifyManager.getInstance().getAccessToken());
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
        viewPagerAdapter.notifyDataSetChanged();
        viewPager.setCurrentItem(viewPager.getCurrentItem(), false);
    }
}