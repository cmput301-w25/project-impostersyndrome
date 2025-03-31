package com.example.impostersyndrom.controller;

import android.graphics.Color;

import com.example.impostersyndrom.R;

/**
 * EditEmojiResources is a utility class that provides methods to retrieve
 * mood-related resources such as colors, descriptions, and drawable resources
 * based on the emoji name.
 *
 * @author Rayan
 */
public class EditEmojiResources {

    /**
     * Returns the color associated with a specific emoji name.
     *
     * @param emojiName The name of the emoji (e.g., "emoji_happy").
     * @return The color associated with the emoji, or gray if the emoji is unknown.
     */
    public static int getMoodColor(String emojiName) {
        if (emojiName == null) return Color.GRAY; // Default color if unknown

        switch (emojiName.toLowerCase()) {
            case "emoji_happy": return Color.parseColor("#FFCC00"); // Yellow
            case "emoji_sad": return Color.parseColor("#2980B9"); // Blue
            case "emoji_angry": return Color.parseColor("#FF4D00"); // Orange-Red
            case "emoji_confused": return Color.parseColor("#8B7355"); // Brown
            case "emoji_surprised": return Color.parseColor("#1ABC9C"); // Teal
            case "emoji_fear": return Color.parseColor("#9B59B6"); // Purple
            case "emoji_disgust": return Color.parseColor("#808000"); // Olive
            case "emoji_shame": return Color.parseColor("#C64B70"); // Dark Pink
            default: return Color.GRAY; // Fallback color
        }
    }

    /**
     * Converts an emoji name into a human-readable mood description.
     *
     * @param emoji The name of the emoji (e.g., "emoji_happy").
     * @return A human-readable mood description, or "Unknown Mood" if the emoji is unknown.
     */
    public static String getReadableMood(String emoji) {
        if (emoji == null) return "Unknown Mood";

        switch (emoji.toLowerCase()) {
            case "emoji_happy": return "Happy";
            case "emoji_sad": return "Sad";
            case "emoji_angry": return "Angry";
            case "emoji_confused": return "Confused";
            case "emoji_surprised": return "Surprised";
            case "emoji_fear": return "Fearful";
            case "emoji_disgust": return "Disgusted";
            case "emoji_shame": return "Ashamed";
            default: return "Unknown Mood"; // Fallback
        }
    }

    /**
     * Returns the drawable resource ID associated with a specific emoji name.
     *
     * @param emojiName The name of the emoji (e.g., "emoji_happy").
     * @return The drawable resource ID for the emoji, or a default emoji if the emoji is unknown.
     */
    public static int getEmojiResource(String emojiName) {
        if (emojiName == null) return R.drawable.emoji_confused; // Default emoji if unknown

        switch (emojiName.toLowerCase()) {
            case "emoji_happy": return R.drawable.emoji_happy;
            case "emoji_sad": return R.drawable.emoji_sad;
            case "emoji_angry": return R.drawable.emoji_angry;
            case "emoji_confused": return R.drawable.emoji_confused;
            case "emoji_surprised": return R.drawable.emoji_surprised;
            case "emoji_fear": return R.drawable.emoji_fear;
            case "emoji_disgust": return R.drawable.emoji_disgust;
            case "emoji_shame": return R.drawable.emoji_shame;
            default: return R.drawable.emoji_confused; // Default if emoji name is unrecognized
        }
    }
}