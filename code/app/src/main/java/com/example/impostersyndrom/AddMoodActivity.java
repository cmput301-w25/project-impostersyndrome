package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AddMoodActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private CollectionReference moodsRef;
    private ImageHandler imageHandler;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private String imageUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood); // Set the layout for this activity
        db = FirebaseFirestore.getInstance();
        moodsRef = db.collection("moods");

        // Initialize views
        TextView emojiView = findViewById(R.id.emojiView);
        TextView emojiDescription = findViewById(R.id.emojiDescription);
        TextView timeView = findViewById(R.id.dateTimeView);
        LinearLayout emojiRectangle = findViewById(R.id.emojiRectangle);
        EditText addReasonEdit = findViewById(R.id.addReasonEdit);
        ImageButton submitButton = findViewById(R.id.submitButton);
        ImageView imagePreview = findViewById(R.id.imagePreview);

        imageHandler = new ImageHandler(this, imagePreview);

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

        if (mood != null) {
            // Display the emoji and description
            emojiView.setText(mood.getEmotionalState());
            emojiDescription.setText(mood.getEmojiDescription());

            // Set the current time
            String currentTime = new SimpleDateFormat("dd-MM-YYYY | HH:mm", Locale.getDefault()).format(mood.getTimestamp());
            timeView.setText(currentTime);

            // Set the background color, rounded corners, and border for the rectangle
            setRoundedBackground(emojiRectangle, mood.getColor());
        }

        submitButton.setOnClickListener(v -> {
            mood.setReason(addReasonEdit.getText().toString().trim());

            if (imageHandler.hasImage()) {
                imageHandler.uploadImageToFirebase(new ImageHandler.OnImageUploadListener() {
                    @Override
                    public void onImageUploadSuccess(String url) {
                        imageUrl = url; // Store the image URL
                        mood.setImageUrl(imageUrl); // Link the image URL to the mood
                        addMood(mood);
                        Toast.makeText(AddMoodActivity.this, "Mood saved!", Toast.LENGTH_SHORT).show();
                        Intent newIntent = new Intent(AddMoodActivity.this, MainActivity.class);
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(newIntent);
                        finish();
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
                Intent newIntent = new Intent(AddMoodActivity.this, MainActivity.class);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newIntent);
                finish();
            }
        });

        Button openGalleryButton = findViewById(R.id.uploadButton);
        openGalleryButton.setOnClickListener(v -> imageHandler.openGallery(galleryLauncher));

        Button openCameraButton = findViewById(R.id.cameraButton);
        openCameraButton.setOnClickListener(v -> imageHandler.openCamera(cameraLauncher));
    }

    public void addMood(Mood mood) {
        DocumentReference docRef = moodsRef.document(mood.getId());
        docRef.set(mood);
    }


    // Helper method to set rounded background with dynamic color
    private void setRoundedBackground(LinearLayout layout, int color) {
        // Create a GradientDrawable for the background color, rounded corners, and border
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50); // Rounded corners (50dp radius)
        gradientDrawable.setColor(color); // Set the background color
        gradientDrawable.setStroke(2, Color.BLACK); // Set the border (2dp width, black color)

        // Set the GradientDrawable as the background
        layout.setBackground(gradientDrawable);
    }
}
