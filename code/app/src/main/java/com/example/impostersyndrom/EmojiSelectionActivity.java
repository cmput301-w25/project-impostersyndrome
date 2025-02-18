package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

        // List of emojis, their descriptions, and colors
        Map<String, Pair<String, Integer>> emojiMap = new HashMap<>();
        emojiMap.put("\uD83D\uDE01", new Pair<>("Happy", Color.parseColor("#FFCC00"))); // Happy
        emojiMap.put("\uD83E\uDD14", new Pair<>("Confused", Color.parseColor("#8B7355"))); // Confused
        emojiMap.put("\uD83E\uDD22", new Pair<>("Disgust", Color.parseColor("#808000"))); // Disgust
        emojiMap.put("\uD83D\uDE21", new Pair<>("Angry", Color.parseColor("#FF4D00"))); // Angry
        emojiMap.put("\uD83D\uDE14", new Pair<>("Sad", Color.parseColor("#2980B9"))); // Sad
        emojiMap.put("\uD83D\uDE28", new Pair<>("Fear", Color.parseColor("#9B59B6"))); // Fear
        emojiMap.put("\uD83D\uDE05", new Pair<>("Shame", Color.parseColor("#FFC0CB"))); // Shame
        emojiMap.put("\uD83D\uDE32", new Pair<>("Surprise", Color.parseColor("#1ABC9C"))); // Surprise

        // Set click listeners for each emoji
        emoji1.setOnClickListener(v -> navigateToViewMood(new Mood("\uD83D\uDE01", emojiMap.get("\uD83D\uDE01").first, new Date(), emojiMap.get("\uD83D\uDE01").second)));
        emoji2.setOnClickListener(v -> navigateToViewMood(new Mood("\uD83E\uDD14", emojiMap.get("\uD83E\uDD14").first, new Date(), emojiMap.get("\uD83E\uDD14").second)));
        emoji3.setOnClickListener(v -> navigateToViewMood(new Mood("\uD83E\uDD22", emojiMap.get("\uD83E\uDD22").first, new Date(), emojiMap.get("\uD83E\uDD22").second)));
        emoji4.setOnClickListener(v -> navigateToViewMood(new Mood("\uD83D\uDE21", emojiMap.get("\uD83D\uDE21").first, new Date(), emojiMap.get("\uD83D\uDE21").second)));
        emoji5.setOnClickListener(v -> navigateToViewMood(new Mood("\uD83D\uDE14", emojiMap.get("\uD83D\uDE14").first, new Date(), emojiMap.get("\uD83D\uDE14").second)));
        emoji6.setOnClickListener(v -> navigateToViewMood(new Mood("\uD83D\uDE28", emojiMap.get("\uD83D\uDE28").first, new Date(), emojiMap.get("\uD83D\uDE28").second)));
        emoji7.setOnClickListener(v -> navigateToViewMood(new Mood("\uD83D\uDE05", emojiMap.get("\uD83D\uDE05").first, new Date(), emojiMap.get("\uD83D\uDE05").second)));
        emoji8.setOnClickListener(v -> navigateToViewMood(new Mood("\uD83D\uDE32", emojiMap.get("\uD83D\uDE32").first, new Date(), emojiMap.get("\uD83D\uDE32").second)));
    }

    // Helper method to navigate to View Mood Screen
    private void navigateToViewMood(Mood mood) {
        Intent intent = new Intent(EmojiSelectionActivity.this, AddMoodActivity.class);
        intent.putExtra("mood", mood);
        startActivity(intent);
    }

    // Pair class to hold emoji description and color
    private static class Pair<F, S> {
        public final F first; //DEsc
        public final S second; //Color

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;

        }
    }
}
