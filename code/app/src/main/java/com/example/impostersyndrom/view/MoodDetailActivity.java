package com.example.impostersyndrom.view;

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
import com.example.impostersyndrom.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * MoodDetailActivity displays detailed information about a specific mood entry.
 * It provides a comprehensive view of a mood, including:
 * - Emoji representation
 * - Timestamp
 * - Mood reason
 * - Social group context
 * - Optional attached image
 *
 * @author Ali Zain
 */
public class MoodDetailActivity extends AppCompatActivity {

    private static final String TAG = "MoodDetailActivity";

    // UI Components
    private ImageView emojiView;
    private TextView timeView;
    private TextView reasonView;
    private TextView emojiDescView;
    private TextView groupView;
    private View emojiRectangle;
    private ImageView imageUrlView;
    private ImageButton backButton;

    // Mood Data
    private String emoji;
    private Timestamp timestamp;
    private String reason;
    private String group;
    private int color;
    private String emojiDescription;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_detail);

        // Initialize UI components
        initializeViews();

        // Retrieve data from Intent
        retrieveIntentData();

        // Set up UI with mood data
        setupUI();

        // Set up back button click listener
        setupBackButton();
    }

    /**
     * Initializes all UI components.
     */
    private void initializeViews() {
        emojiView = findViewById(R.id.emojiView);
        timeView = findViewById(R.id.timeView);
        reasonView = findViewById(R.id.reasonView);
        emojiDescView = findViewById(R.id.emojiDescription);
        groupView = findViewById(R.id.groupView);
        emojiRectangle = findViewById(R.id.emojiRectangle);
        imageUrlView = findViewById(R.id.imageUrlView);
        backButton = findViewById(R.id.backButton);
    }

    /**
     * Retrieves mood data from the Intent.
     */
    private void retrieveIntentData() {
        Intent intent = getIntent();
        emoji = intent.getStringExtra("emoji");
        timestamp = (Timestamp) intent.getParcelableExtra("timestamp");
        reason = intent.getStringExtra("reason");
        group = intent.getStringExtra("group");
        color = intent.getIntExtra("color", Color.WHITE);
        emojiDescription = intent.getStringExtra("emojiDescription");
        imageUrl = intent.getStringExtra("imageUrl");

        // Log received data for debugging
        logMoodData();
    }

    /**
     * Logs mood data for debugging purposes.
     */
    private void logMoodData() {
        Log.d(TAG, "Emoji: " + emoji);
        Log.d(TAG, "Reason: " + reason);
        Log.d(TAG, "Group: " + group);
        Log.d(TAG, "Emoji Description: " + emojiDescription);
        Log.d(TAG, "Image URL: " + (imageUrl != null ? imageUrl : "null"));
    }

    /**
     * Sets up the UI with mood data.
     */
    private void setupUI() {
        setEmojiImage();
        setTimestamp();
        setReason();
        setGroup();
        setEmojiDescription();
        loadImage();
        setRoundedBackground();
    }

    /**
     * Sets the custom emoji image.
     */
    private void setEmojiImage() {
        if (emoji != null) {
            int emojiResId = getResources().getIdentifier(emoji, "drawable", getPackageName());
            if (emojiResId != 0) {
                emojiView.setImageResource(emojiResId);
            } else {
                Log.e(TAG, "Could not find drawable resource for emoji: " + emoji);
            }
        }
    }

    /**
     * Sets the formatted timestamp.
     */
    private void setTimestamp() {
        if (timestamp != null) {
            String formattedTime = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault()).format(timestamp.toDate());
            timeView.setText(formattedTime);
        } else {
            timeView.setText("Unknown time");
        }
    }

    /**
     * Sets the mood reason.
     */
    private void setReason() {
        reasonView.setText(reason != null ? reason : "No reason provided");
    }

    /**
     * Sets the group context.
     */
    private void setGroup() {
        groupView.setText(group != null ? group : "No group provided");
    }

    /**
     * Sets the emoji description.
     */
    private void setEmojiDescription() {
        emojiDescView.setText(emojiDescription != null ? emojiDescription : "No emoji");
    }

    /**
     * Loads the image from the URL using Glide.
     */
    private void loadImage() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageUrlView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Loading image from URL: " + imageUrl);

            Glide.with(this)
                    .load(imageUrl)
                    .into(imageUrlView);
        } else {
            Log.d(TAG, "No image URL provided, hiding ImageView");
            imageUrlView.setVisibility(View.GONE);
        }
    }

    /**
     * Applies a rounded background to the emoji rectangle.
     */
    private void setRoundedBackground() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(2, Color.BLACK);
        emojiRectangle.setBackground(gradientDrawable);
    }

    /**
     * Sets up the back button click listener.
     */
    private void setupBackButton() {
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("isMyMoods", getIntent().getBooleanExtra("isMyMoods", true));
            setResult(RESULT_OK, intent);
            finish();
        });
    }
}