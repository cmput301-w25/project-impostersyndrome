package com.example.impostersyndrom.view;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.ConnectivityReceiver;
import com.example.impostersyndrom.controller.MainViewPagerAdapter;
import com.example.impostersyndrom.controller.NetworkUtils;
import com.example.impostersyndrom.model.EmojiUtils;
import com.example.impostersyndrom.model.ImageHandler;
import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.model.MoodDataManager;
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
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private ImageButton addMoodButton, profileButton, filterButton, searchButton, heartButton, menuButton;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private SwipeRefreshLayout swipeRefreshLayout; // Added for pull-to-refresh
    private MainViewPagerAdapter viewPagerAdapter;
    private DrawerLayout drawerLayout;
    private NavigationView innerNavigationView;
    private LinearLayout logoutContainer;
    private MoodDataManager moodDataManager;
    private ProfileDataManager profileDataManager;
    private String userId;
    private FirebaseFirestore db;
    private ConnectivityReceiver connectivityReceiver;

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
        setupViewPager();
        setupButtonListeners();
        setupNavigationDrawer();

        // Set up swipe-to-refresh
        setupSwipeRefresh();
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout); // Initialize SwipeRefreshLayout
        addMoodButton = findViewById(R.id.addMoodButton);
        profileButton = findViewById(R.id.profileButton);
        filterButton = findViewById(R.id.filterButton);
        searchButton = findViewById(R.id.searchButton);
        heartButton = findViewById(R.id.heartButton);
        menuButton = findViewById(R.id.menuButton);
        drawerLayout = findViewById(R.id.drawerLayout);
        innerNavigationView = findViewById(R.id.innerNavigationView);
        // Note: userNameTextView, userEmailTextView, and logoutContainer are initialized in setupNavigationDrawer
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
                // Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                // startActivity(intent);
            } else if (id == R.id.nav_settings) {
                // Handle settings action
                // Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                // startActivity(intent);
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

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            Log.d("MainActivity", "Swipe to refresh triggered");
            refreshCurrentFragment();
            // Stop the refresh animation once data is loaded
            swipeRefreshLayout.setRefreshing(false);
        });
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
        bottomSheetDialog.setContentView(bottomSheetView); // Fixed: should be bottomSheetView

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
            Number colorNumber = (Number) moodDoc.get("color");
            intent.putExtra("color", colorNumber.intValue());
            intent.putExtra("group", (String) moodDoc.get("group"));
            boolean isPrivateMood = moodDoc.contains("privateMood") && Boolean.TRUE.equals(moodDoc.getBoolean("privateMood"));
            intent.putExtra("privateMood", isPrivateMood);
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });

        deleteMood.setOnClickListener(v -> {
            if (NetworkUtils.isOffline(this)) {
                Log.d("OfflineDelete", "Offline branch taken for moodId: " + moodDoc.getId());
                new MoodDataManager().saveOfflineDelete(this, moodDoc.getId());
                showToast("You're offline. Mood will be deleted once you're back online.");
                refreshCurrentFragment();
            } else {
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
            }
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.setContentView(bottomSheetView); // Fixed: should be bottomSheetView
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
            Number colorNumber = (Number) data.getOrDefault("color", 0);
            intent.putExtra("color", colorNumber.intValue());
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
        syncOfflineMoodsIfNeeded();
        refreshCurrentFragment();
        viewPagerAdapter.notifyDataSetChanged();
        viewPager.setCurrentItem(viewPager.getCurrentItem(), false);
    }

    private void syncOfflineMoodsIfNeeded() {
        if (!NetworkUtils.isOffline(this)) {
            MoodDataManager manager = new MoodDataManager();

            // Sync offline added moods
            List<Mood> offlineMoods = manager.getOfflineMoods(this);
            Log.d("Sync", "Found " + offlineMoods.size() + " offline moods.");
            if (!offlineMoods.isEmpty()) {
                for (Mood mood : offlineMoods) {
                    // Check if the mood has a local image URI.
                    if (mood.getImageUrl() != null && mood.getImageUrl().startsWith("file://")) {
                        ImageHandler offlineImageHandler = new ImageHandler(this, null);
                        offlineImageHandler.uploadImageFromLocalUri(mood.getImageUrl(), new ImageHandler.OnImageUploadListener() {
                            @Override
                            public void onImageUploadSuccess(String url) {
                                mood.setImageUrl(url); // Update the mood with the remote URL
                                manager.addMood(mood, new MoodDataManager.OnMoodAddedListener() {
                                    @Override
                                    public void onMoodAdded() {
                                        Log.d("Sync", "Offline add synced (with image upload): " + mood.getId());
                                    }
                                    @Override
                                    public void onError(String errorMessage) {
                                        Log.e("Sync", "Add failed: " + errorMessage);
                                    }
                                });
                            }
                            @Override
                            public void onImageUploadFailure(Exception e) {
                                Log.e("Sync", "Failed to upload offline image: " + e.getMessage());
                            }
                        });
                    } else {
                        // No local image, add the mood normally.
                        manager.addMood(mood, new MoodDataManager.OnMoodAddedListener() {
                            @Override
                            public void onMoodAdded() {
                                Log.d("Sync", "Offline add synced: " + mood.getId());
                            }
                            @Override
                            public void onError(String errorMessage) {
                                Log.e("Sync", "Add failed: " + errorMessage);
                            }
                        });
                    }
                }
                manager.clearOfflineMoods(this);
            } else {
                Log.d("Sync", "No offline added moods to sync.");
            }

            // Sync offline deletes
            Set<String> deleteIds = manager.getOfflineDeletes(this);
            if (!deleteIds.isEmpty()) {
                Log.d("Sync", "Syncing offline deletes: " + deleteIds.size());
                for (String moodId : deleteIds) {
                    FirebaseFirestore.getInstance().collection("moods").document(moodId)
                            .delete()
                            .addOnSuccessListener(aVoid -> Log.d("Sync", "Offline delete synced: " + moodId))
                            .addOnFailureListener(e -> Log.e("Sync", "Delete failed: " + moodId, e));
                }
                manager.clearOfflineDeletes(this);
            } else {
                Log.d("Sync", "No offline deletes to sync.");
            }
        }
    }

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            runOnUiThread(() -> {
                Log.d("NetworkCallback", "Network available - syncing offline data");
                syncOfflineMoodsIfNeeded();
                // Delay the UI refresh for 2 seconds to allow async tasks to complete.
                new android.os.Handler().postDelayed(() -> refreshCurrentFragment(), 2000);
            });
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        connectivityReceiver = new ConnectivityReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest networkRequest = new NetworkRequest.Builder().build();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(connectivityReceiver);
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
}