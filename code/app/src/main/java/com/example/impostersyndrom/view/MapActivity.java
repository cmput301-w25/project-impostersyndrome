package com.example.impostersyndrom.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
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
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class MapActivity extends AppCompatActivity {
    private static final String TAG = "MapActivity";
    private static final double MAX_DISTANCE_KM = 5.0;

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
    private boolean filterLocalMoods = false;
    private Location currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_mood_location_map);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        mapView = findViewById(R.id.mapView);
        filterButton = findViewById(R.id.filterButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        initializeMap();

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_my_moods) {
                showMyMoods();
                return true;
            } else if (itemId == R.id.nav_following_moods) {
                showFollowingMoods();
                return true;
            } else if (itemId == R.id.nav_local_moods) {
                showLocalMoods();
                return true;
            }
            return false;
        });

        filterButton.setOnClickListener(v -> showFilterDialog());

        bottomNavigationView.setSelectedItemId(R.id.nav_my_moods);
        bottomNavigationView.setItemIconTintList(null);
        bottomNavigationView.setItemTextColor(null);
        showMyMoods();
    }

    private void initializeMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(12.0);
        mapView.getController().setCenter(new GeoPoint(53.5461, -113.4937));

        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            if (myLocationOverlay.getMyLocation() != null) {
                currentLocation = new Location("");
                currentLocation.setLatitude(myLocationOverlay.getMyLocation().getLatitude());
                currentLocation.setLongitude(myLocationOverlay.getMyLocation().getLongitude());
                mapView.getController().setCenter(myLocationOverlay.getMyLocation());
                Log.d(TAG, "Location updated: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
            } else {
                Log.w(TAG, "MyLocationNewOverlay returned null location");
                currentLocation = getCurrentLocation();
            }
        }));
        mapView.getOverlays().add(myLocationOverlay);

        if (currentLocation == null) {
            currentLocation = getCurrentLocation();
            Log.d(TAG, "Using default location: " + currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
        }
    }

    private void showMyMoods() {
        showingMyMoods = true;
        filterFollowing = false;
        filterLocalMoods = false;
        refreshMapWithFilters();
    }

    private void showFollowingMoods() {
        showingMyMoods = false;
        filterFollowing = true;
        filterLocalMoods = false;
        refreshMapWithFilters();
    }

    private void showLocalMoods() {
        showingMyMoods = false;
        filterFollowing = false;
        filterLocalMoods = true;
        refreshMapWithFilters();
    }

    private void loadMoods() {
        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated");
            Toast.makeText(this, "Please log in to view moods", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = auth.getCurrentUser().getUid();
        Log.d(TAG, "Loading moods for user: " + currentUserId + ", filterFollowing: " + filterFollowing + ", filterLocalMoods: " + filterLocalMoods);

        if (filterLocalMoods) {
            loadLocalMoods();
            return;
        }

        if (!filterFollowing) {
            db.collection("moods")
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            Toast.makeText(this, "No moods found for you", Toast.LENGTH_SHORT).show();
                        }
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Mood mood = document.toObject(Mood.class);
                            if (shouldDisplayMood(mood)) {
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
            db.collection("following")
                    .whereEqualTo("followerId", currentUserId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            Toast.makeText(this, "You are not following any users", Toast.LENGTH_SHORT).show();
                            mapView.invalidate();
                            return;
                        }

                        List<String> followingIds = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String followingId = doc.getString("followingId");
                            if (followingId != null) {
                                followingIds.add(followingId);
                            }
                        }

                        if (followingIds.isEmpty()) {
                            Toast.makeText(this, "No followed users found", Toast.LENGTH_SHORT).show();
                            mapView.invalidate();
                            return;
                        }

                        int[] processedUsers = {0};
                        for (String followedUserId : followingIds) {
                            db.collection("moods")
                                    .whereEqualTo("userId", followedUserId)
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            Boolean privateMood = document.getBoolean("privateMood");
                                            if (Boolean.TRUE.equals(privateMood)) continue;
                                            Mood mood = document.toObject(Mood.class);
                                            if (shouldDisplayMood(mood)) {
                                                addMoodMarker(mood, false);
                                            }
                                        }
                                        processedUsers[0]++;
                                        if (processedUsers[0] == followingIds.size()) {
                                            mapView.invalidate();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        processedUsers[0]++;
                                        if (processedUsers[0] == followingIds.size()) {
                                            mapView.invalidate();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error loading followed users", Toast.LENGTH_SHORT).show();
                        mapView.invalidate();
                    });
        }
    }

    private void loadLocalMoods() {
        if (currentLocation == null) {
            Toast.makeText(this, "Unable to determine current location", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "Current location is null");
            return;
        }

        if (auth.getCurrentUser() == null) {
            Log.e(TAG, "User not authenticated in loadLocalMoods");
            Toast.makeText(this, "Please log in to view local moods", Toast.LENGTH_SHORT).show();
            return;
        }

        GeoPoint center = new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
        mapView.getController().setZoom(13.0);
        mapView.getController().setCenter(center);
        mapView.getOverlays().clear();
        mapView.getOverlays().add(myLocationOverlay);
        addRadiusOverlay(center, MAX_DISTANCE_KM);

        db.collection("moods")
                .whereNotEqualTo("latitude", null)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Local moods query successful. Found " + task.getResult().size() + " documents");
                        if (task.getResult().isEmpty()) {
                            Toast.makeText(this, "No local moods found", Toast.LENGTH_SHORT).show();
                            mapView.invalidate();
                            return;
                        }

                        int displayedMoods = 0;
                        List<Mood> nearbyMoods = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Boolean privateMood = document.getBoolean("privateMood");
                            if (Boolean.TRUE.equals(privateMood)) {
                                Log.d(TAG, "Skipping private mood: " + document.getId());
                                continue;
                            }

                            Double latitude = document.getDouble("latitude");
                            Double longitude = document.getDouble("longitude");
                            if (latitude == null || longitude == null) {
                                Log.d(TAG, "Skipping mood with null coordinates: " + document.getId());
                                continue;
                            }

                            Mood mood;
                            try {
                                mood = document.toObject(Mood.class);
                                if (mood == null) {
                                    Log.w(TAG, "Converted mood is null for document: " + document.getId());
                                    continue;
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to convert document to Mood: " + document.getId(), e);
                                continue;
                            }

                            if (mood.getTimestamp() == null) {
                                Log.w(TAG, "Mood has null timestamp: " + document.getId());
                                continue;
                            }

                            if (mood.getUserId() == null) {
                                Log.w(TAG, "Mood has null userId: " + document.getId());
                                continue;
                            }

                            if (isWithinDistance(latitude, longitude) && shouldDisplayMood(mood)) {
                                nearbyMoods.add(mood);
                                displayedMoods++;
                                Log.d(TAG, "Added mood within 5km: " + document.getId());
                            }
                        }

                        Collections.sort(nearbyMoods, (m1, m2) -> {
                            if (m1.getTimestamp() == null || m2.getTimestamp() == null) return 0;
                            return m2.getTimestamp().compareTo(m1.getTimestamp());
                        });

                        for (Mood mood : nearbyMoods) {
                            addMoodMarker(mood, mood.getUserId().equals(auth.getCurrentUser().getUid()));
                        }

                        Toast.makeText(this, "Displayed " + displayedMoods + " local moods within 5km", Toast.LENGTH_SHORT).show();
                        mapView.invalidate();
                    } else {
                        Log.e(TAG, "Failed to load local moods", task.getException());
                        Toast.makeText(this, "Error loading local moods: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                        mapView.invalidate();
                    }
                });
    }

    private boolean isWithinDistance(Double latitude, Double longitude) {
        if (currentLocation == null || latitude == null || longitude == null) {
            Log.d(TAG, "Cannot calculate distance: currentLocation or coordinates are null");
            return false;
        }

        Location eventLoc = new Location("");
        eventLoc.setLatitude(latitude);
        eventLoc.setLongitude(longitude);

        float distance;
        try {
            distance = currentLocation.distanceTo(eventLoc) / 1000; // Convert to km
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance: " + e.getMessage(), e);
            return false;
        }
        Log.d(TAG, "Distance to mood: " + distance + " km");
        return distance <= MAX_DISTANCE_KM;
    }

    private void addRadiusOverlay(GeoPoint center, double radiusKm) {
        if (center == null) {
            Log.e(TAG, "Cannot add radius overlay: center is null");
            return;
        }
        Polygon circle = new Polygon();
        List<GeoPoint> circlePoints = new ArrayList<>();

        double earthRadius = 6371;
        double angularDistance = radiusKm / earthRadius;
        double centerLatRad = Math.toRadians(center.getLatitude());
        double centerLonRad = Math.toRadians(center.getLongitude());

        for (int i = 0; i <= 100; i++) {
            double bearing = Math.toRadians(i * 3.6);
            double lat = Math.asin(Math.sin(centerLatRad) * Math.cos(angularDistance) +
                    Math.cos(centerLatRad) * Math.sin(angularDistance) * Math.cos(bearing));
            double lon = centerLonRad + Math.atan2(
                    Math.sin(bearing) * Math.sin(angularDistance) * Math.cos(centerLatRad),
                    Math.cos(angularDistance) - Math.sin(centerLatRad) * Math.sin(lat));
            circlePoints.add(new GeoPoint(Math.toDegrees(lat), Math.toDegrees(lon)));
        }

        circle.setPoints(circlePoints);
        circle.setFillColor(Color.argb(70, 0, 0, 255));
        circle.setStrokeColor(Color.argb(100, 0, 0, 255));
        circle.setStrokeWidth(2);
        mapView.getOverlays().add(circle);
    }

    private boolean shouldDisplayMood(Mood mood) {
        if (mood == null || mood.getLatitude() == null || mood.getLongitude() == null) {
            Log.w(TAG, "Mood is null or has invalid location data");
            return false;
        }

        if (filterLastWeek) {
            Calendar lastWeek = Calendar.getInstance();
            lastWeek.add(Calendar.WEEK_OF_YEAR, -1);
            if (mood.getTimestamp() == null || mood.getTimestamp().before(lastWeek.getTime())) {
                return false;
            }
        }

        if (!selectedMoodFilter.equals("All")) {
            if (mood.getEmotionalState() == null || !mood.getEmotionalState().toLowerCase().equals(selectedMoodFilter.toLowerCase())) {
                return false;
            }
        }

        if (!keywordFilter.isEmpty()) {
            String lowerKeyword = keywordFilter.toLowerCase();
            String lowerReason = mood.getReason() != null ? mood.getReason().toLowerCase() : "";
            String lowerEmotionalState = mood.getEmotionalState() != null ? mood.getEmotionalState().toLowerCase() : "";
            return lowerReason.contains(lowerKeyword) || lowerEmotionalState.contains(lowerKeyword);
        }

        return true;
    }

    private void addMoodMarker(Mood mood, boolean isOwnMood) {
        if (mood == null || mood.getLatitude() == null || mood.getLongitude() == null) {
            Log.w(TAG, "Cannot add marker for null mood or invalid location");
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
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting marker icon: " + e.getMessage());
        }

        marker.setTitle(mood.getEmotionalState() != null ? mood.getEmotionalState() + " - " : " - " +
                (mood.getReason() != null ? mood.getReason() : ""));
        marker.setSnippet(isOwnMood ? "Your mood" : "Mood by " + (mood.getUserId() != null ? mood.getUserId() : "Unknown"));

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

        CheckBox lastWeekCheckbox = filterView.findViewById(R.id.lastWeekCheckbox);
        Spinner moodSpinner = filterView.findViewById(R.id.moodSpinner);
        EditText keywordSearch = filterView.findViewById(R.id.keywordSearch);

        ArrayList<String> moodOptions = new ArrayList<>();
        moodOptions.add("All");
        moodOptions.addAll(Arrays.asList("Happy", "Sad", "Angry", "Fear",
                "Confused", "Shame", "Surprised", "Disgust"));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, moodOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        moodSpinner.setAdapter(adapter);

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
            filterLocalMoods = false;
            selectedMoodFilter = "All";
            keywordFilter = "";
            showMyMoods();
        });

        builder.show();
    }

    private void refreshMapWithFilters() {
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

    private Location getCurrentLocation() {
        Location defaultLocation = new Location("");
        defaultLocation.setLatitude(53.5461);
        defaultLocation.setLongitude(-113.4937);
        return defaultLocation;
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