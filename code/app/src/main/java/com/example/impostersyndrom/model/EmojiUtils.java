package com.example.impostersyndrom.model;

import android.graphics.Color;
import com.example.impostersyndrom.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing emoji data, including descriptions, colors, and drawable IDs.
 *
 * @author [Your Name]
 * @author Roshan
 */
public class EmojiUtils {

    private static final List<EmojiData> emojiList = new ArrayList<>();

    static {
        // Initialize the list with emoji data
        // Implementation details omitted for brevity
    }

    /**
     * Retrieves the description for a given emoji key.
     *
     * @param emojiKey The key of the emoji (e.g., "emoji_happy")
     * @return The description of the emoji (e.g., "Happy"), or an empty string if no match is found
     */
    public static String getDescription(String emojiKey) {
        // Implementation details omitted for brevity
        return "";
    }

    /**
     * Retrieves the color for a given emoji key.
     *
     * @param emojiKey The key of the emoji (e.g., "emoji_happy")
     * @return The color associated with the emoji, or white if no match is found
     */
    public static int getColor(String emojiKey) {
        // Implementation details omitted for brevity
        return Color.WHITE;
    }

    /**
     * Retrieves the emoji key for a given description.
     *
     * @param description The description of the emoji (e.g., "Happy")
     * @return The key of the emoji (e.g., "emoji_happy"), or an empty string if no match is found
     */
    public static String getEmojiKey(String description) {
        // Implementation details omitted for brevity
        return "";
    }

    /**
     * Retrieves the drawable ID for a given emoji key.
     *
     * @param emojiKey The key of the emoji (e.g., "emoji_happy")
     * @return The drawable resource ID for the emoji, or 0 if no match is found
     */
    public static int getEmojiDrawableId(String emojiKey) {
        // Implementation details omitted for brevity
        return 0;
    }

    /**
     * Retrieves the descriptions of all emojis in the defined order.
     *
     * @return An array of emoji descriptions
     */
    public static String[] getEmojiDescriptions() {
        // Implementation details omitted for brevity
        return new String[0];
    }

    /**
     * Inner class to hold emoji data: key, description, and color.
     */
    private static class EmojiData {
        private final String key;
        private final String description;
        private final int color;

        /**
         * Constructs a new EmojiData instance.
         *
         * @param key The key of the emoji
         * @param description The description of the emoji
         * @param color The color associated with the emoji
         */
        public EmojiData(String key, String description, int color) {
            this.key = key;
            this.description = description;
            this.color = color;
        }

        /**
         * Retrieves the key of the emoji.
         *
         * @return The emoji key
         */
        public String getKey() {
            return key;
        }

        /**
         * Retrieves the description of the emoji.
         *
         * @return The emoji description
         */
        public String getDescription() {
            return description;
        }

        /**
         * Retrieves the color associated with the emoji.
         *
         * @return The emoji color
         */
        public int getColor() {
            return color;
        }
    }
}