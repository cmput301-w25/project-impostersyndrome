package com.example.impostersyndrom.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.impostersyndrom.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.apache.tools.ant.taskdefs.Copy;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsFollowingActivity extends AppCompatActivity {

    private static final String TAG = "MapsFollowingActivity";
    private static final long REFRESH_INTERVAL = 30000; // 30 seconds
    private static final double MAX_DISTANCE_KM = 5.0;

    private MapView mapView;
    private ImageButton backButton;
    private ImageButton refreshButton;
    private Handler refreshHandler;
    private Runnable refreshRunnable;
    private Location currentLocation;
    private FirebaseFirestore db;
    private String currentUserId;

    private ProgressBar progressBar;
    private LinearLayout emptyStateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_followed_users_mood_map);

        // Initialize osmdroid configuration
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Get current user ID
        String passedUserId = getIntent().getStringExtra("userId");

        // Use the passed userId or fall back to current user if null
        if (passedUserId != null && !passedUserId.isEmpty()) {
            currentUserId = passedUserId;
            Log.d(TAG, "Using passed userId: " + currentUserId);
        } else {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Log.d(TAG, "Using current user ID: " + currentUserId);
        }

        db = FirebaseFirestore.getInstance();

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        mapView = findViewById(R.id.mapView);
        backButton = findViewById(R.id.backButton);
        refreshButton = findViewById(R.id.refreshButton);
        emptyStateView = findViewById(R.id.emptyStateView);

        // Get current location (you'll need to implement location fetching)
        currentLocation = getCurrentLocation(); // Implement this method

        // Set up the map
        setupMap();

        // Load initial data
        loadFollowedUsersMoodEvents();

        // Set up refresh button
        refreshButton.setOnClickListener(v -> {
            loadFollowedUsersMoodEvents();
            Toast.makeText(this, "Refreshing mood events...", Toast.LENGTH_SHORT).show();
        });

        // Set up back button
        backButton.setOnClickListener(v -> finish());

        // Set up auto-refresh
        setupAutoRefresh();
    }

    private void setupAutoRefresh() {
        refreshHandler = new Handler();
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadFollowedUsersMoodEvents();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Center map on current location if available
        if (currentLocation != null) {
            GeoPoint center = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
            mapView.getController().setZoom(15.0);
            mapView.getController().setCenter(center);
        } else {
            // Default to a fallback location
            GeoPoint defaultLocation = new GeoPoint(49.2827, -123.1207); // Vancouver, BC
            mapView.getController().setZoom(15.0);
            mapView.getController().setCenter(defaultLocation);
        }
        mapView.invalidate();
        Log.d(TAG, "Map setup complete");
    }

    private void loadFollowedUsersMoodEvents() {
        Log.d(TAG, "Starting loadFollowedUsersMoodEvents for userId: " + currentUserId);

        // Query the 'following' collection where current user is the follower
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Query success! Documents returned: " + task.getResult().size());

                            // Print the raw data of each document for debugging
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, "Document ID: " + document.getId());
                                Log.d(TAG, "Document data: " + document.getData());
                            }

                            List<String> followedUserIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String followedUserId = document.getString("followingId");
                                Log.d(TAG, "Extracted followingUserId: " + followedUserId);

                                if (followedUserId != null) {
                                    followedUserIds.add(followedUserId);
                                }
                            }

                            Log.d(TAG, "Final followedUserIds list size: " + followedUserIds.size());
                            Log.d(TAG, "Final followedUserIds list contents: " + followedUserIds);

                            if (!followedUserIds.isEmpty()) {
                                Log.d(TAG, "Found followed users, proceeding to getRecentMoodEvents");
                                getRecentMoodEvents(followedUserIds);
                            } else {
                                Log.d(TAG, "No followed users found for userId: " + currentUserId);
                                Toast.makeText(MapsFollowingActivity.this,
                                        "You're not following anyone yet", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Query failed", task.getException());
                            Toast.makeText(MapsFollowingActivity.this,
                                    "Failed to load followed users", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getRecentMoodEvents(List<String> userIds) {
        Log.d(TAG, "Starting getRecentMoodEvents for " + userIds.size() + " users");

        // Clear existing markers
        mapView.getOverlays().clear();
        Log.d(TAG, "Cleared existing map overlays");

        // Add current user's location marker if available
        if (currentLocation != null) {
            addCurrentLocationMarker();
            Log.d(TAG, "Added current user location marker at: " +
                    currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
        } else {
            Log.d(TAG, "Current location is null, skipping current location marker");
        }

        // For each followed user, get their most recent mood event with location
        for (String userId : userIds) {
            Log.d(TAG, "Querying mood events for user: " + userId);

            db.collection("mood")
                    .whereEqualTo("userId", userId)
                    .whereNotEqualTo("latitude", null)  // Changed from "location" to "latitude"
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Mood query successful for user: " + userId);

                                if (task.getResult().isEmpty()) {
                                    Log.d(TAG, "No mood events with location found for user: " + userId);
                                } else {
                                    Log.d(TAG, "Found " + task.getResult().size() + " mood events for user: " + userId);

                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        Map<String, Object> moodEvent = document.getData();
                                        Log.d(TAG, "Mood event data: " + moodEvent);

                                        Double latitude = (Double) moodEvent.get("latitude");
                                        Double longitude = (Double) moodEvent.get("longitude");
                                        String emoji = (String) moodEvent.get("emotionalState");  // Changed from "emoji" to "emotionalState"

                                        if (latitude == null || longitude == null) {
                                            Log.e(TAG, "Location coordinates are null for mood event: " + document.getId());
                                            continue;
                                        }

                                        if (emoji == null) {
                                            Log.e(TAG, "Emoji is null for mood event: " + document.getId());
                                            continue;
                                        }

                                        Log.d(TAG, "Processing mood event - Location: " +
                                                latitude + ", " + longitude + " Emoji: " + emoji);

                                        // Convert coordinates to osmdroid GeoPoint
                                        org.osmdroid.util.GeoPoint eventLocation =
                                                new org.osmdroid.util.GeoPoint(latitude, longitude);

                                        // Check if within 5km of current location
                                        boolean isWithin = isWithinDistance(latitude, longitude);
                                        Log.d(TAG, "Is mood event within distance? " + isWithin);

                                        if (isWithin) {
                                            Log.d(TAG, "Adding mood marker for user: " + userId +
                                                    " with emoji: " + emoji);
                                            addMoodMarker(eventLocation, emoji, userId);
                                        } else {
                                            Log.d(TAG, "Mood event is too far away (" +
                                                    getDistanceInKm(latitude, longitude) + " km), not adding marker");
                                        }
                                    }
                                }
                            } else {
                                Log.e(TAG, "Failed to query mood events for user: " + userId,
                                        task.getException());
                            }
                        }
                    });
        }

        Log.d(TAG, "Finished initiating mood queries for all users");
    }

    // Updated helper method to get the distance in km for logging
    private float getDistanceInKm(Double latitude, Double longitude) {
        if (currentLocation == null || latitude == null || longitude == null) return -1;

        Location eventLoc = new Location("");
        eventLoc.setLatitude(latitude);
        eventLoc.setLongitude(longitude);

        return currentLocation.distanceTo(eventLoc) / 1000; // Convert to km
    }

    // Updated isWithinDistance method to use latitude/longitude
    private boolean isWithinDistance(Double latitude, Double longitude) {
        if (currentLocation == null || latitude == null || longitude == null) {
            Log.d(TAG, "Current location or coordinates are null, can't calculate distance");
            return false;
        }

        Location eventLoc = new Location("");
        eventLoc.setLatitude(latitude);
        eventLoc.setLongitude(longitude);

        float distance = currentLocation.distanceTo(eventLoc) / 1000; // Convert to km
        Log.d(TAG, "Distance to event: " + distance + " km (max: " + MAX_DISTANCE_KM + " km)");
        return distance <= MAX_DISTANCE_KM;
    }

    private void addCurrentLocationMarker() {
        Log.d(TAG, "Adding current location marker");
        GeoPoint currentGeoPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        List<OverlayItem> items = new ArrayList<>();
        OverlayItem currentLocationItem = new OverlayItem("You", "Your location", currentGeoPoint);

        Drawable marker = ContextCompat.getDrawable(this, R.drawable.ic_play_circle);
        if (marker == null) {
            Log.e(TAG, "Current location marker drawable is null (R.drawable.ic_play_circle)");
            return;
        }

        currentLocationItem.setMarker(marker);
        items.add(currentLocationItem);

        ItemizedOverlayWithFocus<OverlayItem> overlay = new ItemizedOverlayWithFocus<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, this);
        overlay.setFocusItemsOnTap(true);
        mapView.getOverlays().add(overlay);
        Log.d(TAG, "Current location marker added successfully");
    }

    private void addMoodMarker(org.osmdroid.util.GeoPoint location, String emoji, String userId) {
        Log.d(TAG, "Adding mood marker for emoji: " + emoji + " at location: " +
                location.getLatitude() + ", " + location.getLongitude());

        Drawable emojiDrawable = getEmojiDrawable(emoji);
        if (emojiDrawable == null) {
            Log.e(TAG, "Emoji drawable not found for: " + emoji);
            return;
        }
        Log.d(TAG, "Got emoji drawable for: " + emoji);

        Bitmap markerBitmap = drawableToBitmap(emojiDrawable);
        if (markerBitmap == null) {
            Log.e(TAG, "Failed to convert drawable to bitmap for emoji: " + emoji);
            return;
        }
        Log.d(TAG, "Converted emoji drawable to bitmap");

        List<OverlayItem> items = new ArrayList<>();
        OverlayItem moodItem = new OverlayItem("Mood Event", "User's mood", location);
        moodItem.setMarker(new android.graphics.drawable.BitmapDrawable(getResources(), markerBitmap));
        items.add(moodItem);
        Log.d(TAG, "Created overlay item for mood marker");

        ItemizedOverlayWithFocus<OverlayItem> overlay = new ItemizedOverlayWithFocus<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        // Show more info about this mood event
                        Toast.makeText(MapsFollowingActivity.this,
                                "Mood by user: " + userId, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "User tapped on mood marker for user: " + userId);
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, this);
        overlay.setFocusItemsOnTap(true);
        mapView.getOverlays().add(overlay);
        Log.d(TAG, "Added mood marker overlay to map");
        mapView.invalidate();
    }

    private Drawable getEmojiDrawable(String emojiName) {
        if (emojiName == null) {
            Log.e(TAG, "Emoji name is null");
            return null;
        }

        Log.d(TAG, "Looking for emoji drawable resource with name: " + emojiName);
        int resId = getResources().getIdentifier(emojiName, "drawable", getPackageName());

        if (resId != 0) {
            Log.d(TAG, "Found emoji drawable resource ID: " + resId);
            Drawable drawable = ContextCompat.getDrawable(this, resId);
            if (drawable == null) {
                Log.e(TAG, "Got null drawable from resource ID: " + resId);
            }
            return drawable;
        } else {
            Log.e(TAG, "Resource not found for emoji name: " + emojiName);
            return null;
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) {
            Log.e(TAG, "Cannot convert null drawable to bitmap");
            return null;
        }

        try {
            int width = 60;
            int height = 60;
            Log.d(TAG, "Creating bitmap of size: " + width + "x" + height);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            Log.d(TAG, "Successfully converted drawable to bitmap");
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Error converting drawable to bitmap", e);
            return null;
        }
    }

    private Location getCurrentLocation() {
        Log.d(TAG, "Getting current location");
        // implement location retrieval using FusedLocationProviderClient
        Location defaultLocation = new Location("");
        defaultLocation.setLatitude(37.4227);
        defaultLocation.setLongitude(-122.0807); // Mountain View, CA
        Log.d(TAG, "Using default location: " + defaultLocation.getLatitude() +
                ", " + defaultLocation.getLongitude());
        return defaultLocation;
    }
}