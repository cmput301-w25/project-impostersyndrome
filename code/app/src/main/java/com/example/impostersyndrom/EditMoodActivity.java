package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.MenuInflater;
import android.widget.PopupMenu;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditMoodActivity extends AppCompatActivity {
    private String moodId;
    private FirebaseFirestore db;

    private TextView editEmojiDescription;
    private EditText editReason;
    private ImageView editImagePreview;
    private ImageButton backButton, submitButton;
    private String selectedGroup;
    private ImageHandler imageHandler;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private String imageUrl = null;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> galleryPermissionLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_mood);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get UI elements
        editEmojiDescription = findViewById(R.id.EditEmojiDescription);
        editReason = findViewById(R.id.EditReason);
        backButton = findViewById(R.id.backButton);
        submitButton = findViewById(R.id.submitButton);
        LinearLayout EditEmojiRectangle = findViewById(R.id.EditEmojiRectangle);

        // Initialize buttons
        ImageButton editGroupButton = findViewById(R.id.EditGroupButton);
        ImageButton editCameraMenuButton = findViewById(R.id.EditCameraMenuButton);
        editCameraMenuButton.setOnClickListener(v -> showImageMenu(v));

        // Initialize image handling
        imageHandler = new ImageHandler(this, editImagePreview);

        // Initialize permission launchers
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

        // Set up listener to show/hide image preview
        imageHandler.setOnImageLoadedListener(new ImageHandler.OnImageLoadedListener() {
            @Override
            public void onImageLoaded() {
                editImagePreview.setVisibility(View.VISIBLE);
            }

            @Override
            public void onImageCleared() {
                editImagePreview.setVisibility(View.GONE);
            }
        });


        // Attach event listeners
        editGroupButton.setOnClickListener(v -> showGroupsMenu(v));
        editCameraMenuButton.setOnClickListener(v -> showImageMenu(v));


        // Retrieve passed mood data
        Intent intent = getIntent();
        moodId = intent.getStringExtra("moodId");
        String emoji = intent.getStringExtra("emoji");
        String reason = intent.getStringExtra("reason");
        String imageUrl = intent.getStringExtra("imageUrl");
        int color = intent.getIntExtra("color", 0);

        editImagePreview = findViewById(R.id.EditImagePreview);

        // Get UI elements
        editEmojiDescription = findViewById(R.id.EditEmojiDescription);
        editReason = findViewById(R.id.EditReason);
        backButton = findViewById(R.id.backButton);
        submitButton = findViewById(R.id.submitButton);

        if (imageUrl != null && !imageUrl.isEmpty() && editImagePreview != null) {
            Glide.with(this).load(imageUrl).into(editImagePreview);
        } else {
            editImagePreview.setImageResource(0); // Clear image if null
        }

        // Set UI elements with retrieved data
        editEmojiDescription.setText(getReadableMood(emoji));
        editReason.setText(reason);

        // Apply the background color to the rectangle
        setRoundedBackground(EditEmojiRectangle, color);

        // Back button functionality
        backButton.setOnClickListener(v -> finish());

        // Save updated mood when checkmark button is clicked
        submitButton.setOnClickListener(v -> updateMoodInFirestore());


        // Set UI elements with retrieved data
        editEmojiDescription.setText(getReadableMood(emoji));
        editReason.setText(reason); // Display previous reason

        // Clear text only when the user first clicks inside the EditText
        editReason.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                editReason.setText(""); // Clears text when clicked
                editReason.setOnFocusChangeListener(null); // Removes listener so it doesn't clear repeatedly
            }
        });



        // Back button functionality
        backButton.setOnClickListener(v -> finish());

        // Save updated mood when checkmark button is clicked
        submitButton.setOnClickListener(v -> updateMoodInFirestore());
    }

    private void updateMoodInFirestore() {
        String newReason = editReason.getText().toString().trim();

        // Create a map for updating Firestore
        Map<String, Object> updates = new HashMap<>();
        updates.put("reason", newReason);

        // Only update the group if a new one was selected
        if (selectedGroup != null) {
            updates.put("group", selectedGroup);
        }

        // Handle Image Upload
        if (imageHandler.hasImage()) {
            imageHandler.uploadImageToFirebase(new ImageHandler.OnImageUploadListener() {
                @Override
                public void onImageUploadSuccess(String url) {
                    updates.put("imageUrl", url);
                    saveToFirestore(updates);
                }

                @Override
                public void onImageUploadFailure(Exception e) {
                    Toast.makeText(EditMoodActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // If image is removed, update Firestore with null
            updates.put("imageUrl", null);
            saveToFirestore(updates);
        }
    }


    private void saveToFirestore(Map<String, Object> updates) {
        db.collection("moods").document(moodId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditMoodActivity.this, "Mood updated!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to MainActivity
                })
                .addOnFailureListener(e -> Toast.makeText(EditMoodActivity.this, "Failed to update mood", Toast.LENGTH_SHORT).show());
    }



    private String getReadableMood(String emoji) {
        if (emoji == null) return "Unknown Mood";

        switch (emoji.toLowerCase()) {
            case "emoji_happy": return "Happy";
            case "emoji_sad": return "Sad";
            case "emoji_angry": return "Angry";
            case "emoji_confused": return "Confused";
            case "emoji_surprised": return "Surprised";
            case "emoji_fear": return "Fearful";
            case "emoji_disgust": return "Disgusted";
            case "emoji_shame": return "Ashamed";
            default: return "Unknown Mood"; // Fallback
        }
    }

    private void setRoundedBackground(LinearLayout layout, int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50); // Rounded corners
        gradientDrawable.setColor(color); // Apply mood color
        gradientDrawable.setStroke(2, Color.BLACK); // Add border

        // Apply the background to the layout
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
                selectedGroup = menuMap.get(item.getItemId()); // Store selection
                Toast.makeText(EditMoodActivity.this, "Group Selection: " + selectedGroup, Toast.LENGTH_SHORT).show();
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
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    imageHandler.openCamera(cameraLauncher);
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA);
                }
                return true;
            } else if (item.getTitle().equals("Choose from Gallery")) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                    imageHandler.openGallery(galleryLauncher);
                } else {
                    galleryPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
                }
                return true;
            } else if (item.getTitle().equals("Remove Photo")) {
                imageHandler.clearImage();
                updateMoodInFirestore();
                return true;
            }
            return false;
        });

        popup.show();
    }



}
