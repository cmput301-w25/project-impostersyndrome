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
import com.example.impostersyndrom.controller.ConnectivityReceiver;
import com.example.impostersyndrom.controller.MainViewPagerAdapter;
import com.example.impostersyndrom.controller.NetworkUtils;
import com.example.impostersyndrom.model.EmojiUtils;
import com.example.impostersyndrom.model.ImageHandler;
import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.spotify.SpotifyManager;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ADD_MOOD = 1001; // Request code for AddMoodActivity

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
    private ConnectivityReceiver connectivityReceiver;

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
                    showMessage("Error loading profile: " + errorMessage);
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
                Intent intent = new Intent(this, MapActivity.class);
                intent.putExtra("userId", userId); // Add userId to the Intent
                startActivity(intent);
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
        // Allow addMoodButton to work even offline.
        addMoodButton.setOnClickListener(v -> navigateToEmojiSelection());

        // Restrict profileButton when offline.
        profileButton.setOnClickListener(v -> {
            if (NetworkUtils.isOffline(this)) {
                Toast.makeText(MainActivity.this, "You're offline", Toast.LENGTH_SHORT).show();
                return;
            }
            navigateToProfile();
        });

        // Restrict filterButton when offline.
        filterButton.setOnClickListener(v -> {
            if (NetworkUtils.isOffline(this)) {
                Toast.makeText(MainActivity.this, "You're offline", Toast.LENGTH_SHORT).show();
                return;
            }
            showFilterDialog();
        });

        // Restrict searchButton when offline.
        searchButton.setOnClickListener(v -> {
            if (NetworkUtils.isOffline(this)) {
                Toast.makeText(MainActivity.this, "You're offline", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
        });

        // Restrict heartButton (for following moods) when offline.
        heartButton.setOnClickListener(v -> {
            if (NetworkUtils.isOffline(this)) {
                Toast.makeText(MainActivity.this, "You're offline", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(MainActivity.this, FollowingActivity.class));
        });

        // Restrict menuButton if needed (if its functions are not add/edit/delete).
        menuButton.setOnClickListener(v -> {
            if (NetworkUtils.isOffline(this)) {
                Toast.makeText(MainActivity.this, "You're offline", Toast.LENGTH_SHORT).show();
                return;
            }
            toggleNavigationDrawer();
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
        Intent intent = new Intent(this, EmojiSelectionActivity.class); // Launch EmojiSelectionActivity
        intent.putExtra("userId", userId);
        startActivityForResult(intent, REQUEST_ADD_MOOD);
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
              showMessage("You're offline. Mood will be deleted once you're back online.");
              refreshCurrentFragment();
          } else {
              moodDataManager.deleteMood(moodDoc.getId(), new MoodDataManager.OnMoodDeletedListener() {
                  @Override
                  public void onMoodDeleted() {
                      showMessage("Mood deleted!");
                      refreshCurrentFragment();
                  }

                  @Override
                  public void onError(String errorMessage) {
                      showMessage("Failed to delete mood: " + errorMessage);
                  }
              });
          }

            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    public void navigateToMoodDetail(DocumentSnapshot moodDoc) {
        if (moodDoc == null || !moodDoc.exists()) {
            showMessage("Mood details unavailable.");
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
            intent.putExtra("accessToken", SpotifyManager.getInstance().getAccessToken());

            // Add latitude and longitude
            Double latitude = moodDoc.getDouble("latitude");
            Double longitude = moodDoc.getDouble("longitude");
            intent.putExtra("latitude", latitude != null ? latitude : 0.0);
            intent.putExtra("longitude", longitude != null ? longitude : 0.0);

            intent.putExtra("moodId", moodDoc.getId());
            startActivity(intent);
        } else {
            showMessage("Error loading mood details.");
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        showMessage("Logged out successfully!");
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void refreshCurrentFragment() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentByTag("f" + viewPager.getCurrentItem());
        if (currentFragment instanceof MyMoodsFragment) {
            if (NetworkUtils.isOffline(this)) {
                // In offline mode, show offline status and pending changes
                MoodDataManager manager = new MoodDataManager();
                List<MoodDataManager.OfflineEdit> offlineEdits = manager.getOfflineEdits(this);
                Set<String> offlineDeletes = manager.getOfflineDeletes(this);
                
                StringBuilder message = new StringBuilder("You're offline. ");
                if (!offlineEdits.isEmpty() || !offlineDeletes.isEmpty()) {
                    message.append("You have ");
                    if (!offlineEdits.isEmpty()) {
                        message.append(offlineEdits.size()).append(" edit(s) ");
                    }
                    if (!offlineEdits.isEmpty() && !offlineDeletes.isEmpty()) {
                        message.append("and ");
                    }
                    if (!offlineDeletes.isEmpty()) {
                        message.append(offlineDeletes.size()).append(" delete(s) ");
                    }
                    message.append("pending. Changes will sync when you're back online.");
                } else {
                    message.append("Your changes will sync when you're back online.");
                }
                showMessage(message.toString());
                Log.d("MainActivity", "Offline mode - skipping refresh");
                return;
            }
            // Use refreshMoods instead of fetchMyMoods to properly handle pagination
            ((MyMoodsFragment) currentFragment).refreshMoods();
            Log.d("MainActivity", "Refreshing MyMoodsFragment");
        } else if (currentFragment instanceof FollowingMoodsFragment) {
            if (NetworkUtils.isOffline(this)) {
                showMessage("You're offline. Your changes will sync when you're back online.");
                Log.d("MainActivity", "Offline mode - skipping refresh");
                return;
            }
            ((FollowingMoodsFragment) currentFragment).fetchFollowingMoods();
            Log.d("MainActivity", "Refreshing FollowingMoodsFragment");
        } else {
            Log.d("MainActivity", "No valid fragment found to refresh");
        }
    }

    /**
     * Displays a Snackbar message.
     *
     * @param message The message to display.
     */
    private void showMessage(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null && !isFinishing()) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ADD_MOOD && resultCode == RESULT_OK && data != null) {
            String moodId = data.getStringExtra("moodId");
            Log.d("MainActivity", "Mood added with ID: " + moodId);
            refreshCurrentFragment(); // Refresh MyMoodsFragment to show the new mood
            viewPager.setCurrentItem(0, true); // Switch to "My Moods" tab

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "onResume called");
        
        // Handle offline sync if we're online
        if (!NetworkUtils.isOffline(this)) {
            syncOfflineMoodsIfNeeded();
            // Only show sync message if there were actual changes
            MoodDataManager manager = new MoodDataManager();
            List<MoodDataManager.OfflineEdit> offlineEdits = manager.getOfflineEdits(this);
            Set<String> offlineDeletes = manager.getOfflineDeletes(this);
            List<MoodDataManager.OfflineMood> offlineMoods = manager.getOfflineMoods(this);
            
            if (!offlineEdits.isEmpty() || !offlineDeletes.isEmpty() || !offlineMoods.isEmpty()) {
                // Show sync success message
                StringBuilder message = new StringBuilder("Synced ");
                boolean hasMultipleChanges = false;
                
                if (!offlineMoods.isEmpty()) {
                    message.append(offlineMoods.size()).append(" new mood(s)");
                    hasMultipleChanges = true;
                }
                
                if (!offlineEdits.isEmpty()) {
                    if (hasMultipleChanges) message.append(", ");
                    message.append(offlineEdits.size()).append(" edit(s)");
                    hasMultipleChanges = true;
                }
                
                if (!offlineDeletes.isEmpty()) {
                    if (hasMultipleChanges) message.append(", ");
                    message.append(offlineDeletes.size()).append(" delete(s)");
                }
                
                message.append(" successfully!");
                showMessage(message.toString());
                
                // Only refresh if we actually synced changes
                new Handler().postDelayed(() -> refreshCurrentFragment(), 1000);
            }
        }
        
        // Only update the ViewPager if needed
        if (viewPagerAdapter != null) {
            viewPagerAdapter.notifyDataSetChanged();
            viewPager.setCurrentItem(viewPager.getCurrentItem(), false);
        }
    }

    private void syncOfflineMoodsIfNeeded() {
        if (!NetworkUtils.isOffline(this)) {
            MoodDataManager manager = new MoodDataManager();

            // Sync offline added moods
            List<MoodDataManager.OfflineMood> offlineMoods = manager.getOfflineMoods(this);
            if (!offlineMoods.isEmpty()) {
                Log.d("Sync", "Syncing offline moods: " + offlineMoods.size());
                for (MoodDataManager.OfflineMood mood : offlineMoods) {
                    Map<String, Object> moodData = new HashMap<>();
                    moodData.put("emotionalState", mood.emoji);
                    moodData.put("reason", mood.reason);
                    moodData.put("group", mood.group);
                    moodData.put("color", mood.color);
                    moodData.put("imageUrl", mood.imageUrl);
                    moodData.put("timestamp", new Timestamp(new Date(mood.timestamp)));
                    moodData.put("privateMood", mood.privateMood);
                    moodData.put("userId", userId);

                    db.collection("moods")
                            .add(moodData)
                            .addOnSuccessListener(documentReference -> {
                                Log.d("Sync", "Offline mood synced: " + documentReference.getId());
                            })
                            .addOnFailureListener(e -> Log.e("Sync", "Mood sync failed", e));
                }
                manager.clearOfflineMoods(this);
            }

            // Sync offline edits
            List<MoodDataManager.OfflineEdit> offlineEdits = manager.getOfflineEdits(this);
            if (!offlineEdits.isEmpty()) {
                Log.d("Sync", "Syncing offline edits: " + offlineEdits.size());
                for (MoodDataManager.OfflineEdit edit : offlineEdits) {
                    // Force update from server by using a get(Source.SERVER) after update.
                    FirebaseFirestore.getInstance().collection("moods").document(edit.moodId)
                            .update(edit.updates)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Sync", "Offline edit synced: " + edit.moodId);
                                // Force a fresh reload of this document from the server.
                                FirebaseFirestore.getInstance().collection("moods").document(edit.moodId)
                                        .get(com.google.firebase.firestore.Source.SERVER)
                                        .addOnSuccessListener(documentSnapshot -> {
                                            Log.d("Sync", "Refreshed mood " + edit.moodId + " from server: reason=" + documentSnapshot.getString("reason"));
                                            // Optionally, trigger a UI refresh here if needed.
                                        });
                            })
                            .addOnFailureListener(e -> Log.e("Sync", "Edit failed: " + edit.moodId, e));
                }
                manager.clearOfflineEdits(this);
            } else {
                Log.d("Sync", "No offline edits to sync.");
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
                MoodDataManager manager = new MoodDataManager();
                List<MoodDataManager.OfflineEdit> offlineEdits = manager.getOfflineEdits(MainActivity.this);
                Set<String> offlineDeletes = manager.getOfflineDeletes(MainActivity.this);
                
                if (!offlineEdits.isEmpty() || !offlineDeletes.isEmpty()) {
                    showMessage("Back online! Syncing your offline changes...");
                }
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