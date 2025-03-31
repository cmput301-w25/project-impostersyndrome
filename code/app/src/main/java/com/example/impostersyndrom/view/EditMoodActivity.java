package com.example.impostersyndrom.view;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.MenuInflater;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import android.Manifest;
import android.widget.Toast;

import com.example.impostersyndrom.controller.EditEmojiResources;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.NetworkUtils;
import com.example.impostersyndrom.model.ImageHandler;
import com.example.impostersyndrom.model.MoodDataManager;

import com.example.impostersyndrom.model.Mood;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for editing an existing mood entry. Provides functionality to:
 * locally and synced when connectivity is restored. Manages all Firestore updates and
 * Firebase Storage operations for images.
 */
public class EditMoodActivity extends AppCompatActivity {
    /** Maximum allowed characters for mood reason text */
    private static final int MAX_REASON_LENGTH = 200;

    /** ID of the mood document being edited */
    private String moodId;

    /** Firestore database instance */
    private FirebaseFirestore db;

    /** Displays the current emoji description */
    private TextView editEmojiDescription;

    /** EditText for modifying the mood reason */
    private EditText editReason;

    /** Preview of the attached image */
    private ImageView editImagePreview;

    /** Navigation buttons */
    private ImageButton backButton, submitButton;

    /** Current group selection */
    private String selectedGroup;

    /** Handles image selection and upload operations */
    private ImageHandler imageHandler;

    /** Activity launcher for gallery access */
    private ActivityResultLauncher<Intent> galleryLauncher;

    /** Activity launcher for camera access */
    private ActivityResultLauncher<Intent> cameraLauncher;

    /** URL of the current image (null if no image) */
    private String imageUrl = null;

    /** Permission launcher for camera access */
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    /** Permission launcher for gallery access */
    private ActivityResultLauncher<String> galleryPermissionLauncher;

    /** Current emoji identifier */
    private String emoji;

    /** View displaying the current emoji */
    private ImageView editEmoji;

    /** Background container for the emoji */
    private LinearLayout editEmojiRectangle;

    /** Original image URL before any edits */
    private String originalImageUrl;

    /** Flag indicating if changes have been submitted */
    private boolean hasSubmittedChanges = false;

    /** Client for location services */
    private FusedLocationProviderClient fusedLocationClient;

    /** Permission launcher for location access */
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    /** Current device location if attached */
    private Location currentLocation;

    /** Flag indicating if location is attached */
    private boolean isLocationAttached = false;

    /** Flag indicating if image was removed */
    private boolean imageRemoved = false;

    /** Current privacy status of the mood */
    private boolean isPrivateMood = false;

    /** Displays character count for reason text */
    private TextView reasonCharCounter;

    /**
     * Initializes the activity, sets up UI components, and loads existing mood data.
     * @param savedInstanceState Saved instance state bundle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_mood);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Initialize location permission launcher
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

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get references to UI components
        editEmoji = findViewById(R.id.EditEmoji);
        editEmojiDescription = findViewById(R.id.EditEmojiDescription);
        editReason = findViewById(R.id.EditReason);
        editImagePreview = findViewById(R.id.EditImagePreview);
        backButton = findViewById(R.id.backButton);
        submitButton = findViewById(R.id.submitButton);
        editEmojiRectangle = findViewById(R.id.EditEmojiRectangle);
        TextView editDateTimeView = findViewById(R.id.EditDateTimeView);
        reasonCharCounter = findViewById(R.id.reasonCharCounter);

        // Retrieve and display existing mood data
        initializeMoodData();

        // Set up character counter
        editReason.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updateCharCounter(s.length());
            }
        });

        // Initialize image handling
        initializeImageHandling();
    }

    /**
     * Loads and displays the existing mood data from the intent extras
     */
    private void initializeMoodData() {
        Intent intent = getIntent();
        moodId = intent.getStringExtra("moodId");
        emoji = intent.getStringExtra("emoji");
        String reason = intent.getStringExtra("reason");
        imageUrl = intent.getStringExtra("imageUrl");
        originalImageUrl = imageUrl;
        int color = intent.getIntExtra("color", 0);
        isPrivateMood = intent.getBooleanExtra("privateMood", false);

        // Display timestamp if available
        Timestamp timestamp = (Timestamp) intent.getParcelableExtra("timestamp");
        if (timestamp != null) {
            String formattedTime = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault())
                    .format(timestamp.toDate());
            ((TextView) findViewById(R.id.EditDateTimeView)).setText(formattedTime);
        }

        // Set initial UI state
        editEmoji.setImageResource(EditEmojiResources.getEmojiResource(emoji));
        editEmojiDescription.setText(EditEmojiResources.getReadableMood(emoji));
        editReason.setText(reason);
        updateCharCounter(reason != null ? reason.length() : 0);
        setRoundedBackground(editEmojiRectangle, color);

        // Load image if exists
        if (imageUrl != null && !imageUrl.isEmpty()) {
            editImagePreview.setVisibility(View.VISIBLE);
            Glide.with(this).load(imageUrl).into(editImagePreview);
        }
    }

    /**
     * Initializes all image handling components including:
     * - ImageHandler instance
     * - Permission launchers
     * - Activity launchers for camera/gallery
     */
    private void initializeImageHandling() {
        imageHandler = new ImageHandler(this, editImagePreview);
        editImagePreview.setVisibility(View.GONE);

        // Set up image loaded listener
        imageHandler.setOnImageLoadedListener(new ImageHandler.OnImageLoadedListener() {
            @Override public void onImageLoaded() { editImagePreview.setVisibility(View.VISIBLE); }
            @Override public void onImageCleared() { editImagePreview.setVisibility(View.GONE); }
        });

        // Initialize permission launchers
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) imageHandler.openCamera(cameraLauncher);
                    else showMessage("Camera permission required");
                });

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) imageHandler.openGallery(galleryLauncher);
                    else showMessage("Storage permission required");
                });

        // Initialize activity launchers
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        imageHandler.handleActivityResult(result.getResultCode(), result.getData());
                        imageUrl = null;
                    }
                });

        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        imageHandler.handleActivityResult(result.getResultCode(), result.getData());
                        imageUrl = null;
                    }
                });
    }

    /**
     * Updates the character counter display
     * @param currentLength Current number of characters in reason field
     */
    private void updateCharCounter(int currentLength) {
        reasonCharCounter.setText(currentLength + "/" + MAX_REASON_LENGTH);
    }

    /**
     * Handles updating the mood in Firestore, including:
     * - Online/offline state detection
     * - Image uploads/deletions
     * - Location updates
     * - All other field updates
     */
    private void updateMoodInFirestore() {
        String newReason = editReason.getText().toString().trim();
        Map<String, Object> updates = new HashMap<>();
        updates.put("reason", newReason);
        updates.put("emotionalState", emoji);
        updates.put("emojiDescription", EditEmojiResources.getReadableMood(emoji));
        updates.put("color", EditEmojiResources.getMoodColor(emoji));
        updates.put("privateMood", isPrivateMood);

        // Handle location if attached
        if (isLocationAttached && currentLocation != null) {
            updates.put("latitude", currentLocation.getLatitude());
            updates.put("longitude", currentLocation.getLongitude());
        }

        // Handle group if changed
        if (selectedGroup != null) {
            updates.put("group", selectedGroup);
        }

        // Handle offline case
        if (NetworkUtils.isOffline(this)) {
            handleOfflineUpdate(updates);
            return;
        }

        // Handle online case with image operations
        handleOnlineUpdate(updates);
    }

    /**
     * Handles offline updates by saving changes locally
     * @param updates Map of fields to update
     */
    private void handleOfflineUpdate(Map<String, Object> updates) {
        if (imageUrl == null && imageHandler.hasImage()) {
            String localUri = imageHandler.getLocalImageUri();
            updates.put("imageUrl", localUri != null ? localUri : originalImageUrl);
        }
        Toast.makeText(this, "You're offline. Edits will sync when you're back online.", Toast.LENGTH_LONG).show();
        new MoodDataManager().saveOfflineEdit(this, moodId, updates);
        finish();
    }

    /**
     * Handles online updates including image uploads/deletions
     * @param updates Map of fields to update
     */
    private void handleOnlineUpdate(Map<String, Object> updates) {
        if (imageUrl == null && imageHandler.hasImage()) {
            uploadNewImage(updates);
        } else if (imageUrl == null && originalImageUrl != null) {
            deleteExistingImage(updates);
        } else {
            saveToFirestore(updates);
        }
    }

    /**
     * Uploads a new image and updates Firestore with the new URL
     * @param updates Map of fields to update
     */
    private void uploadNewImage(Map<String, Object> updates) {
        imageHandler.uploadImageToFirebase(new ImageHandler.OnImageUploadListener() {
            @Override
            public void onImageUploadSuccess(String url) {
                if (originalImageUrl != null && !originalImageUrl.isEmpty()) {
                    deleteImageFromStorage(originalImageUrl);
                }
                updates.put("imageUrl", url);
                saveToFirestore(updates);
            }

            @Override
            public void onImageUploadFailure(Exception e) {
                showMessage("Failed to upload image: " + e.getMessage());
            }
        });
    }

    /**
     * Deletes an existing image from storage and updates Firestore
     * @param updates Map of fields to update
     */
    private void deleteExistingImage(Map<String, Object> updates) {
        StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(originalImageUrl);
        imageRef.delete().addOnSuccessListener(aVoid -> {
            updates.put("imageUrl", null);
            saveToFirestore(updates);
        }).addOnFailureListener(e -> {
            showMessage("Failed to delete image: " + e.getMessage());
        });
    }

    /**
     * Deletes an image from Firebase Storage
     * @param imageUrl URL of the image to delete
     */
    private void deleteImageFromStorage(String imageUrl) {
        StorageReference oldImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
        oldImageRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("Firebase Storage", "Old image deleted"))
                .addOnFailureListener(e -> Log.e("Firebase Storage", "Delete failed", e));
    }

    /**
     * Saves updates to Firestore after ensuring proper image URL handling
     * @param updates Map of fields to update
     */
    private void saveToFirestore(Map<String, Object> updates) {
        if (!updates.containsKey("imageUrl")) {
            db.collection("moods").document(moodId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("imageUrl")) {
                            updates.put("imageUrl", documentSnapshot.getString("imageUrl"));
                        }
                        updateFirestore(updates);
                    });
        } else {
            updateFirestore(updates);
        }
    }

    /**
     * Performs the actual Firestore document update
     * @param updates Map of fields to update
     */
    private void updateFirestore(Map<String, Object> updates) {
        db.collection("moods").document(moodId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showMessage("Mood updated!");
                    finish();
                })
                .addOnFailureListener(e -> showMessage("Failed to update mood"));
    }

    /**
     * Handles activity results including:
     * - Emoji selection changes
     * - Location settings changes
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle emoji selection result
        if (requestCode == 1 && resultCode == RESULT_OK) {
            handleEmojiSelectionResult(data);
        }
    }

    /**
     * Processes emoji selection result and updates UI
     * @param data Intent containing selected emoji
     */
    private void handleEmojiSelectionResult(Intent data) {
        String selectedEmoji = data.getStringExtra("selectedEmoji");
        emoji = selectedEmoji;
        editEmoji.setImageResource(EditEmojiResources.getEmojiResource(selectedEmoji));
        editEmojiDescription.setText(EditEmojiResources.getReadableMood(selectedEmoji));
        setRoundedBackground(editEmojiRectangle, EditEmojiResources.getMoodColor(selectedEmoji));
    }

    /**
     * Applies rounded corner styling to a layout
     * @param layout Layout to style
     * @param color Background color to apply
     */
    private void setRoundedBackground(LinearLayout layout, int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(2, Color.BLACK);
        layout.setBackground(gradientDrawable);
    }

    /**
     * Shows group selection popup menu
     * @param v Anchor view for the popup
     */
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
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Shows image selection popup menu
     * @param v Anchor view for the popup
     */
    private void showImageMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Take a Photo");
        popup.getMenu().add("Choose from Gallery");
        popup.getMenu().add("Remove Photo");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Take a Photo")) {
                handleCameraSelection();
                return true;
            } else if (title.equals("Choose from Gallery")) {
                handleGallerySelection();
                return true;
            } else if (title.equals("Remove Photo")) {
                handleImageRemoval();
                return true;
            }
            return false;
        });
        popup.show();
    }

    /**
     * Handles camera selection with permission check
     */
    private void handleCameraSelection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            imageHandler.openCamera(cameraLauncher);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /**
     * Handles gallery selection with permission check
     */
    private void handleGallerySelection() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
            imageHandler.openGallery(galleryLauncher);
        } else {
            galleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        }
    }

    /**
     * Handles image removal including Firestore update
     */
    private void handleImageRemoval() {
        imageHandler.clearImage();
        imageUrl = null;
        imageRemoved = true;
        db.collection("moods").document(moodId)
                .update("imageUrl", null)
                .addOnSuccessListener(aVoid -> showMessage("Image removed"))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to remove image", e));
    }

    /**
     * Shows location attachment prompt dialog
     */
    private void showLocationPrompt() {
        new AlertDialog.Builder(this)
                .setTitle("Attach Location")
                .setMessage("Attach your current location to this mood?")
                .setPositiveButton("Yes", (dialog, which) -> checkLocationPermissions())
                .setNegativeButton("No", (dialog, which) -> {
                    isLocationAttached = false;
                    currentLocation = null;
                    showMessage("Location not attached");
                })
                .show();
    }

    /**
     * Checks location permissions before fetching location
     */
    private void checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    /**
     * Fetches current device location if permissions are granted
     */
    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            currentLocation = location;
                            isLocationAttached = true;
                            showMessage("Location attached");
                        } else {
                            showMessage("Unable to fetch location");
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        showMessage("Failed to fetch location: " + e.getMessage());
                    });
        }
    }

    /**
     * Displays a snackbar message
     * @param message The message to display
     */
    private void showMessage(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
    }
}