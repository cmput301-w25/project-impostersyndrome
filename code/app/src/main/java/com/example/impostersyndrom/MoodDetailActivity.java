package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide; // Add Glide dependency
import com.google.firebase.Timestamp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MoodDetailActivity extends AppCompatActivity {

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
        View rootLayout = findViewById(R.id.rootLayout);

        // Retrieve data from Intent
        Intent intent = getIntent();
        String emoji = intent.getStringExtra("emoji");
        Timestamp timestamp = (Timestamp) intent.getParcelableExtra("timestamp");
        String reason = intent.getStringExtra("reason");
        String group = intent.getStringExtra("group");
        int color = intent.getIntExtra("color", Color.WHITE);
        String emojiDescription = intent.getStringExtra("emojiDescription");

        ImageButton backButton = findViewById(R.id.backButton);

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
            emojiView.setImageResource(emojiResId);
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
        emojiDescView.setText(emojiDescription != null ? emojiDescription : "No emoji");




        // Load the image from URL using Glide

        String imageUrl = intent.getStringExtra("imageUrl");
        ImageView imageUrlView = findViewById(R.id.imageUrlView);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .into(imageUrlView);
        } else {
            imageUrlView.setVisibility(View.GONE); // Hide the ImageView if no URL is provided
        }

        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(2, Color.BLACK);
        rootLayout.setBackground(gradientDrawable);
    }
}
