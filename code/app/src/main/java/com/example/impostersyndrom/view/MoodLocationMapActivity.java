package com.example.impostersyndrom.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.impostersyndrom.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.List;
/**
 * An Activity that displays a map with a marker showing the location where a mood was recorded.
 * Uses OpenStreetMap (osmdroid) to render the map and places a custom emoji marker at the specified coordinates.
 * Handles map initialization, zoom controls, and marker placement with emoji icons.
 *
 * @author Roshan
 */
public class MoodLocationMapActivity extends AppCompatActivity {

    private static final String TAG = "MoodLocationMapActivity";
    private ImageButton backButton;
    private MapView mapView;
    private Double latitude;
    private Double longitude;
    private String emoji;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Initialize osmdroid configuration
        Configuration.getInstance().load(getApplicationContext(), getSharedPreferences("osmdroid", MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Retrieve intent data
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);
        emoji = getIntent().getStringExtra("emoji");

        Log.d(TAG, "Received: lat=" + latitude + ", lon=" + longitude + ", emoji=" + emoji);

        // Initialize views
        mapView = findViewById(R.id.mapView);
        backButton = findViewById(R.id.backButton);
        // Set up the map
        setupMap();
        backButton.setOnClickListener(v -> finish());
        // Add the mood marker
        if (latitude != 0.0 && longitude != 0.0 && emoji != null) {
            addMoodMarker();
        } else {
            Log.e(TAG, "Invalid location or emoji data: lat=" + latitude + ", lon=" + longitude + ", emoji=" + emoji);
        }

    }

    private void setupMap() {
        // Set the tile source (OpenStreetMap)
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        // Enable zoom controls and multi-touch
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // Set initial zoom level and center
        mapView.getController().setZoom(15.0); // Adjust zoom level to match the mockup
        if (latitude != 0.0 && longitude != 0.0) {
            GeoPoint moodLocation = new GeoPoint(latitude, longitude);
            mapView.getController().setCenter(moodLocation);
        } else {
            // Default to a fallback location if needed (e.g., a city center)
            GeoPoint defaultLocation = new GeoPoint(49.2827, -123.1207); // Vancouver, BC as a fallback
            mapView.getController().setCenter(defaultLocation);
        }
    }

    private void addMoodMarker() {
        // Create a GeoPoint for the mood's location
        GeoPoint moodLocation = new GeoPoint(latitude, longitude);

        // Convert the emoji drawable to a marker icon
        Drawable emojiDrawable = getEmojiDrawable(emoji);
        if (emojiDrawable == null) {
            Log.e(TAG, "Emoji drawable not found for: " + emoji);
            return;
        }

        // Convert Drawable to Bitmap for osmdroid marker
        Bitmap markerBitmap = drawableToBitmap(emojiDrawable);

        // Create an OverlayItem for the mood
        List<OverlayItem> items = new ArrayList<>();
        OverlayItem moodItem = new OverlayItem("Mood Location", "Mood at this location", moodLocation);
        moodItem.setMarker(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_myplaces)); // Default marker
        moodItem.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);
        items.add(moodItem);

        // Create an overlay to display the marker
        ItemizedOverlayWithFocus<OverlayItem> overlay = new ItemizedOverlayWithFocus<>(items,
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                    @Override
                    public boolean onItemSingleTapUp(int index, OverlayItem item) {
                        // Handle marker tap if needed (e.g., show a toast or dialog)
                        return true;
                    }

                    @Override
                    public boolean onItemLongPress(int index, OverlayItem item) {
                        return false;
                    }
                }, this);
        overlay.setFocusItemsOnTap(true);

        // Add the overlay to the map
        mapView.getOverlays().add(overlay);

        // Set the custom marker icon (emoji)
        moodItem.setMarker(new android.graphics.drawable.BitmapDrawable(getResources(), markerBitmap));
        mapView.invalidate(); // Refresh the map to show the marker
    }

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