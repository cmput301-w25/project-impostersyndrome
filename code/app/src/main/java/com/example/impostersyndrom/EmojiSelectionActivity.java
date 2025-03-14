package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

        // Map to store emoji descriptions and corresponding colors
        Map<String, Pair<String, Integer>> emojiMap = new HashMap<>();
        emojiMap.put("emoji_happy", new Pair<>("Happy", Color.parseColor("#FFCC00"))); // Happy
        emojiMap.put("emoji_confused", new Pair<>("Confused", Color.parseColor("#8B7355"))); // Confused
        emojiMap.put("emoji_disgust", new Pair<>("Disgust", Color.parseColor("#808000"))); // Disgust
        emojiMap.put("emoji_angry", new Pair<>("Angry", Color.parseColor("#FF4D00"))); // Angry
        emojiMap.put("emoji_sad", new Pair<>("Sad", Color.parseColor("#2980B9"))); // Sad
        emojiMap.put("emoji_fear", new Pair<>("Fear", Color.parseColor("#9B59B6"))); // Fear
        emojiMap.put("emoji_shame", new Pair<>("Shame", Color.parseColor("#C64B70"))); // Shame
        emojiMap.put("emoji_surprised", new Pair<>("Surprise", Color.parseColor("#1ABC9C"))); // Surprise

        // Set click listeners for each emoji
        emoji1.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_happy", emojiMap.get("emoji_happy").desc, new Date(), emojiMap.get("emoji_happy").color, null);
            mood.setEmojiDrawableId(R.drawable.emoji_happy); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji2.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_confused", emojiMap.get("emoji_confused").desc, new Date(), emojiMap.get("emoji_confused").color, null);
            mood.setEmojiDrawableId(R.drawable.emoji_confused); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji3.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_disgust", emojiMap.get("emoji_disgust").desc, new Date(), emojiMap.get("emoji_disgust").color, null);
            mood.setEmojiDrawableId(R.drawable.emoji_disgust); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji4.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_angry", emojiMap.get("emoji_angry").desc, new Date(), emojiMap.get("emoji_angry").color, null);
            mood.setEmojiDrawableId(R.drawable.emoji_angry); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji5.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_sad", emojiMap.get("emoji_sad").desc, new Date(), emojiMap.get("emoji_sad").color, null);
            mood.setEmojiDrawableId(R.drawable.emoji_sad); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji6.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_fear", emojiMap.get("emoji_fear").desc, new Date(), emojiMap.get("emoji_fear").color, null);
            mood.setEmojiDrawableId(R.drawable.emoji_fear); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji7.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_shame", emojiMap.get("emoji_shame").desc, new Date(), emojiMap.get("emoji_shame").color, null);
            mood.setEmojiDrawableId(R.drawable.emoji_shame); // Pass the drawable resource ID
            navigateToViewMood(mood);
        });

        emoji8.setOnClickListener(v -> {
            Mood mood = new Mood("emoji_surprised", emojiMap.get("emoji_surprised").desc, new Date(), emojiMap.get("emoji_surprised").color, null);
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

    /**
     * A simple Pair class to hold two related values: description and color.
     *
     * @param <F> The type of the first value (description).
     * @param <S> The type of the second value (color).
     */
    private static class Pair<F, S> {
        public final F desc; // Description of the emoji
        public final S color; // Color associated with the emoji

        /**
         * Constructs a new Pair with the given values.
         *
         * @param desc  The description of the emoji.
         * @param color The color associated with the emoji.
         */
        public Pair(F desc, S color) {
            this.desc = desc;
            this.color = color;
        }
    }
}