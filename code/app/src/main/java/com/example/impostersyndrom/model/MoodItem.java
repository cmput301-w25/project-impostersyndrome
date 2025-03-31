package com.example.impostersyndrom.model;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Represents a single mood item with associated metadata extracted from a Firestore document.
 *
 * @author Roshan
 */
public class MoodItem {
    private final DocumentSnapshot moodDoc;
    private final String username;

    /**
     * Constructs a new MoodItem.
     *
     * @param moodDoc The Firestore document snapshot containing mood data
     * @param username The username associated with the mood
     */
    public MoodItem(DocumentSnapshot moodDoc, String username) {
        this.moodDoc = moodDoc != null ? moodDoc : null;
        this.username = username != null ? username : "Unknown User";
    }

    /**
     * Retrieves the Firestore document snapshot for this mood.
     *
     * @return The mood document snapshot, or null if not available
     */
    public DocumentSnapshot getMoodDoc() {
        return moodDoc;
    }

    /**
     * Retrieves the username associated with this mood.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Retrieves the reason for this mood.
     *
     * @return The reason, or "No reason provided" if not available
     */
    public String getReason() {
        return moodDoc != null && moodDoc.contains("reason") ? moodDoc.getString("reason") : "No reason provided";
    }

    /**
     * Retrieves the emotional state of this mood.
     *
     * @return The emotional state, or "default_emoji" if not available
     */
    public String getEmotionalState() {
        return moodDoc != null && moodDoc.contains("emotionalState") ? moodDoc.getString("emotionalState") : "default_emoji";
    }

    /**
     * Retrieves the timestamp of this mood.
     *
     * @return The timestamp, or null if not available
     */
    public Timestamp getTimestamp() {
        return moodDoc != null && moodDoc.contains("timestamp") ? moodDoc.getTimestamp("timestamp") : null;
    }

    /**
     * Retrieves the color associated with this mood.
     *
     * @return The color as an integer, or light gray if not available
     */
    public int getColor() {
        return moodDoc != null && moodDoc.contains("color")
                ? moodDoc.getLong("color").intValue()
                : android.graphics.Color.LTGRAY;
    }

    /**
     * Retrieves the social situation (group context) of this mood.
     *
     * @return The social situation, or "Alone" if not specified or empty
     */
    public String getSocialSituation() {
        // Implementation details omitted for brevity
        return "Alone";
    }
}