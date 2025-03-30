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

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
        requestPermissions();

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
        moodSpinner.setSelection(moodOptions.indexOf(selectedMoodFilter.toLowerCase()));
        keywordSearch.setText(keywordFilter);

        builder.setPositiveButton("Apply", (dialog, which) -> {
            // Save filter values
            filterLastWeek = lastWeekCheckbox.isChecked();
            selectedMoodFilter = moodSpinner.getSelectedItem().toString().toLowerCase();
            keywordFilter = keywordSearch.getText().toString().trim();

            // Refresh map with new filters
            refreshMapWithFilters();
        });

        builder.setNegativeButton("Cancel", null);

        builder.setNeutralButton("Clear Filters", (dialog, which) -> {
            filterLastWeek = false;
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
        if (mood.getLatitude() == null || mood.getLongitude() == null) {
            return false;
        }

        // Filter by last week
        if (filterLastWeek) {
            Calendar lastWeek = Calendar.getInstance();
            lastWeek.add(Calendar.WEEK_OF_YEAR, -1);
            if (mood.getTimestamp().before(lastWeek.getTime())) {
                return false;
            }
        }

        // Filter by mood (case-insensitive comparison)
        if (!selectedMoodFilter.equals("All") &&
            !mood.getEmotionalState().toLowerCase().equals(selectedMoodFilter.toLowerCase())) {
            return false;
        }

        // Filter by keyword
        if (!keywordFilter.isEmpty()) {
            String lowerKeyword = keywordFilter.toLowerCase();
            String lowerReason = mood.getReason().toLowerCase();
            String lowerEmotionalState = mood.getEmotionalState().toLowerCase();
            
            boolean matchesKeyword = lowerReason.contains(lowerKeyword) || 
                                   lowerEmotionalState.contains(lowerKeyword);
            
            if (!matchesKeyword) {
                return false;
            }
        }

        return true;
    }

    private void loadMoods() {
        String currentUserId = auth.getCurrentUser().getUid();

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
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error loading moods: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });

        // Load following users' moods
        db.collection("users")
            .document(currentUserId)
            .collection("following")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    loadUserMoods(document.getId());
                }
            });
    }

    private void loadUserMoods(String userId) {
        db.collection("moods")
            .whereEqualTo("userId", userId)
            .whereEqualTo("privateMood", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Mood mood = document.toObject(Mood.class);
                    if (shouldDisplayMood(mood)) {
                        addMoodMarker(mood, false);
                    }
                }
            });
    }

    private void addMoodMarker(Mood mood, boolean isOwnMood) {
        Marker marker = new Marker(mapView);
        marker.setPosition(new GeoPoint(mood.getLatitude(), mood.getLongitude()));

        // Store the mood object in the marker for later retrieval
        marker.setRelatedObject(mood);

        // Set marker icon based on mood emoji drawable ID and resize it
        if (mood.getEmojiDrawableId() != 0) {
            Drawable originalDrawable = getResources().getDrawable(mood.getEmojiDrawableId());

            // Convert drawable to bitmap and resize it
            Bitmap originalBitmap = ((BitmapDrawable) originalDrawable).getBitmap();
            int newWidth = 20;  // Width in pixels
            int newHeight = 20; // Height in pixels
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);

            // Convert back to drawable
            Drawable resizedDrawable = new BitmapDrawable(getResources(), resizedBitmap);
            marker.setIcon(resizedDrawable);
        }

        // Set marker title with emotional state and reason
        String title = mood.getEmotionalState() + " - " + mood.getReason();
        marker.setTitle(title);

        // Set marker snippet with timestamp and group context
        String snippet = isOwnMood ?
            "Your mood" + " (" + mood.getGroup() + ")" :
            "Mood by " + mood.getUserId() + " (" + mood.getGroup() + ")";
        marker.setSnippet(snippet);

        mapView.getOverlays().add(marker);
    }

    private void openMoodDetail(Mood mood) {
        Intent intent = new Intent(this, MoodDetailActivity.class);
        intent.putExtra("MOOD_ID", mood.getId());
        intent.putExtra("USER_ID", mood.getUserId());
        startActivity(intent);
    }
}
