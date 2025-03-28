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
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        progressBar = findViewById(R.id.progressBar);
        mapView = findViewById(R.id.mapView);
        backButton = findViewById(R.id.backButton);
        refreshButton = findViewById(R.id.refreshButton);

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
    }

    private void loadFollowedUsersMoodEvents() {
        // Query the 'following' collection where current user is the follower
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<String> followedUserIds = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Get the userId being followed (different from followerId)
                                String followedUserId = document.getString("userId");
                                if (followedUserId != null) {
                                    followedUserIds.add(followedUserId);
                                }
                            }

                            if (!followedUserIds.isEmpty()) {
                                // Now get the most recent mood event for each followed user
                                getRecentMoodEvents(followedUserIds);
                            } else {
                                Toast.makeText(MapsFollowingActivity.this,
                                        "You're not following anyone yet", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.w(TAG, "Error getting followed users", task.getException());
                            Toast.makeText(MapsFollowingActivity.this,
                                    "Failed to load followed users", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void getRecentMoodEvents(List<String> userIds) {
        // Clear existing markers
        mapView.getOverlays().clear();

        // Add current user's location marker if available
        if (currentLocation != null) {
            addCurrentLocationMarker();
        }

        // For each followed user, get their most recent mood event with location
        for (String userId : userIds) {
            db.collection("mood")
                    .whereEqualTo("userId", userId)
                    .whereNotEqualTo("location", null)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful() && !task.getResult().isEmpty()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Map<String, Object> moodEvent = document.getData();
                                    com.google.firebase.firestore.GeoPoint firebaseLocation =
                                            (com.google.firebase.firestore.GeoPoint) moodEvent.get("location");
                                    String emoji = (String) moodEvent.get("emoji");

                                    // Convert Firebase GeoPoint to osmdroid GeoPoint
                                    org.osmdroid.util.GeoPoint eventLocation =
                                            new org.osmdroid.util.GeoPoint(
                                                    firebaseLocation.getLatitude(),
                                                    firebaseLocation.getLongitude()
                                            );

                                    // Check if within 5km of current location
                                    if (isWithinDistance(firebaseLocation)) {
                                        addMoodMarker(eventLocation, emoji, userId);
                                    }
                                }
                            }
                        }
                    });
        }
    }
    private boolean isWithinDistance(com.google.firebase.firestore.GeoPoint eventLocation) {
        if (currentLocation == null) return false;

        Location eventLoc = new Location("");
        eventLoc.setLatitude(eventLocation.getLatitude());
        eventLoc.setLongitude(eventLocation.getLongitude());

        float distance = currentLocation.distanceTo(eventLoc) / 1000; // Convert to km
        return distance <= MAX_DISTANCE_KM;
    }

    private void addCurrentLocationMarker() {
        GeoPoint currentGeoPoint = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());

        List<OverlayItem> items = new ArrayList<>();
        OverlayItem currentLocationItem = new OverlayItem("You", "Your location", currentGeoPoint);
        currentLocationItem.setMarker(ContextCompat.getDrawable(this, R.drawable.ic_play_circle));
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
    }

    private void addMoodMarker(org.osmdroid.util.GeoPoint location, String emoji, String userId) {
        Drawable emojiDrawable = getEmojiDrawable(emoji);
        if (emojiDrawable == null) {
            Log.e(TAG, "Emoji drawable not found for: " + emoji);
            return;
        }

        Bitmap markerBitmap = drawableToBitmap(emojiDrawable);

        List<OverlayItem> items = new ArrayList<>();
        OverlayItem moodItem = new OverlayItem("Mood Event", "User's mood", location);
        moodItem.setMarker(new android.graphics.drawable.BitmapDrawable(getResources(), markerBitmap));
        items.add(moodItem);

        ItemizedOverlayWithFocus<OverlayItem> overlay = new ItemizedOverlayWithFocus<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        // Show more info about this mood event
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, this);
        overlay.setFocusItemsOnTap(true);
        mapView.getOverlays().add(overlay);
        mapView.invalidate();
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

    // Implement these methods similar to your MoodLocationMapActivity
    private Drawable getEmojiDrawable(String emojiName) {
        if (emojiName == null) return null;
        int resId = getResources().getIdentifier(emojiName, "drawable", getPackageName());
        if (resId != 0) {
            return ContextCompat.getDrawable(this, resId);
        }
        return null;
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable == null) return null;
        int width = 60;
        int height = 60;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    // You need to implement this to get the user's current location
    private Location getCurrentLocation() {
        // implement location retrieval using FusedLocationProviderClient

        return null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        if (refreshHandler != null) {
            refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshHandler != null) {
            refreshHandler.removeCallbacksAndMessages(null);
        }
    }
}