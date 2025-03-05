package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditMoodActivity extends AppCompatActivity {
    private String moodId;
    private FirebaseFirestore db;

    private TextView editEmojiDescription;
    private EditText editReason, editTrigger;
    private ImageView editImagePreview;
    private ImageButton backButton, submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_mood);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get UI elements
        editEmojiDescription = findViewById(R.id.EditEmojiDescription);
        editReason = findViewById(R.id.EditReason);
        editTrigger = findViewById(R.id.editTrigger);
        editImagePreview = findViewById(R.id.EditImagePreview);
        backButton = findViewById(R.id.backButton);
        submitButton = findViewById(R.id.submitButton);

        // Retrieve passed mood data
        Intent intent = getIntent();
        moodId = intent.getStringExtra("moodId");
        String emoji = intent.getStringExtra("emoji");
        String reason = intent.getStringExtra("reason");
        String trigger = intent.getStringExtra("trigger");
        String imageUrl = intent.getStringExtra("imageUrl");

        // Set UI elements with retrieved data
        editEmojiDescription.setText(emoji);
        editReason.setText(reason);
        editTrigger.setText(trigger);

        // Load image if available
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(editImagePreview);
        }

        // Back button functionality
        backButton.setOnClickListener(v -> finish());

        // Save updated mood when checkmark button is clicked
        submitButton.setOnClickListener(v -> updateMoodInFirestore());
    }

    private void updateMoodInFirestore() {
        String newReason = editReason.getText().toString().trim();
        String newTrigger = editTrigger.getText().toString().trim();

        db.collection("moods").document(moodId)
                .update("reason", newReason, "trigger", newTrigger)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditMoodActivity.this, "Mood updated!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to MainActivity
                })
                .addOnFailureListener(e -> Toast.makeText(EditMoodActivity.this, "Failed to update mood", Toast.LENGTH_SHORT).show());
    }
}
