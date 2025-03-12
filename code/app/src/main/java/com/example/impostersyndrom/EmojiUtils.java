package com.example.impostersyndrom;

import android.graphics.Color;
import java.util.HashMap;
import java.util.Map;

public class EmojiUtils {

    // Map to store emoji keys, descriptions, and colors
    private static final Map<String, Pair<String, Integer>> emojiMap = new HashMap<>();

    static {
        // Initialize the map with emoji data
        emojiMap.put("emoji_happy", new Pair<>("Happy", Color.parseColor("#FFCC00"))); // Happy
        emojiMap.put("emoji_confused", new Pair<>("Confused", Color.parseColor("#8B7355"))); // Confused
        emojiMap.put("emoji_disgust", new Pair<>("Disgust", Color.parseColor("#808000"))); // Disgust
        emojiMap.put("emoji_angry", new Pair<>("Angry", Color.parseColor("#FF4D00"))); // Angry
        emojiMap.put("emoji_sad", new Pair<>("Sad", Color.parseColor("#2980B9"))); // Sad
        emojiMap.put("emoji_fear", new Pair<>("Fear", Color.parseColor("#9B59B6"))); // Fear
        emojiMap.put("emoji_shame", new Pair<>("Shame", Color.parseColor("#C64B70"))); // Shame
        emojiMap.put("emoji_surprised", new Pair<>("Surprise", Color.parseColor("#1ABC9C"))); // Surprise
    }

    /**
     * Gets the description for a given emoji key.
     *
     * @param emojiKey The key of the emoji (e.g., "emoji_happy").
     * @return The description of the emoji (e.g., "Happy").
     */
    public static String getDescription(String emojiKey) {
        Pair<String, Integer> pair = emojiMap.get(emojiKey);
        return pair != null ? pair.desc : "";
    }

    /**
     * Gets the color for a given emoji key.
     *
     * @param emojiKey The key of the emoji (e.g., "emoji_happy").
     * @return The color associated with the emoji.
     */
    public static int getColor(String emojiKey) {
        Pair<String, Integer> pair = emojiMap.get(emojiKey);
        return pair != null ? pair.color : Color.WHITE;
    }

    /**
     * Gets all emoji keys.
     *
     * @return A list of all emoji keys.
     */
    public static String[] getEmojiKeys() {
        return emojiMap.keySet().toArray(new String[0]);
    }

    /**
     * Gets all emoji descriptions.
     *
     * @return A list of all emoji descriptions.
     */
    public static String[] getEmojiDescriptions() {
        return emojiMap.values().stream().map(pair -> pair.desc).toArray(String[]::new);
    }

    /**
     * A simple Pair class to hold two related values: description and color.
     *
     * @param <F> The type of the first value (description).
     * @param <S> The type of the second value (color).
     */
    public static class Pair<F, S> {
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

    /**
     * Gets the emoji map.
     *
     * @return The map of emoji keys to their descriptions and colors.
     */
    public static Map<String, Pair<String, Integer>> getEmojiMap() {
        return emojiMap;
    }

    /**
     * Gets the emoji key for a given description.
     *
     * @param description The description of the emoji (e.g., "Happy").
     * @return The emoji key (e.g., "emoji_happy").
     */
    public static String getEmojiKey(String description) {
        for (Map.Entry<String, Pair<String, Integer>> entry : emojiMap.entrySet()) {
            if (entry.getValue().desc.equals(description)) {
                return entry.getKey();
            }
        }
        return ""; // Return empty string if no match is found
    }
}