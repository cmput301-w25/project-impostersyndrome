package com.example.impostersyndrom.view;

import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.NetworkUtils;
import com.example.impostersyndrom.model.ImageHandler;
import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.model.User;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMoodActivity extends AppCompatActivity {
    private static final int REQUEST_CHECK_SETTINGS = 2001;
    private MoodDataManager moodDataManager; // Handles Firestore operations
    private ImageHandler imageHandler; // Handles image selection and uploading
    private String selectedGroup; // Stores the selected group for the mood
    private String imageUrl = null; // URL of the uploaded image

    // ActivityResultLaunchers for gallery and camera
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;

    private FusedLocationProviderClient fusedLocationClient; // For fetching location
    private ActivityResultLauncher<String[]> locationPermissionLauncher; // For location permission request
    private Location currentLocation; // Stores the current location
    private boolean isLocationAttached = false; // Tracks if location is attached

    private boolean isPrivateMood = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood);

        // Initialize Firestore and moods collection reference
        moodDataManager = new MoodDataManager();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null &&
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // Permission granted, fetch location
                        fetchLocation();
                    } else {
                        Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        // Initialize views
        ImageView emojiView = findViewById(R.id.emojiView);
        TextView emojiDescription = findViewById(R.id.emojiDescription);
        TextView timeView = findViewById(R.id.dateTimeView);
        LinearLayout emojiRectangle = findViewById(R.id.emojiRectangle);
        EditText addReasonEdit = findViewById(R.id.addReasonEdit);
        TextView reasonCharCount = findViewById(R.id.reasonCharCount);
        ImageButton submitButton = findViewById(R.id.submitButton);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton groupButton = findViewById(R.id.groupButton);
        ImageButton cameraMenuButton = findViewById(R.id.cameraMenuButton);
        ImageView imagePreview = findViewById(R.id.imagePreview);

        ImageButton attachLocationButton = findViewById(R.id.attachLocationButton); // Replace with your button's ID
        attachLocationButton.setOnClickListener(v -> {
            Log.d("AddMoodActivity", "Attach Location button clicked");
            showLocationPrompt();
        });

        SwitchMaterial privacySwitch = findViewById(R.id.privacySwitch);


        // Initialize image handling
        imageHandler = new ImageHandler(this, imagePreview);

        // Set default social situation to "Alone"
        selectedGroup = "Alone"; // Default value

        // Initialize ActivityResultLaunchers
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> imageHandler.handleActivityResult(result.getResultCode(), result.getData())
        );

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> imageHandler.handleActivityResult(result.getResultCode(), result.getData())
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, launch camera intent
                        imageHandler.openCamera(cameraLauncher);
                    } else {
                        Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, launch gallery intent
                        imageHandler.openGallery(galleryLauncher);
                    } else {
                        Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        privacySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isPrivateMood = isChecked;
            String status = isPrivateMood ? "Private" : "Public";
            Toast.makeText(this, "Mood set to " + status, Toast.LENGTH_SHORT).show();
        });

        addReasonEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                int chars = s.length();
                String text = s.toString().trim();
                reasonCharCount.setText(chars + "/200");
            }
        });

        // Set up the listener to show/hide image preview
        imageHandler.setOnImageLoadedListener(new ImageHandler.OnImageLoadedListener() {
            @Override
            public void onImageLoaded() {
                imagePreview.setVisibility(View.VISIBLE);
            }

            @Override
            public void onImageCleared() {
                imagePreview.setVisibility(View.GONE);
            }
        });

        // Retrieve the Mood object from the intent
        Intent intent = getIntent();
        Mood mood = (Mood) intent.getSerializableExtra("mood");
        intent.putExtra("userId", getIntent().getStringExtra("userId"));

        if (mood != null) {
            // Display the custom emoji using drawable resource ID
            emojiView.setImageResource(mood.getEmojiDrawableId());
            emojiDescription.setText(mood.getEmojiDescription());

            // Set the current time
            String currentTime = new SimpleDateFormat("dd-MM-YYYY | HH:mm", Locale.getDefault()).format(mood.getTimestamp());
            timeView.setText(currentTime);

            // Set the background color, rounded corners, and border for the rectangle
            setRoundedBackground(emojiRectangle, mood.getColor());
        }

        // Group button functionality
        groupButton.setOnClickListener(v -> showGroupsMenu(v));

        // Setup camera menu button to show options
        cameraMenuButton.setOnClickListener(v -> showImageMenu(v));

        // Back button functionality
        backButton.setOnClickListener(v -> finish());

        // Submit button with image handling
        submitButton.setOnClickListener(v -> {
            // Ensure the mood object is properly initialized
            if (mood == null) {
                Toast.makeText(AddMoodActivity.this, "Mood object is null", Toast.LENGTH_SHORT).show();
                return;
            }

            // Update the mood object with the new values
            mood.setReason(addReasonEdit.getText().toString().trim());
            mood.setGroup(selectedGroup);
            mood.setUserId(User.getInstance().getUserId());
            mood.setPrivateMood(isPrivateMood);

            // If location is attached, update the mood
            if (isLocationAttached && currentLocation != null) {
                mood.setLatitude(currentLocation.getLatitude());
                mood.setLongitude(currentLocation.getLongitude());
            } else {
                mood.setLatitude(null);
                mood.setLongitude(null);
            }

            // Log the mood details for debugging
            Log.d("AddMoodActivity", "Mood details: " + mood.toString());

            // Check connectivity using your NetworkUtils
            if (NetworkUtils.isOffline(AddMoodActivity.this)) {
                // Offline branch: Save mood locally

                // If an image is selected, save it locally and update the mood's imageUrl
                if (imageHandler.hasImage()) {
                    String localImageUri = imageHandler.saveImageLocally();
                    if (localImageUri != null) {
                        mood.setImageUrl(localImageUri);
                        Log.d("OfflineImage", "Image saved locally: " + localImageUri);
                    } else {
                        Log.e("OfflineImage", "Failed to save image locally.");
                        mood.setImageUrl(null);
                    }
                } else {
                    mood.setImageUrl(null);
                }

                // Log and save the mood offline
                Log.d("OfflineMood", "Saving offline mood: " + mood.toString());
                moodDataManager.saveMoodOffline(AddMoodActivity.this, mood);
                Log.d("MoodSubmit", "Mood saved offline: " + mood.getReason());
                navigateToMainActivity();
            } else {
                // Online branch: Sync directly with Firestore
                if (imageHandler.hasImage()) {
                    imageHandler.uploadImageToFirebase(new ImageHandler.OnImageUploadListener() {
                        @Override
                        public void onImageUploadSuccess(String url) {
                            imageUrl = url;
                            mood.setImageUrl(imageUrl);
                            saveMood(mood);
                        }

                        @Override
                        public void onImageUploadFailure(Exception e) {
                            Toast.makeText(AddMoodActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    mood.setImageUrl(null);
                    saveMood(mood);
                }
            }
        });

    }
    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                            isLocationAttached = true;
                            Toast.makeText(this, "Location attached: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Toast.makeText(this, "Failed to fetch location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, response -> {
            // Location settings are satisfied, fetch location
            fetchLocation();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, prompt the user to enable them
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    Toast.makeText(this, "Failed to enable location settings", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Location settings check failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Saves the mood to Firestore.
     *
     * @param mood The Mood object to be saved.
     */
    private void saveMood(Mood mood) {
        moodDataManager.addMood(mood, new MoodDataManager.OnMoodAddedListener() {
            @Override
            public void onMoodAdded() {
                Toast.makeText(AddMoodActivity.this, "Mood saved!", Toast.LENGTH_SHORT).show();
                Log.d("AddMoodActivity", "Mood saved to Firestore");
                navigateToMainActivity();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AddMoodActivity.this, "Failed to save mood: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("AddMoodActivity", "Error saving mood: " + errorMessage);
            }
        });
    }

    /**
     * Navigates to the MainActivity and clears the back stack.
     */
    private void navigateToMainActivity() {
        Intent newIntent = new Intent(AddMoodActivity.this, MainActivity.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(newIntent);
        finish();
    }

    /**
     * Sets a rounded background with dynamic color for a LinearLayout.
     *
     * @param layout The LinearLayout to apply the background to.
     * @param color  The color to set as the background.
     */
    private void setRoundedBackground(LinearLayout layout, int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50); // Rounded corners (50dp radius)
        gradientDrawable.setColor(color); // Set the background color
        gradientDrawable.setStroke(2, Color.BLACK); // Set the border (2dp width, black color)

        // Set the GradientDrawable as the background
        layout.setBackground(gradientDrawable);
    }

    /**
     * Displays a popup menu for selecting a group.
     *
     * @param v The view to anchor the popup menu.
     */
    private void showGroupsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.group_menu, popup.getMenu());
        Map<Integer, String> menuMap = new HashMap<>(); // Hashmap maps each menu id to each response
        menuMap.put(R.id.alone, "Alone");
        menuMap.put(R.id.with_another, "With another person");
        menuMap.put(R.id.with_several, "With several people");
        menuMap.put(R.id.with_crowd, "With a crowd");

        popup.setOnMenuItemClickListener(item -> {
            if (menuMap.containsKey(item.getItemId())) {
                selectedGroup = menuMap.get(item.getItemId());
                Toast.makeText(AddMoodActivity.this, "Group Status Saved!", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        popup.show();
    }

    /**
     * Displays a popup menu for image options (camera, gallery, remove photo).
     *
     * @param v The view to anchor the popup menu.
     */
    private void showImageMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Take a Photo");
        popup.getMenu().add("Choose from Gallery");
        popup.getMenu().add("Remove Photo");  // Add option to remove photo

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Take a Photo")) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    // Permission already granted, launch camera
                    imageHandler.openCamera(cameraLauncher);
                } else {
                    // Request camera permission
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                }
                return true;
            } else if (item.getTitle().equals("Choose from Gallery")) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    // Permission already granted, launch gallery
                    imageHandler.openGallery(galleryLauncher);
                } else {
                    // Request gallery permission
                    galleryPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
                }
                return true;
            } else if (item.getTitle().equals("Remove Photo")) {
                imageHandler.clearImage();  // Clear the current image
                return true;
            }
            return false;
        });

        popup.show();

    }
    private void showLocationPrompt() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Attach Location");
        builder.setMessage("Would you like to attach your current location to this mood event?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            // Check location permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // Permission already granted, fetch location
                fetchLocation();
            } else {
                // Request location permissions
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            // User chose not to attach location
            isLocationAttached = false;
            currentLocation = null;
            Toast.makeText(this, "Location not attached", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

}