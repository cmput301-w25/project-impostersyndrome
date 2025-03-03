package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.Button;
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
    private String selectedGroup;
    private ImageHandler imageHandler;
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private String imageUrl = null;
    private TextView triggerCharCount; // Character count for trigger field
    private TextView reasonCharCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood);

        db = FirebaseFirestore.getInstance();
        moodsRef = db.collection("moods");

        // Initialize views
        ImageView emojiView = findViewById(R.id.emojiView); // Using ImageView for custom emoji
        TextView emojiDescription = findViewById(R.id.emojiDescription);
        TextView timeView = findViewById(R.id.dateTimeView);
        LinearLayout emojiRectangle = findViewById(R.id.emojiRectangle);
        EditText addReasonEdit = findViewById(R.id.addReasonEdit);
        reasonCharCount = findViewById(R.id.reasonCharCount);
        ImageButton submitButton = findViewById(R.id.submitButton);
        ImageButton backButton = findViewById(R.id.backButton);
        ImageButton groupButton = findViewById(R.id.groupButton);
        ImageView imagePreview = findViewById(R.id.imagePreview);

        // Add text change listener to update character count


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

        backButton.setOnClickListener(v -> finish());
    }



    // Helper method to navigate to main activity
    private void navigateToMainActivity() {
        Intent newIntent = new Intent(AddMoodActivity.this, MainActivity.class);
        newIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(newIntent);
        finish();
    }
    // Add mood to Firestore
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

    // Group menu method
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
}