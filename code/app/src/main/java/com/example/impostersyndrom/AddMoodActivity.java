package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
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

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMoodActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private CollectionReference moodsRef;
    String selectedGroup;
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
        ImageButton groupButton = findViewById(R.id.groupButton);
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
            selectedGroup = mood.getGroup();

        }
        groupButton.setOnClickListener(v -> showGroupsMenu(v));

        submitButton.setOnClickListener(v -> {
            mood.setReason(addReasonEdit.getText().toString().trim());

            mood.setGroup(selectedGroup);
            addMood(mood);
            Toast.makeText(AddMoodActivity.this, "Mood saved!", Toast.LENGTH_SHORT).show();
            Intent new_intent = new Intent(AddMoodActivity.this, MainActivity.class);
            new_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(new_intent);
            finish();

            mood.setUserId(User.getInstance().getUserId());

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
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50); // Rounded corners (50dp radius)
        gradientDrawable.setColor(color); // Set the background color
        gradientDrawable.setStroke(2, Color.BLACK); // Set the border (2dp width, black color)

        // Set the GradientDrawable as the background
        layout.setBackground(gradientDrawable);
    }


    private void showGroupsMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.group_menu, popup.getMenu());
        Map<Integer, String> menuMap = new HashMap<>(); // Hashmap maps each menu id to each respose
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
}