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
import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresPermission;
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

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMoodActivity extends AppCompatActivity {
    private static final int REQUEST_CHECK_SETTINGS = 2001;
    private MoodDataManager moodDataManager;
    private ImageHandler imageHandler;
    private String selectedGroup;
    private String imageUrl = null;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;

    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;
    private Location currentLocation;
    private boolean isLocationAttached = false;

    private boolean isPrivateMood = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood);

        moodDataManager = new MoodDataManager();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null &&
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        fetchLocation();
                    } else {
                        showMessage("Location permission required");
                    }
                }
        );

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
        ImageButton attachLocationButton = findViewById(R.id.attachLocationButton);
        SwitchMaterial privacySwitch = findViewById(R.id.privacySwitch);

        imageHandler = new ImageHandler(this, imagePreview);

        selectedGroup = "Alone";

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
                        imageHandler.openCamera(cameraLauncher);
                    } else {
                        showMessage("Camera permission required");
                    }
                }
        );

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        imageHandler.openGallery(galleryLauncher);
                    } else {
                        showMessage("Storage permission required");
                    }
                }
        );

        privacySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isPrivateMood = isChecked;
            String status = isPrivateMood ? "Private" : "Public";
            showMessage("Mood set to " + status);
        });

        addReasonEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                int chars = s.length();
                reasonCharCount.setText(chars + "/200");
            }
        });

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

        Intent intent = getIntent();
        Mood mood = (Mood) intent.getSerializableExtra("mood");
        intent.putExtra("userId", getIntent().getStringExtra("userId"));

        if (mood != null) {
            emojiView.setImageResource(mood.getEmojiDrawableId());
            emojiDescription.setText(mood.getEmojiDescription());
            String currentTime = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault()).format(mood.getTimestamp());
            timeView.setText(currentTime);
            setRoundedBackground(emojiRectangle, mood.getColor());
        }

        groupButton.setOnClickListener(v -> showGroupsMenu(v));
        cameraMenuButton.setOnClickListener(v -> showImageMenu(v));
        backButton.setOnClickListener(v -> finish());
        attachLocationButton.setOnClickListener(v -> {
            Log.d("AddMoodActivity", "Attach Location button clicked");
            showLocationPrompt();
        });

        submitButton.setOnClickListener(v -> {
            if (mood == null) {
                showMessage("Mood object is null");
                return;
            }

            mood.setReason(addReasonEdit.getText().toString().trim());
            mood.setGroup(selectedGroup);
            mood.setUserId(User.getInstance().getUserId());
            mood.setPrivateMood(isPrivateMood);

            // If location is attached, update the mood
            if (isLocationAttached && currentLocation != null) {
                mood.setLatitude(currentLocation.getLatitude());
                mood.setLongitude(currentLocation.getLongitude());
                Log.d("AddMoodActivity", "Location set: lat=" + mood.getLatitude() + ", lon=" + mood.getLongitude());
            } else {
                mood.setLatitude(null);
                mood.setLongitude(null);
                Log.d("AddMoodActivity", "No location attached");
            }

            Log.d("AddMoodActivity", "Mood details: " + mood.toString());

            if (NetworkUtils.isOffline(AddMoodActivity.this)) {
                // Offline branch: Save mood locally
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
                            showMessage("Failed to upload image: " + e.getMessage());
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
            checkLocationSettings();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    @RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
    private void checkLocationSettings() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, response -> {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                            isLocationAttached = true;
                            Log.d("AddMoodActivity", "Location fetched: lat=" + location.getLatitude() + ", lon=" + location.getLongitude());
                            showMessage("Location attached: " + location.getLatitude() + ", " + location.getLongitude());
                        } else {
                            Log.w("AddMoodActivity", "Location is null");
                            showMessage("Unable to fetch location");
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e("AddMoodActivity", "Failed to fetch location: " + e.getMessage());
                        showMessage("Failed to fetch location: " + e.getMessage());
                    });
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
                try {
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    showMessage("Failed to enable location settings");
                }
            } else {
                showMessage("Location settings check failed");
            }
        });
    }

    /**
     * Displays a Snackbar message.
     *
     * @param message The message to display.
     */
    private void showMessage(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
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
                showMessage("Mood saved!");
                Log.d("AddMoodActivity", "Mood saved to Firestore");
                navigateToMainActivity();
            }

            @Override
            public void onError(String errorMessage) {
                showMessage("Failed to save mood: " + errorMessage);
                Log.e("AddMoodActivity", "Error saving mood: " + errorMessage);
            }
        });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToMoodDetailActivity(Mood mood) {
        Intent intent = new Intent(this, MoodDetailActivity.class);
        intent.putExtra("updatedMood", mood); // Pass the full Mood object
        intent.putExtra("emoji", mood.getEmotionalState());
        intent.putExtra("timestamp", new com.google.firebase.Timestamp(mood.getTimestamp()));
        intent.putExtra("reason", mood.getReason());
        intent.putExtra("group", mood.getGroup());
        intent.putExtra("color", mood.getColor());
        intent.putExtra("emojiDescription", mood.getEmojiDescription());
        intent.putExtra("imageUrl", mood.getImageUrl());
        intent.putExtra("latitude", mood.getLatitude());
        intent.putExtra("longitude", mood.getLongitude());
        intent.putExtra("isMyMoods", true); // Assuming this is needed
        startActivity(intent);
        finish();
    }

    private void setRoundedBackground(LinearLayout layout, int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(2, Color.BLACK);
        layout.setBackground(gradientDrawable);
    }

    private void showGroupsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.group_menu, popup.getMenu());
        Map<Integer, String> menuMap = new HashMap<>();
        menuMap.put(R.id.alone, "Alone");
        menuMap.put(R.id.with_another, "With another person");
        menuMap.put(R.id.with_several, "With several people");
        menuMap.put(R.id.with_crowd, "With a crowd");

        popup.setOnMenuItemClickListener(item -> {
            if (menuMap.containsKey(item.getItemId())) {
                selectedGroup = menuMap.get(item.getItemId());
                showMessage("Group Status Saved!");
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showImageMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Take a Photo");
        popup.getMenu().add("Choose from Gallery");
        popup.getMenu().add("Remove Photo");

        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Take a Photo")) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    imageHandler.openCamera(cameraLauncher);
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                }
                return true;
            } else if (item.getTitle().equals("Choose from Gallery")) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    imageHandler.openGallery(galleryLauncher);
                } else {
                    galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                }
                return true;
            } else if (item.getTitle().equals("Remove Photo")) {
                imageHandler.clearImage();
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                checkLocationSettings();
            } else {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            isLocationAttached = false;
            currentLocation = null;
            showMessage("Location not attached");
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == RESULT_OK) {
            fetchLocation();
        }
    }
}