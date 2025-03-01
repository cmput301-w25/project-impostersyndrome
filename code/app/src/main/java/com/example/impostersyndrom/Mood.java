package com.example.impostersyndrom;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Mood implements Serializable {

    private String emotionalState;
    private String emojiDescription;
    private String id;
    private String reason;
    private String trigger; // New field for trigger description
    private Date timestamp;
    private int color;
    private String imageUrl;
    private String userId;
    private String group;
    private int emojiDrawableId;

    // Constructor
    public Mood(String emotionalState, String emojiDescription, Date timestamp, int color, String reason) {
        this.id = UUID.randomUUID().toString();
        this.emotionalState = emotionalState;
        this.emojiDescription = emojiDescription;
        this.timestamp = timestamp;
        this.color = color;
        this.reason = reason;
        this.trigger = ""; // Initialize trigger to empty string
        this.group = "alone"; // Default to alone
        this.imageUrl = null; // Initialize imageUrl to null
    }

    // Add getter and setter for trigger
    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    // Existing getters and setters...
    public String getEmotionalState() {
        return emotionalState;
    }

    public void setEmotionalState(String emotionalState) {
        this.emotionalState = emotionalState;
    }

    public String getEmojiDescription() {
        return emojiDescription;
    }

    public void setEmojiDescription(String emojiDescription) {
        this.emojiDescription = emojiDescription;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getEmojiDrawableId() {
        return emojiDrawableId;
    }

    public void setEmojiDrawableId(int emojiDrawableId) {
        this.emojiDrawableId = emojiDrawableId;
    }
}