package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AddMoodActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private CollectionReference moodsRef;

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
            addMood(mood);
            Toast.makeText(AddMoodActivity.this, "Mood saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
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
