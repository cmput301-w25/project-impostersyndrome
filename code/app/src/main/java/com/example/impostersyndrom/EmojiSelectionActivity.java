package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class EmojiSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emoji_selection); // Set the layout for this activity

        // Initialize the TextViews for emojis
        TextView emoji1 = findViewById(R.id.emoji1);
        TextView emoji2 = findViewById(R.id.emoji2);
        TextView emoji3 = findViewById(R.id.emoji3);
        TextView emoji4 = findViewById(R.id.emoji4);
        TextView emoji5 = findViewById(R.id.emoji5);
        TextView emoji6 = findViewById(R.id.emoji6);
        TextView emoji7 = findViewById(R.id.emoji7);
        TextView emoji8 = findViewById(R.id.emoji8);

        // List of emojis and their colors
        Map<String, Integer> emojiColorMap = new HashMap<>();
        emojiColorMap.put("\uD83D\uDE01", Color.parseColor("#FFCC00")); // Happy
        emojiColorMap.put("\uD83E\uDD14", Color.parseColor("#8B7355")); // confused
        emojiColorMap.put("\uD83E\uDD22", Color.parseColor("#808000")); // Disgust
        emojiColorMap.put("\uD83D\uDE21", Color.parseColor("#FF4D00")); // Angry
        emojiColorMap.put("\uD83D\uDE14", Color.parseColor("#2980B9")); // Sad
        emojiColorMap.put("\uD83D\uDE28", Color.parseColor("#9B59B6")); // Fear
        emojiColorMap.put("\uD83D\uDE05", Color.parseColor("#FFC0CB")); // Shame (pink)
        emojiColorMap.put("\uD83D\uDE32", Color.parseColor("#1ABC9C")); // Surprise

        // Set click listeners for each emoji
        emoji1.setOnClickListener(v -> navigateToViewMood("\uD83D\uDE01", emojiColorMap.get("\uD83D\uDE01")));
        emoji2.setOnClickListener(v -> navigateToViewMood("\uD83E\uDD14", emojiColorMap.get("\uD83E\uDD14")));
        emoji3.setOnClickListener(v -> navigateToViewMood("\uD83E\uDD22", emojiColorMap.get("\uD83E\uDD22")));
        emoji4.setOnClickListener(v -> navigateToViewMood("\uD83D\uDE21", emojiColorMap.get("\uD83D\uDE21")));
        emoji5.setOnClickListener(v -> navigateToViewMood("\uD83D\uDE14", emojiColorMap.get("\uD83D\uDE14")));
        emoji6.setOnClickListener(v -> navigateToViewMood("\uD83D\uDE28", emojiColorMap.get("\uD83D\uDE28")));
        emoji7.setOnClickListener(v -> navigateToViewMood("\uD83D\uDE05", emojiColorMap.get("\uD83D\uDE05")));
        emoji8.setOnClickListener(v -> navigateToViewMood("\uD83D\uDE32", emojiColorMap.get("\uD83D\uDE32")));
    }

    // Helper method to navigate to View Mood Screen
    private void navigateToViewMood(String emoji, int color) {
        Intent intent = new Intent(EmojiSelectionActivity.this, AddMoodActivity.class);
        intent.putExtra("emoji", emoji);
        intent.putExtra("color", color);
        startActivity(intent);
    }
}