package com.example.impostersyndrom.model;

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;

public class EmojiUtils {

    // List to store emoji data in a specific order
    private static final List<EmojiData> emojiList = new ArrayList<>();

    static {
        // Initialize the list with emoji data
        emojiList.add(new EmojiData("emoji_happy", "Happy", Color.parseColor("#FFCC00"))); // Happy
        emojiList.add(new EmojiData("emoji_confused", "Confused", Color.parseColor("#8B7355"))); // Confused
        emojiList.add(new EmojiData("emoji_disgust", "Disgust", Color.parseColor("#808000"))); // Disgust
        emojiList.add(new EmojiData("emoji_angry", "Angry", Color.parseColor("#FF4D00"))); // Angry
        emojiList.add(new EmojiData("emoji_sad", "Sad", Color.parseColor("#2980B9"))); // Sad
        emojiList.add(new EmojiData("emoji_fear", "Fear", Color.parseColor("#9B59B6"))); // Fear
        emojiList.add(new EmojiData("emoji_shame", "Shame", Color.parseColor("#C64B70"))); // Shame
        emojiList.add(new EmojiData("emoji_surprised", "Surprise", Color.parseColor("#1ABC9C"))); // Surprise
    }

    /**
     * Gets the description for a given emoji key.
     *
     * @param emojiKey The key of the emoji (e.g., "emoji_happy").
     * @return The description of the emoji (e.g., "Happy").
     */
    public static String getDescription(String emojiKey) {
        for (EmojiData emojiData : emojiList) {
            if (emojiData.getKey().equals(emojiKey)) {
                return emojiData.getDescription();
            }
        }
        return ""; // Return empty string if no match is found
    }

    /**
     * Gets the color for a given emoji key.
     *
     * @param emojiKey The key of the emoji (e.g., "emoji_happy").
     * @return The color associated with the emoji.
     */
    public static int getColor(String emojiKey) {
        for (EmojiData emojiData : emojiList) {
            if (emojiData.getKey().equals(emojiKey)) {
                return emojiData.getColor();
            }
        }
        return Color.WHITE; // Return default color if no match is found
    }

    /**
     * Gets all emoji keys.
     *
     * @return An array of all emoji keys.
     */
    public static String getEmojiKey(String description) {
        for (EmojiData emojiData : emojiList) {
            if (emojiData.getDescription().equals(description)) {
                return emojiData.getKey();
            }
        }
        return ""; // Return empty string if no match is found
    }

    /**
     * Gets the descriptions of all emojis in the correct order.
     *
     * @return An array of emoji descriptions.
     */
    public static String[] getEmojiDescriptions() {
        String[] descriptions = new String[emojiList.size()];
        for (int i = 0; i < emojiList.size(); i++) {
            descriptions[i] = emojiList.get(i).getDescription();
        }
        return descriptions;
    }

    /**
     * A class to hold emoji data: key, description, and color.
     */
    private static class EmojiData {
        private final String key; // Key of the emoji
        private final String description; // Description of the emoji
        private final int color; // Color associated with the emoji

        public EmojiData(String key, String description, int color) {
            this.key = key;
            this.description = description;
            this.color = color;
        }

        public String getKey() {
            return key;
        }

        public String getDescription() {
            return description;
        }

        public int getColor() {
            return color;
        }
    }
}