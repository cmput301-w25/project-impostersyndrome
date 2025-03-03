package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class MoodDetailActivity extends AppCompatActivity {

    private static final String TAG = "MoodDetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_detail);

        // Initialize views
        ImageView emojiView = findViewById(R.id.emojiView);
        TextView timeView = findViewById(R.id.timeView);
        TextView reasonView = findViewById(R.id.reasonView);
        TextView emojiDescView = findViewById(R.id.emojiDescription);
        TextView groupView = findViewById(R.id.groupView);
        View emojiRectangle = findViewById(R.id.emojiRectangle);
        ImageView imageUrlView = findViewById(R.id.imageUrlView);
        ImageButton backButton = findViewById(R.id.backButton);

        // Retrieve data from Intent
        Intent intent = getIntent();
        String emoji = intent.getStringExtra("emoji");
        Timestamp timestamp = (Timestamp) intent.getParcelableExtra("timestamp");
        String reason = intent.getStringExtra("reason");
        String group = intent.getStringExtra("group");
        int color = intent.getIntExtra("color", Color.WHITE);
        String emojiDescription = intent.getStringExtra("emojiDescription");
        String imageUrl = intent.getStringExtra("imageUrl");

        // Log received data for debugging
        Log.d(TAG, "Emoji: " + emoji);
        Log.d(TAG, "Reason: " + reason);
        Log.d(TAG, "Group: " + group);
        Log.d(TAG, "Emoji Description: " + emojiDescription);
        Log.d(TAG, "Image URL: " + (imageUrl != null ? imageUrl : "null"));

        // Set click listener for the back button
        backButton.setOnClickListener(v -> {
            // Navigate to MainActivity
            Intent intent2 = new Intent(MoodDetailActivity.this, MainActivity.class);
            intent2.putExtra("userId", getIntent().getStringExtra("userId")); // Pass userId back
            startActivity(intent2);
            finish();
        });

        // Set the custom emoji image
        if (emoji != null) {
            int emojiResId = getResources().getIdentifier(emoji, "drawable", getPackageName());
            if (emojiResId != 0) {
                emojiView.setImageResource(emojiResId);
            } else {
                Log.e(TAG, "Could not find drawable resource for emoji: " + emoji);
            }
        }

        // Set the time
        if (timestamp != null) {
            String formattedTime = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault()).format(timestamp.toDate());
            timeView.setText(formattedTime);
        } else {
            timeView.setText("Unknown time");
        }

        // Set the reason
        reasonView.setText(reason != null ? reason : "No reason provided");

        // Set the group
        groupView.setText(group != null ? group : "No group provided");

        // Set the emoji description
        emojiDescView.setText(emojiDescription != null ? emojiDescription : "No emoji");

        // Load the image from URL using Glide
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Make sure ImageView is visible
            imageUrlView.setVisibility(View.VISIBLE);

            Log.d(TAG, "Loading image from URL: " + imageUrl);

            // Use Glide to load the image with error handling
            Glide.with(this)
                    .load(imageUrl)
                    .into(imageUrlView);
        } else {
            Log.d(TAG, "No image URL provided, hiding ImageView");
            imageUrlView.setVisibility(View.GONE);
        }

        // Apply background to the emoji rectangle, not the root layout
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(2, Color.BLACK);
        emojiRectangle.setBackground(gradientDrawable);
    }

//    @Override
//    protected void onDestroy() {
//        // Clean up Glide resources when activity is destroyed
//        Glide.with(this).clear(findViewById(R.id.imageUrlView));
//        super.onDestroy();
//    }
}