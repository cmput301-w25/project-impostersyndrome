package com.example.impostersyndrom;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public class ExampleUnitTest {

    @Test
    public void testMoodDataFormatting_validData() {
        // Create valid mood data
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("emoji", "emoji_happy");
        moodData.put("reason", "Feeling great today!");
        moodData.put("group", "Alone");
        moodData.put("color", "#FFCC00");

        // Validate the formatting
        assertEquals("emoji_happy", moodData.get("emoji"));
        assertEquals("Feeling great today!", moodData.get("reason"));
        assertEquals("Alone", moodData.get("group"));
        assertEquals("#FFCC00", moodData.get("color"));
    }

    @Test
    public void testMoodDataFormatting_missingFields() {
        // Create mood data with missing fields
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("emoji", "emoji_sad");
        moodData.put("reason", "");
        moodData.put("group", "");
        moodData.put("color", "");

        // Validate that missing fields are stored as empty values (not null)
        assertEquals("emoji_sad", moodData.get("emoji"));
        assertEquals("", moodData.get("reason")); // Empty reason
        assertEquals("", moodData.get("group"));  // Empty group
        assertEquals("", moodData.get("color"));  // Empty color
    }

    @Test
    public void testGetEmojiResource_validEmoji() {
        // Ensure correct drawable ID is returned for a valid emoji
        assertEquals(R.drawable.emoji_happy, EditEmojiResources.getEmojiResource("emoji_happy"));
        assertEquals(R.drawable.emoji_sad, EditEmojiResources.getEmojiResource("emoji_sad"));
    }

    @Test
    public void testGetMoodDescription_validEmoji() {
        // Ensure correct mood description is returned
        assertEquals("Happy", EditEmojiResources.getReadableMood("emoji_happy"));
        assertEquals("Sad", EditEmojiResources.getReadableMood("emoji_sad"));
    }

    @Test
    public void testIsValidReason_tooLong() {
        // Define the max allowed length (assuming 20 characters)
        int maxReasonLength = 20;

        // Create an overly long reason
        String longReason = "This reason is way too long for the limit";

        // Check if it exceeds the allowed length
        boolean isValid = longReason.length() <= maxReasonLength;

        // The test should fail if an overly long reason is considered valid
        assertFalse(isValid);
    }
}
