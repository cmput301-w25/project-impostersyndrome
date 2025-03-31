package com.example.impostersyndrom.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a mood entry with details about the user's emotional state and associated metadata.
 *
 * @author Roshan Banisetti
 * @author Garrick
 * @author Eric Mo
 */
public class Mood implements Serializable {

    private String emotionalState;
    private String emojiDescription;
    private String id;
    private String reason;
    private Date timestamp;
    private int color;
    private String imageUrl;
    private String userId;
    private String group;
    private int emojiDrawableId;
    private boolean privateMood = false;
    private Double latitude;
    private Double longitude;

    /**
     * Default constructor required by Firebase Firestore for deserialization.
     */
    public Mood() {
        // Implementation details omitted for brevity
    }

    /**
     * Constructs a new Mood with the specified details.
     *
     * @param emotionalState The emotional state (e.g., "happy", "sad")
     * @param emojiDescription Description of the emoji representing the mood
     * @param timestamp Timestamp of the mood entry
     * @param color Color associated with the mood
     * @param reason Reason for the mood
     */
    public Mood(String emotionalState, String emojiDescription, Date timestamp, int color, String reason) {
        // Implementation details omitted for brevity
    }

    /**
     * Retrieves the emotional state.
     *
     * @return The emotional state
     */
    public String getEmotionalState() {
        return emotionalState;
    }

    /**
     * Sets the emotional state.
     *
     * @param emotionalState The emotional state to set
     */
    public void setEmotionalState(String emotionalState) {
        this.emotionalState = emotionalState;
    }

    /**
     * Retrieves the emoji description.
     *
     * @return The emoji description
     */
    public String getEmojiDescription() {
        return emojiDescription;
    }

    /**
     * Sets the emoji description.
     *
     * @param emojiDescription The emoji description to set
     */
    public void setEmojiDescription(String emojiDescription) {
        this.emojiDescription = emojiDescription;
    }

    /**
     * Retrieves the timestamp.
     *
     * @return The timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Retrieves the color associated with the mood.
     *
     * @return The color
     */
    public int getColor() {
        return color;
    }

    /**
     * Sets the color associated with the mood.
     *
     * @param color The color to set
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Retrieves the reason for the mood.
     *
     * @return The reason
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the reason for the mood.
     *
     * @param reason The reason to set
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Retrieves the unique ID of the mood entry.
     *
     * @return The ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique ID of the mood entry and returns the Mood instance for chaining.
     *
     * @param id The ID to set
     * @return This Mood instance
     */
    public Mood setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Retrieves the group context.
     *
     * @return The group context
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the group context.
     *
     * @param group The group context to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Retrieves the image URL.
     *
     * @return The image URL
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the image URL.
     *
     * @param imageUrl The image URL to set
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Retrieves the user ID.
     *
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID and returns the Mood instance for chaining.
     *
     * @param userId The user ID to set
     * @return This Mood instance
     */
    public Mood setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Retrieves the emoji drawable resource ID.
     *
     * @return The emoji drawable resource ID
     */
    public int getEmojiDrawableId() {
        return emojiDrawableId;
    }

    /**
     * Sets the emoji drawable resource ID.
     *
     * @param emojiDrawableId The emoji drawable resource ID to set
     */
    public void setEmojiDrawableId(int emojiDrawableId) {
        this.emojiDrawableId = emojiDrawableId;
    }

    /**
     * Retrieves the privacy status of the mood.
     *
     * @return True if the mood is private, false otherwise
     */
    public boolean isPrivateMood() {
        return privateMood;
    }

    /**
     * Sets the privacy status of the mood.
     *
     * @param privateMood The privacy status to set
     */
    public void setPrivateMood(boolean privateMood) {
        this.privateMood = privateMood;
    }

    /**
     * Retrieves the latitude coordinate.
     *
     * @return The latitude
     */
    public Double getLatitude() {
        return latitude;
    }

    /**
     * Sets the latitude coordinate.
     *
     * @param latitude The latitude to set
     */
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    /**
     * Retrieves the longitude coordinate.
     *
     * @return The longitude
     */
    public Double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude coordinate.
     *
     * @param longitude The longitude to set
     */
    public void setPrivateMood(boolean privateMood) {this.privateMood = privateMood;}

    public void setLocation(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }

        // Validate longitude range (-180 to 180)
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
}

