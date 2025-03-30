package com.example.impostersyndrom.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.Mood;
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
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = "MapActivity";
    private MapView mapView;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MyLocationNewOverlay myLocationOverlay;
    private ImageButton filterButton;
    private Button myMoodsButton;
    private Button followingMoodsButton;
    private boolean showingMyMoods = true;

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
        myMoodsButton = findViewById(R.id.my_moods_button);
        followingMoodsButton = findViewById(R.id.following_moods_button);

        // Initialize and configure map
        initializeMap();

        // Set button listeners
        myMoodsButton.setOnClickListener(v -> showMyMoods());
        followingMoodsButton.setOnClickListener(v -> showFollowingMoods());
        filterButton.setOnClickListener(v -> showFilterDialog());

        // Load initial view (My Moods)
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
        myMoodsButton.setEnabled(false);
        followingMoodsButton.setEnabled(true);

        // Clear existing markers
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);

        loadMoods(true);
    }

    private void showFollowingMoods() {
        showingMyMoods = false;
        myMoodsButton.setEnabled(true);
        followingMoodsButton.setEnabled(false);

        // Clear existing markers
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);

        loadMoods(false);
    }

    private void loadMoods(boolean loadMyMoodsOnly) {
        String currentUserId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Loading moods for user: " + currentUserId + ", MyMoodsOnly: " + loadMyMoodsOnly);

        if (loadMyMoodsOnly) {
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
                            Log.d(TAG, "Adding my mood: " + mood.getEmotionalState() + " at " + mood.getLatitude() + "," + mood.getLongitude());
                            addMoodMarker(mood, true);
                        }
                        mapView.invalidate();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading my moods: " + e.getMessage());
                        Toast.makeText(this, "Error loading your moods", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Load moods from followed users (matching original code)
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
                            } else {
                                Log.w(TAG, "Document " + doc.getId() + " has no followingId field");
                            }
                        }

                        if (followingIds.isEmpty()) {
                            Log.d(TAG, "No valid following IDs found");
                            Toast.makeText(this, "No followed users found", Toast.LENGTH_SHORT).show();
                            mapView.invalidate();
                            return;
                        }

                        Log.d(TAG, "Total following IDs found: " + followingIds.size());
                        int[] processedUsers = {0};  // Counter for processed users
                        for (String followedUserId : followingIds) {
                            Log.d(TAG, "Loading moods for followed user: " + followedUserId);
                            db.collection("moods")
                                    .whereEqualTo("userId", followedUserId)
                                    .get()  // Note: Removed isPublic filter to match original code's loadUserMoods
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " moods for user " + followedUserId);
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            Boolean isPublic = document.getBoolean("isPublic");
                                            Boolean privateMood = document.getBoolean("privateMood");
                                            if (Boolean.TRUE.equals(privateMood)) {
                                                Log.d(TAG, "Skipping private mood: " + document.getId());
                                                continue;  // Skip private moods as in original code
                                            }
                                            Mood mood = document.toObject(Mood.class);
                                            Log.d(TAG, "Adding following mood: " + mood.getEmotionalState() + " at " +
                                                    mood.getLatitude() + "," + mood.getLongitude() + " by " + mood.getUserId());
                                            addMoodMarker(mood, false);
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
        // TODO: Implement filter dialog if needed
        Toast.makeText(this, "Filter functionality to be implemented", Toast.LENGTH_SHORT).show();
    }

    private void refreshMapWithFilters() {
        if (showingMyMoods) {
            showMyMoods();
        } else {
            showFollowingMoods();
        }
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