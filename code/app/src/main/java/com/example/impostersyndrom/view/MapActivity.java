package com.example.impostersyndrom.view;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.EditText;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.Mood;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Query;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import org.osmdroid.util.BoundingBox;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicInteger;
import android.util.Log;

public class MapActivity extends AppCompatActivity {
    private MapView mapView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private MyLocationNewOverlay myLocationOverlay;
    private RotationGestureOverlay rotationGestureOverlay;
    private ImageButton filterButton;
    private boolean filterLastWeek = false;
    private String selectedMoodFilter = "All";
    private String keywordFilter = "";
    private boolean filterFollowing = false;
    private static final String TAG = "MapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configuration
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_mood_location_map);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize and configure map
        initializeMap();


        loadMoods();
        filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    private void initializeMap() {
        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Set initial position to Edmonton, Alberta and zoom level
        mapView.getController().setZoom(12.0); // City level zoom
        mapView.getController().setCenter(new GeoPoint(53.5461, -113.4937)); // Edmonton coordinates

        // Set bounds so it doesn't show multiple times (can't scroll infinitely)
        BoundingBox boundingBox = new BoundingBox(85, 180, -85, -180); // max latitude, max longitude, min latitude, min longitude
        mapView.setScrollableAreaLimitDouble(boundingBox);

        // Disable wrap
        mapView.setHorizontalMapRepetitionEnabled(false);
        mapView.setVerticalMapRepetitionEnabled(false);

        // Add location overlay
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        // Add rotation gesture overlay
        rotationGestureOverlay = new RotationGestureOverlay(mapView);
        rotationGestureOverlay.setEnabled(true);
        mapView.getOverlays().add(rotationGestureOverlay);

        // Set min/max zoom levels
        mapView.setMinZoomLevel(2.0);
        mapView.setMaxZoomLevel(18.0);
    }

    private void requestPermissions() {
        String[] permissions = {
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View filterView = getLayoutInflater().inflate(R.layout.dialog_map_filter, null);
        builder.setView(filterView);

        // Initialize filter controls
        CheckBox lastWeekCheckbox = filterView.findViewById(R.id.lastWeekCheckbox);
        CheckBox followingCheckbox = filterView.findViewById(R.id.followingMoods);
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
        followingCheckbox.setChecked(filterFollowing);
        
        // Find the correct index for the selected mood filter
        int selectedIndex = moodOptions.indexOf(selectedMoodFilter);
        if (selectedIndex == -1) {
            selectedIndex = 0; // Default to "All" if not found
        }
        moodSpinner.setSelection(selectedIndex);
        
        keywordSearch.setText(keywordFilter);

        builder.setPositiveButton("Apply", (dialog, which) -> {
            // Save filter values
            filterLastWeek = lastWeekCheckbox.isChecked();
            filterFollowing = followingCheckbox.isChecked();
            selectedMoodFilter = moodSpinner.getSelectedItem().toString();
            keywordFilter = keywordSearch.getText().toString().trim();

            // Refresh map with new filters
            refreshMapWithFilters();
        });

        builder.setNegativeButton("Cancel", null);

        builder.setNeutralButton("Clear Filters", (dialog, which) -> {
            filterLastWeek = false;
            filterFollowing = false;
            selectedMoodFilter = "All";
            keywordFilter = "";
            refreshMapWithFilters();
        });

        builder.show();
    }

    private void refreshMapWithFilters() {
        // Clear existing markers
        mapView.getOverlays().clear();
        
        // Re-add non-marker overlays
        mapView.getOverlays().add(myLocationOverlay);
        mapView.getOverlays().add(rotationGestureOverlay);
        
        // Reload moods
        loadMoods();
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

        // Filter by mood (case-insensitive comparison)
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

    private void loadMoods() {
        String currentUserId = auth.getCurrentUser().getUid();

        // Only load user's own moods if not filtering by following
        if (!filterFollowing) {
            // Load user's own moods
            db.collection("moods")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Mood mood = document.toObject(Mood.class);
                        if (shouldDisplayMood(mood)) {
                            addMoodMarker(mood, true);
                        }
                    }

                    // Load all public moods from other users
                    db.collection("moods")
                        .whereNotEqualTo("userId", currentUserId)
                        .whereEqualTo("isPublic", true)
                        .get()
                        .addOnSuccessListener(otherMoods -> {
                            for (QueryDocumentSnapshot document : otherMoods) {
                                Mood mood = document.toObject(Mood.class);
                                if (shouldDisplayMood(mood)) {
                                    addMoodMarker(mood, false);
                                }
                            }
                        });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading moods: " + e.getMessage());
                });
        } else {
            // If filtering by following, only load moods from followed users
            Log.d(TAG, "Starting following filter with current user ID: " + currentUserId);
            
            // Query the following collection with the correct structure
            db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Log.d(TAG, "Following collection query completed. Found " + querySnapshot.size() + " documents");
                    
                    // Log all documents to see their structure
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Log.d(TAG, "Following document ID: " + doc.getId());
                        Log.d(TAG, "Following document data: " + doc.getData().toString());
                    }
                    
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "No following documents found for user: " + currentUserId);
                        Toast.makeText(this, "You are not following any users", 
                            Toast.LENGTH_SHORT).show();
                    }
                    
                    List<String> followingIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String followingId = doc.getString("followingId");
                        if (followingId != null) {
                            followingIds.add(followingId);
                            Log.d(TAG, "Added following ID: " + followingId);
                        } else {
                            Log.d(TAG, "Document " + doc.getId() + " has no followingId field");
                        }
                    }

                    Log.d(TAG, "Total following IDs found: " + followingIds.size());

                    // Load moods for each followed user
                    for (String followedUserId : followingIds) {
                        Log.d(TAG, "Loading moods for followed user: " + followedUserId);
                        loadUserMoods(followedUserId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying following collection: " + e.getMessage());
                });
        }
    }

    private void loadUserMoods(String userId) {
        Log.d(TAG, "Querying moods for user: " + userId);
        
        // Log the query details
        Log.d(TAG, "Mood query parameters: userId=" + userId);
        
        // Query all moods for the user first
        db.collection("moods")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " moods for user " + userId);
                
                if (queryDocumentSnapshots.isEmpty()) {
                    Log.d(TAG, "No moods found for user: " + userId);
                    return;
                }

                // Log all document data
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    String moodInfo = "Mood Details:\n" +
                        "ID: " + document.getId() + "\n" +
                        "User ID: " + document.getString("userId") + "\n" +
                        "Emotional State: " + document.getString("emotionalState") + "\n" +
                        "Reason: " + document.getString("reason") + "\n" +
                        "Latitude: " + document.getDouble("latitude") + "\n" +
                        "Longitude: " + document.getDouble("longitude") + "\n" +
                        "Timestamp: " + document.getTimestamp("timestamp") + "\n" +
                        "Is Public: " + document.getBoolean("isPublic") + "\n" +
                        "Private Mood: " + document.getBoolean("privateMood") + "\n" +
                        "Group: " + document.getString("group") + "\n" +
                        "Raw Data: " + document.getData().toString();
                    
                    Log.d(TAG, moodInfo);
                }

                AtomicInteger validMoods = new AtomicInteger(0);
                AtomicInteger processedMoods = new AtomicInteger(0);
                
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    // Check if the mood is public (either isPublic is true or privateMood is false)
                    Boolean isPublic = document.getBoolean("isPublic");
                    Boolean privateMood = document.getBoolean("privateMood");
                    
                    // Skip if the mood is explicitly private
                    if (Boolean.TRUE.equals(privateMood)) {
                        Log.d(TAG, "Skipping private mood: " + document.getId());
                        continue;
                    }
                    
                    Mood mood = document.toObject(Mood.class);
                    processedMoods.incrementAndGet();
                    
                    Log.d(TAG, "Processing mood " + processedMoods.get() + "/" + 
                        queryDocumentSnapshots.size() + ": " + mood.getEmotionalState() + 
                        " at " + mood.getLatitude() + ", " + mood.getLongitude());
                    
                    if (shouldDisplayMood(mood)) {
                        // Run on UI thread to ensure marker is added properly
                        runOnUiThread(() -> {
                            addMoodMarker(mood, false);
                            validMoods.incrementAndGet();
                            Log.d(TAG, "Added marker for mood: " + mood.getEmotionalState() + 
                                " at " + mood.getLatitude() + ", " + mood.getLongitude());
                            
                            // Check if we've processed all moods
                            if (processedMoods.get() == queryDocumentSnapshots.size()) {
                                String finalMessage = "Finished processing all moods for user " + userId + 
                                    ". Added " + validMoods.get() + " markers.";
                                Log.d(TAG, finalMessage);
                            }
                        });
                    } else {
                        Log.d(TAG, "Mood filtered out: " + mood.getEmotionalState());
                    }
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading moods for user " + userId + ": " + e.getMessage());
            });
    }

    private void addMoodMarker(Mood mood, boolean isOwnMood) {
        try {
            Log.d(TAG, "Starting to add marker for mood: " + mood.getEmotionalState() + 
                " at " + mood.getLatitude() + ", " + mood.getLongitude());
            
            // Create marker
            Marker marker = new Marker(mapView);
            
            // Set position with null check
            if (mood.getLatitude() != null && mood.getLongitude() != null) {
                GeoPoint position = new GeoPoint(mood.getLatitude(), mood.getLongitude());
                marker.setPosition(position);
                Log.d(TAG, "Marker position set to: " + position);
            } else {
                String errorMessage = "Invalid location data for mood: " + mood.getEmotionalState();
                Log.e(TAG, errorMessage);
                return;
            }

            // Store the mood object in the marker for later retrieval
            marker.setRelatedObject(mood);

            // Set marker icon based on mood emoji drawable ID and resize it
            if (mood.getEmojiDrawableId() != 0) {
                Drawable originalDrawable = getResources().getDrawable(mood.getEmojiDrawableId());
                Log.d(TAG, "Original drawable loaded with ID: " + mood.getEmojiDrawableId());

                // Convert drawable to bitmap and resize it
                Bitmap originalBitmap = ((BitmapDrawable) originalDrawable).getBitmap();
                int newWidth = 20;  // Width in pixels
                int newHeight = 20; // Height in pixels
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

                // Convert back to drawable
                Drawable resizedDrawable = new BitmapDrawable(getResources(), resizedBitmap);
                marker.setIcon(resizedDrawable);
                Log.d(TAG, "Marker icon set successfully");
            } else {
                Log.d(TAG, "No emoji drawable ID found for mood");
            }

            // Set marker title with emotional state and reason
            String title = mood.getEmotionalState() + " - " + mood.getReason();
            marker.setTitle(title);
            Log.d(TAG, "Marker title set to: " + title);

            // Set marker snippet with timestamp and group context
            String snippet = isOwnMood ?
                "Your mood" + " (" + mood.getGroup() + ")" :
                "Mood by " + mood.getUserId() + " (" + mood.getGroup() + ")";
            marker.setSnippet(snippet);
            Log.d(TAG, "Marker snippet set to: " + snippet);

            // Add click listener to the marker
            marker.setOnMarkerClickListener((marker1, mapView) -> {
                Mood clickedMood = (Mood) marker1.getRelatedObject();
                if (clickedMood != null) {
                    Log.d(TAG, "Marker clicked, opening mood detail for: " + clickedMood.getEmotionalState());
                    openMoodDetail(clickedMood);
                }
                return true;
            });

            // Add the marker to the map and ensure it's visible
            mapView.getOverlays().add(marker);
            mapView.invalidate(); // Force map to redraw
            String successMessage = "Marker added to map for mood: " + mood.getEmotionalState() + 
                " at " + mood.getLatitude() + ", " + mood.getLongitude();
            Log.d(TAG, successMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error adding marker: " + e.getMessage());
        }
    }

    private void openMoodDetail(Mood mood) {
        Intent intent = new Intent(this, MoodDetailActivity.class);
        intent.putExtra("MOOD_ID", mood.getId());
        intent.putExtra("USER_ID", mood.getUserId());
        startActivity(intent);
    }
}
