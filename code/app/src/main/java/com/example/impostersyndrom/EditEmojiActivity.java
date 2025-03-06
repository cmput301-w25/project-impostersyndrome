package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class EditEmojiActivity extends AppCompatActivity {
    private String selectedEmoji;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_emoji);

        // Get the passed emoji data
        Intent intent = getIntent();
        selectedEmoji = intent.getStringExtra("emoji");

        // Initialize UI components
        ImageView currentEmojiView = findViewById(R.id.EditEmojiView);
        currentEmojiView.setImageResource(getEmojiResource(selectedEmoji));

        // Set click listeners for each emoji selection
        findViewById(R.id.emojiI).setOnClickListener(v -> returnEmoji("emoji_happy"));
        findViewById(R.id.emojiV).setOnClickListener(v -> returnEmoji("emoji_sad"));
        findViewById(R.id.emojiIV).setOnClickListener(v -> returnEmoji("emoji_angry"));
        findViewById(R.id.emojiII).setOnClickListener(v -> returnEmoji("emoji_confused"));
        findViewById(R.id.emojiVIII).setOnClickListener(v -> returnEmoji("emoji_surprised"));
        findViewById(R.id.emojiVI).setOnClickListener(v -> returnEmoji("emoji_fear"));
        findViewById(R.id.emojiIII).setOnClickListener(v -> returnEmoji("emoji_disgust"));
        findViewById(R.id.emojiVII).setOnClickListener(v -> returnEmoji("emoji_shame"));
    }

    // Converts emoji name into drawable resource
    private int getEmojiResource(String emojiName) {
        if (emojiName == null) return R.drawable.emoji_confused; // Default emoji

        switch (emojiName.toLowerCase()) {
            case "emoji_happy": return R.drawable.emoji_happy;
            case "emoji_sad": return R.drawable.emoji_sad;
            case "emoji_angry": return R.drawable.emoji_angry;
            case "emoji_confused": return R.drawable.emoji_confused;
            case "emoji_surprised": return R.drawable.emoji_surprised;
            case "emoji_fear": return R.drawable.emoji_fear;
            case "emoji_disgust": return R.drawable.emoji_disgust;
            case "emoji_shame": return R.drawable.emoji_shame;
            default: return R.drawable.emoji_confused;
        }
    }

    // Returns the selected emoji back to EditMoodActivity
    private void returnEmoji(String emoji) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedEmoji", emoji);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
