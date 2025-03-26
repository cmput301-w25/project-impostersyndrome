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
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.impostersyndrom.R;
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
                        Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
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
                Toast.makeText(this, "Mood object is null", Toast.LENGTH_SHORT).show();
                return;
            }

            mood.setReason(addReasonEdit.getText().toString().trim());
            mood.setGroup(selectedGroup);
            mood.setUserId(User.getInstance().getUserId());
            mood.setPrivateMood(isPrivateMood);

            if (isLocationAttached && currentLocation != null) {
                mood.setLatitude(currentLocation.getLatitude());
                mood.setLongitude(currentLocation.getLongitude());
                Log.d("AddMoodActivity", "Location set: lat=" + mood.getLatitude() + ", lon=" + mood.getLongitude());
            } else {
                mood.setLatitude(null);
                mood.setLongitude(null);
                Log.d("AddMoodActivity", "No location attached");
            }

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
                            Toast.makeText(this, "Location attached: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w("AddMoodActivity", "Location is null");
                            Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        Log.e("AddMoodActivity", "Failed to fetch location: " + e.getMessage());
                        Toast.makeText(this, "Failed to fetch location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof ResolvableApiException) {
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS && resultCode == RESULT_OK) {
            fetchLocation();
        }
    }

    private void saveMood(Mood mood) {
        moodDataManager.addMood(mood, new MoodDataManager.OnMoodAddedListener() {
            @Override
            public void onMoodAdded() {
                Toast.makeText(AddMoodActivity.this, "Mood saved!", Toast.LENGTH_SHORT).show();
                Log.d("AddMoodActivity", "Mood saved to Firestore: " + mood.toString());

                // Set the result to pass back to MainActivity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("moodId", mood.getId());
                setResult(RESULT_OK, resultIntent);

                // Start MainActivity and clear the activity stack
                Intent mainIntent = new Intent(AddMoodActivity.this, MainActivity.class);
                mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(mainIntent);

                // Finish AddMoodActivity (and EmojiSelectionActivity will be finished automatically)
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(AddMoodActivity.this, "Failed to save mood: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e("AddMoodActivity", "Error saving mood: " + errorMessage);
            }
        });
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
                Toast.makeText(this, "Group Status Saved!", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(this, "Location not attached", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }
}