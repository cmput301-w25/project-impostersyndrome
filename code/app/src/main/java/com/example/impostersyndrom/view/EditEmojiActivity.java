package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.impostersyndrom.controller.EditEmojiResources;
import com.example.impostersyndrom.R;

/**
 * EditEmojiActivity allows users to select a new emoji for their mood entry.
 * It displays the current emoji and provides options to choose a different one.
 * The selected emoji is returned to the calling activity (EditMoodActivity).
 *
 * @author Rayan
 */
public class EditEmojiActivity extends AppCompatActivity {
    private String selectedEmoji; // Stores the currently selected emoji

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_emoji);

        // Get the passed emoji data from the intent
        Intent intent = getIntent();
        selectedEmoji = intent.getStringExtra("emoji");

        // Initialize UI components
        ImageView currentEmojiView = findViewById(R.id.EditEmojiView);
        currentEmojiView.setImageResource(EditEmojiResources.getEmojiResource(selectedEmoji));

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

    /**
     * Returns the selected emoji back to the calling activity (EditMoodActivity).
     *
     * @param emoji The name of the selected emoji.
     */
    private void returnEmoji(String emoji) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("selectedEmoji", emoji);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}