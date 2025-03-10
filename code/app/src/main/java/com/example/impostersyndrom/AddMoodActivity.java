package com.example.impostersyndrom;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * AddMoodActivity is responsible for allowing users to add a new mood entry.
 * It provides functionality to select an emoji, add a reason, choose a group, and optionally upload an image.
 * The mood data is then saved to Firestore.
 * @author Roshan Banisetti
 * @author
 */
public class AddMoodActivity extends AppCompatActivity {
    private FirebaseFirestore db; // Firestore database instance
    private CollectionReference moodsRef; // Reference to the "moods" collection in Firestore
    private String selectedGroup; // Stores the selected group for the mood
    private ImageHandler imageHandler; // Handles image selection and uploading
    private ActivityResultLauncher<Intent> galleryLauncher; // Launcher for gallery intent
    private ActivityResultLauncher<Intent> cameraLauncher; // Launcher for camera intent
    private String imageUrl = null; // URL of the uploaded image
    private TextView reasonCharCount; // Displays character count for the reason text
    private ImageView imagePreview; // Preview of the selected image
    private ActivityResultLauncher<String> cameraPermissionLauncher; // Launcher for camera permission request
    private ActivityResultLauncher<String> galleryPermissionLauncher; // Launcher for gallery permission request

    /**
     * Initializes the activity, setting up UI components, event listeners,
     * and handling mood creation workflow.
     *
     * Key initialization steps:
     * - Sets up Firebase Firestore connection
     * - Configures permission launchers for camera and gallery
     * - Sets up UI event listeners
     * - Handles incoming mood data
     *
     * @param savedInstanceState Previous saved state of the activity
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood);

        // Initialize Firestore and moods collection reference
        db = FirebaseFirestore.getInstance();
        moodsRef = db.collection("moods");

        // Initialize permission launchers for camera and gallery
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, launch camera intent
                        imageHandler.openCamera(cameraLauncher);
                    } else {
                        Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                    }
                });

        galleryPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, launch gallery intent
                        imageHandler.openGallery(galleryLauncher);
                    } else {
                        Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show();
                    }
                });

        // Initialize views
        ImageView emojiView = findViewById(R.id.emojiView);
        TextView emojiDescription = findViewById(R.id.emojiDescription);
        TextView timeView = findViewById(R.id.dateTimeView);
        LinearLayout emojiRectangle = findViewById(R.id.emojiRectangle);
        EditText addReasonEdit = findViewById(R.id.addReasonEdit);
        reasonCharCount = findViewById(R.id.reasonCharCount);
        ImageButton submitButton = findViewById(R.id.submitButton);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton groupButton = findViewById(R.id.groupButton);
        ImageButton cameraMenuButton = findViewById(R.id.cameraMenuButton);
        imagePreview = findViewById(R.id.imagePreview);

        // Initially hide the image preview
        imagePreview.setVisibility(View.GONE);

        // Add text change listener to update character count and enforce word limit
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
                String[] words = text.split("\\s+"); // Splits based on number of whitespaces
                int wordCount = words.length;

                if (wordCount > 3) {
                    s.delete(s.length() - 1, s.length());
                }
                reasonCharCount.setText(chars + "/20");
            }
        });

        // Initialize image handling
        imageHandler = new ImageHandler(this, imagePreview);

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

        // Start ActivityResultLauncher for gallery
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> imageHandler.handleActivityResult(result.getResultCode(), result.getData())
        );

        // Start ActivityResultLauncher for camera
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> imageHandler.handleActivityResult(result.getResultCode(), result.getData())
        );

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
            selectedGroup = mood.getGroup();
        }

        // Group button functionality
        groupButton.setOnClickListener(v -> showGroupsMenu(v));

        // Setup camera menu button to show options
        cameraMenuButton.setOnClickListener(v -> showImageMenu(v));

        // Back button functionality
        backButton.setOnClickListener(v -> {
            finish();
        });

        // Submit button with image handling
        submitButton.setOnClickListener(v -> {
            mood.setReason(addReasonEdit.getText().toString().trim());
            mood.setGroup(selectedGroup);
            mood.setUserId(User.getInstance().getUserId());

            if (imageHandler.hasImage()) {
                imageHandler.uploadImageToFirebase(new ImageHandler.OnImageUploadListener() {
                    @Override
                    public void onImageUploadSuccess(String url) {
                        imageUrl = url;
                        mood.setImageUrl(imageUrl);
                        addMood(mood);
                        Toast.makeText(AddMoodActivity.this, "Mood saved!", Toast.LENGTH_SHORT).show();
                        navigateToMainActivity();
                    }

                    @Override
                    public void onImageUploadFailure(Exception e) {
                        Toast.makeText(AddMoodActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                mood.setImageUrl(null);
                addMood(mood);
                Toast.makeText(AddMoodActivity.this, "Mood saved!", Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
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
     * Adds a mood entry to Firestore.
     *
     * @param mood The Mood object to be saved.
     */
    public void addMood(Mood mood) {
        DocumentReference docRef = moodsRef.document(mood.getId());
        docRef.set(mood);
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
}