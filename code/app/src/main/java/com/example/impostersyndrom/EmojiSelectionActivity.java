package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Date;

/**
 * EmojiSelectionActivity allows users to select an emoji representing their current mood.
 * Each emoji corresponds to a specific mood, description, and color. Upon selection,
 * the user is navigated to the AddMoodActivity to add additional details about the mood.
 *
 * @author Roshan Banisetti
 */
public class EmojiSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emoji_selection);

        // Initialize the ImageViews for emojis
        ImageView emoji1 = findViewById(R.id.emoji1);
        ImageView emoji2 = findViewById(R.id.emoji2);
        ImageView emoji3 = findViewById(R.id.emoji3);
        ImageView emoji4 = findViewById(R.id.emoji4);
        ImageView emoji5 = findViewById(R.id.emoji5);
        ImageView emoji6 = findViewById(R.id.emoji6);
        ImageView emoji7 = findViewById(R.id.emoji7);
        ImageView emoji8 = findViewById(R.id.emoji8);
        ImageButton backButton = findViewById(R.id.backButton);

        // Set click listeners for each emoji
        emoji1.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_happy", EmojiUtils.getDescription("emoji_happy"), new Date(), EmojiUtils.getColor("emoji_happy"), null);
            mood.setEmojiDrawableId(R.drawable.emoji_happy); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji2.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_confused", EmojiUtils.getDescription("emoji_confused"), new Date(), EmojiUtils.getColor("emoji_confused"), null);
            mood.setEmojiDrawableId(R.drawable.emoji_confused); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji3.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_disgust", EmojiUtils.getDescription("emoji_disgust"), new Date(), EmojiUtils.getColor("emoji_disgust"), null);
            mood.setEmojiDrawableId(R.drawable.emoji_disgust); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji4.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_angry", EmojiUtils.getDescription("emoji_angry"), new Date(), EmojiUtils.getColor("emoji_angry"), null);
            mood.setEmojiDrawableId(R.drawable.emoji_angry); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji5.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_sad", EmojiUtils.getDescription("emoji_sad"), new Date(), EmojiUtils.getColor("emoji_sad"), null);
            mood.setEmojiDrawableId(R.drawable.emoji_sad); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji6.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_fear", EmojiUtils.getDescription("emoji_fear"), new Date(), EmojiUtils.getColor("emoji_fear"), null);
            mood.setEmojiDrawableId(R.drawable.emoji_fear); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji7.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_shame", EmojiUtils.getDescription("emoji_shame"), new Date(), EmojiUtils.getColor("emoji_shame"), null);
            mood.setEmojiDrawableId(R.drawable.emoji_shame); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji8.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_surprised", EmojiUtils.getDescription("emoji_surprised"), new Date(), EmojiUtils.getColor("emoji_surprised"), null);
            mood.setEmojiDrawableId(R.drawable.emoji_surprised); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        // Back button functionality
        backButton.setOnClickListener(v -> finish());
    }

    /**
     * Navigates to the AddMoodActivity with the selected mood details.
     *
     * @param mood The Mood object containing the selected emoji, description, color, and timestamp.
     */
    private void navigateToViewMood(Mood mood) {
        Intent intent = new Intent(EmojiSelectionActivity.this, AddMoodActivity.class);
        intent.putExtra("mood", mood);
        intent.putExtra("userId", getIntent().getStringExtra("userId")); // Pass userId
        startActivity(intent);
    }
}