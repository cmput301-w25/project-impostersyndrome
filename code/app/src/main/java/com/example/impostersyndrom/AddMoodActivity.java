package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddMoodActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood); // Set the layout for this activity

        // Initialize views
        TextView emojiView = findViewById(R.id.emojiView);
        TextView emojiDescription = findViewById(R.id.emojiDescription);
        TextView timeView = findViewById(R.id.dateTimeView);
        LinearLayout emojiRectangle = findViewById(R.id.emojiRectangle);

        // Retrieve the selected emoji and color from the intent
        Intent intent = getIntent();
        String emoji = intent.getStringExtra("emoji");
        int color = intent.getIntExtra("color", Color.BLACK);
        String emojiDesc = getEmojiDescription(emoji); // Get the emoji description

        // Display the emoji and description
        emojiView.setText(emoji);
        emojiDescription.setText(emojiDesc);

        // Set the current time
        String currentTime = new SimpleDateFormat("dd-MM-YYYY | HH:mm", Locale.getDefault()).format(new Date());
        timeView.setText(currentTime);

        // Set the background color, rounded corners, and border for the rectangle
        setRoundedBackground(emojiRectangle, color);
    }

    // Helper method to get the emoji description
    private String getEmojiDescription(String emoji) {
        switch (emoji) {
            case "\uD83D\uDE01": // Happy
                return "Happy";
            case "\uD83E\uDD14": // Confused
                return "Confused";
            case "\uD83E\uDD22": // Disgust
                return "Disgust";
            case "\uD83D\uDE21": // Angry
                return "Angry";
            case "\uD83D\uDE14": // Sad
                return "Sad";
            case "\uD83D\uDE28": // Fear
                return "Fear";
            case "\uD83D\uDE05": // Shame
                return "Shame";
            case "\uD83D\uDE32": // Surprise
                return "Surprise";
            default:
                return "Mood";
        }
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