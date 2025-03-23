package com.example.impostersyndrom.model;

import android.util.Log;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

public class MoodItem {
    private final DocumentSnapshot moodDoc;
    private final String username;

    public MoodItem(DocumentSnapshot moodDoc, String username) {
        this.moodDoc = moodDoc != null ? moodDoc : null;
        this.username = username != null ? username : "Unknown User";
    }

    public DocumentSnapshot getMoodDoc() {
        return moodDoc;
    }

    public String getUsername() {
        return username;
    }

    public String getReason() {
        return moodDoc != null && moodDoc.contains("reason") ? moodDoc.getString("reason") : "No reason provided";
    }

    public String getEmotionalState() {
        return moodDoc != null && moodDoc.contains("emotionalState") ? moodDoc.getString("emotionalState") : "default_emoji";
    }

    public Timestamp getTimestamp() {
        return moodDoc != null && moodDoc.contains("timestamp") ? moodDoc.getTimestamp("timestamp") : null;
    }

    public int getColor() {
        return moodDoc != null && moodDoc.contains("color")
                ? moodDoc.getLong("color").intValue()
                : android.graphics.Color.LTGRAY; // Default color
    }

    public String getSocialSituation() {
        String socialSituation = moodDoc != null && moodDoc.contains("group") ? moodDoc.getString("group") : null;
        // Default to "Alone" if the group field is null or empty
        String result = (socialSituation == null || socialSituation.trim().isEmpty()) ? "Alone" : socialSituation;
        Log.d("MoodItem", "Social Situation for mood: " + (moodDoc != null ? moodDoc.getId() : "null") + " is " + result);
        return result;
    }
}