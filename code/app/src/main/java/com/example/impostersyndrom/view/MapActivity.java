package com.example.impostersyndrom.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.Mood;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = "MapActivity";
    private MapView mapView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MyLocationNewOverlay myLocationOverlay;
    private ImageButton filterButton;
    private BottomNavigationView bottomNavigationView;
    private boolean showingMyMoods = true;
    private boolean filterLastWeek = false;
    private String selectedMoodFilter = "All";
    private String keywordFilter = "";
    private boolean filterFollowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_mood_location_map);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        mapView = findViewById(R.id.mapView);
        filterButton = findViewById(R.id.filterButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Initialize and configure map
        initializeMap();

        // Set up bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_my_moods) {
                    showMyMoods();
                    return true;
                } else if (itemId == R.id.nav_following_moods) {
                    showFollowingMoods();
                    return true;
                }
                return false;
            }
        });

        // Set filter button listener
        filterButton.setOnClickListener(v -> showFilterDialog());

        // Load initial view (My Moods)
        bottomNavigationView.setSelectedItemId(R.id.nav_my_moods);
        showMyMoods();
    }

    private void initializeMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(53.5461, -113.4937));

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void showMyMoods() {
        showingMyMoods = true;
        filterFollowing = false;  // Reset to show only my moods
        refreshMapWithFilters();
    }

    private void showFollowingMoods() {
        showingMyMoods = false;
        filterFollowing = true;  // Set to show following moods
        refreshMapWithFilters();
    }

    private void loadMoods() {
        String currentUserId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Loading moods for user: " + currentUserId + ", filterFollowing: " + filterFollowing);

        if (!filterFollowing) {
            // Load user's own moods
            db.collection("moods")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        Log.d(TAG, "My Moods: Found " + queryDocumentSnapshots.size() + " moods");
                        if (queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "No moods found for you", Toast.LENGTH_SHORT).show();
                        }
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Mood mood = document.toObject(Mood.class);
                            if (shouldDisplayMood(mood)) {
                                Log.d(TAG, "Adding my mood: " + mood.getEmotionalState() + " at " + mood.getLatitude() + "," + mood.getLongitude());
                                addMoodMarker(mood, true);
                            }
                        }
                        mapView.invalidate();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading my moods: " + e.getMessage());
                        Toast.makeText(this, "Error loading your moods", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Load moods from followed users
            Log.d(TAG, "Starting following filter with current user ID: " + currentUserId);

            db.collection("following")
                    .whereEqualTo("followerId", currentUserId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        Log.d(TAG, "Following collection query completed. Found " + querySnapshot.size() + " documents");
                        if (querySnapshot.isEmpty()) {
                            Log.d(TAG, "No following documents found for user: " + currentUserId);
                            Toast.makeText(this, "You are not following any users", Toast.LENGTH_SHORT).show();
                            mapView.invalidate();
                            return;
                        }

                        List<String> followingIds = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String followingId = doc.getString("followingId");
                            if (followingId != null) {
                                followingIds.add(followingId);
                                Log.d(TAG, "Added following ID: " + followingId);
                            }
                        }

                        if (followingIds.isEmpty()) {
                            Log.d(TAG, "No valid following IDs found");
                            Toast.makeText(this, "No followed users found", Toast.LENGTH_SHORT).show();
                            mapView.invalidate();
                            return;
                        }

                        Log.d(TAG, "Total following IDs found: " + followingIds.size());
                        int[] processedUsers = {0};
                        for (String followedUserId : followingIds) {
                            Log.d(TAG, "Loading moods for followed user: " + followedUserId);
                            db.collection("moods")
                                    .whereEqualTo("userId", followedUserId)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " moods for user " + followedUserId);
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            Boolean privateMood = document.getBoolean("privateMood");
                                            if (Boolean.TRUE.equals(privateMood)) {
                                                Log.d(TAG, "Skipping private mood: " + document.getId());
                                                continue;
                                            }
                                            Mood mood = document.toObject(Mood.class);
                                            if (shouldDisplayMood(mood)) {
                                                Log.d(TAG, "Adding following mood: " + mood.getEmotionalState() + " at " +
                                                        mood.getLatitude() + "," + mood.getLongitude() + " by " + mood.getUserId());
                                                addMoodMarker(mood, false);
                                            }
                                        }
                                        processedUsers[0]++;
                                        if (processedUsers[0] == followingIds.size()) {
                                            Log.d(TAG, "Finished loading all following moods");
                                            mapView.invalidate();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error loading moods for user " + followedUserId + ": " + e.getMessage());
                                        processedUsers[0]++;
                                        if (processedUsers[0] == followingIds.size()) {
                                            mapView.invalidate();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error querying following collection: " + e.getMessage());
                        Toast.makeText(this, "Error loading followed users", Toast.LENGTH_SHORT).show();
                        mapView.invalidate();
                    });
        }
    }

    private boolean shouldDisplayMood(Mood mood) {
        Log.d(TAG, "Checking if mood should be displayed: " + mood.getEmotionalState());

        if (mood.getLatitude() == null || mood.getLongitude() == null) {
            Log.d(TAG, "Mood filtered out: Invalid location data");
            return false;
        }

        // Filter by last week
        if (filterLastWeek) {
            Calendar lastWeek = Calendar.getInstance();
            lastWeek.add(Calendar.WEEK_OF_YEAR, -1);
            if (mood.getTimestamp().before(lastWeek.getTime())) {
                Log.d(TAG, "Mood filtered out: Older than last week");
                return false;
            }
        }

        // Filter by mood type
        if (!selectedMoodFilter.equals("All")) {
            Log.d(TAG, "Checking mood filter: Selected=" + selectedMoodFilter +
                    ", Mood=" + mood.getEmotionalState().toLowerCase());
            if (!mood.getEmotionalState().toLowerCase().equals(selectedMoodFilter.toLowerCase())) {
                Log.d(TAG, "Mood filtered out: Doesn't match selected mood filter");
                return false;
            }
        }

        // Filter by keyword
        if (!keywordFilter.isEmpty()) {
            String lowerKeyword = keywordFilter.toLowerCase();
            String lowerReason = mood.getReason().toLowerCase();
            String lowerEmotionalState = mood.getEmotionalState().toLowerCase();

            Log.d(TAG, "Checking keyword filter: Keyword=" + lowerKeyword +
                    ", Reason=" + lowerReason + ", EmotionalState=" + lowerEmotionalState);

            boolean matchesKeyword = lowerReason.contains(lowerKeyword) ||
                    lowerEmotionalState.contains(lowerKeyword);

            if (!matchesKeyword) {
                Log.d(TAG, "Mood filtered out: Doesn't match keyword filter");
                return false;
            }
        }

        Log.d(TAG, "Mood passed all filters and will be displayed");
        return true;
    }

    private void addMoodMarker(Mood mood, boolean isOwnMood) {
        if (mood.getLatitude() == null || mood.getLongitude() == null) {
            Log.w(TAG, "Skipping mood with null coordinates: " + mood.getEmotionalState());
            return;
        }

        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(mood.getLatitude(), mood.getLongitude()));
        marker.setRelatedObject(mood);

        try {
            if (mood.getEmojiDrawableId() != 0) {
                Drawable originalDrawable = getResources().getDrawable(mood.getEmojiDrawableId());
                Bitmap bitmap = convertDrawableToBitmap(originalDrawable);
                if (bitmap != null) {
                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 20, 20, true);
                    marker.setIcon(new BitmapDrawable(getResources(), resizedBitmap));
                } else {
                    Log.w(TAG, "Failed to convert drawable to bitmap for mood: " + mood.getEmotionalState());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting marker icon: " + e.getMessage());
        }

        marker.setTitle(mood.getEmotionalState() + " - " + mood.getReason());
        marker.setSnippet(isOwnMood ? "Your mood" : "Mood by " + mood.getUserId());

        marker.setOnMarkerClickListener((marker1, mapView) -> {
            Mood clickedMood = (Mood) marker1.getRelatedObject();
            if (clickedMood != null) {
                openMoodDetail(clickedMood);
            }
            return true;
        });

        mapView.getOverlays().add(marker);
    }

    private Bitmap convertDrawableToBitmap(Drawable drawable) {
        if (drawable == null) return null;

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        if (width <= 0 || height <= 0) {
            width = 20;
            height = 20;
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View filterView = getLayoutInflater().inflate(R.layout.dialog_map_filter, null);
        builder.setView(filterView);

        // Initialize filter controls
        CheckBox lastWeekCheckbox = filterView.findViewById(R.id.lastWeekCheckbox);
        Spinner moodSpinner = filterView.findViewById(R.id.moodSpinner);
        EditText keywordSearch = filterView.findViewById(R.id.keywordSearch);

        // Set up mood spinner
        ArrayList<String> moodOptions = new ArrayList<>();
        moodOptions.add("All");
        moodOptions.addAll(Arrays.asList("emoji_happy", "emoji_sad", "emoji_angry", "emoji_fear",
                "emoji_confused", "emoji_shame", "emoji_surprised", "emoji_disgust"));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, moodOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(adapter);

        // Set current filter values
        lastWeekCheckbox.setChecked(filterLastWeek);
        int selectedIndex = moodOptions.indexOf(selectedMoodFilter);
        if (selectedIndex == -1) selectedIndex = 0;
        moodSpinner.setSelection(selectedIndex);
        keywordSearch.setText(keywordFilter);

        builder.setPositiveButton("Apply", (dialog, which) -> {
            filterLastWeek = lastWeekCheckbox.isChecked();
            selectedMoodFilter = moodSpinner.getSelectedItem().toString();
            keywordFilter = keywordSearch.getText().toString().trim();
            refreshMapWithFilters();
        });

        builder.setNegativeButton("Cancel", null);

        builder.setNeutralButton("Clear Filters", (dialog, which) -> {
            filterLastWeek = false;
            filterFollowing = false;
            selectedMoodFilter = "All";
            keywordFilter = "";
            showMyMoods();
        });

        builder.show();
    }

    private void refreshMapWithFilters() {
        // Clear existing markers
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);
        loadMoods();
    }

    private void openMoodDetail(Mood mood) {
        Intent intent = new Intent(this, MoodDetailActivity.class);
        intent.putExtra("MOOD_ID", mood.getId());
        intent.putExtra("USER_ID", mood.getUserId());
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}