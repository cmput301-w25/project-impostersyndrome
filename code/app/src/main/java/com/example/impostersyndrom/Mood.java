package com.example.impostersyndrom;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * The Mood class represents a mood entry in the application.
 * It stores information about the user's emotional state, including:
 * - Emotional state (e.g., happy, sad)
 * - Emoji description
 * - Timestamp
 * - Color associated with the mood
 * - Reason for the mood
 * - Group context (e.g., alone, with others)
 * - Optional image URL
 * - User ID
 * - Emoji drawable resource ID
 *
 * This class implements Serializable to allow Mood objects to be passed between activities.
 *
 * @author Roshan Banisetti
 * @author Garrick
 * @author Eric Mo
 */
public class Mood implements Serializable {

    private String emotionalState; // The emotional state (e.g., happy, sad)
    private String emojiDescription; // Description of the emoji representing the mood
    private String id; // Unique ID for the mood entry
    private String reason; // Reason for the mood
    private Date timestamp; // Timestamp of the mood entry
    private int color; // Color associated with the mood
    private String imageUrl; // URL of the optional image associated with the mood
    private String userId; // ID of the user who created the mood entry
    private String group; // Group context (e.g., alone, with others)
    private int emojiDrawableId; // Resource ID of the emoji drawable

    /**
     * Constructor for the Mood class.
     *
     * @param emotionalState   The emotional state (e.g., happy, sad).
     * @param emojiDescription Description of the emoji representing the mood.
     * @param timestamp        Timestamp of the mood entry.
     * @param color            Color associated with the mood.
     * @param reason           Reason for the mood.
     */
    public Mood(String emotionalState, String emojiDescription, Date timestamp, int color, String reason) {
        this.id = UUID.randomUUID().toString(); // Generate a unique ID for the mood
        this.emotionalState = emotionalState;
        this.emojiDescription = emojiDescription;
        this.timestamp = timestamp;
        this.color = color;
        this.reason = reason;
        this.group = "alone"; // Default group context is "alone"
        this.imageUrl = null; // Initialize image URL to null
    }

    // Getters and Setters

    /**
     * Gets the emotional state.
     *
     * @return The emotional state.
     */
    public String getEmotionalState() {
        return emotionalState;
    }

    /**
     * Sets the emotional state.
     *
     * @param emotionalState The emotional state to set.
     */
    public void setEmotionalState(String emotionalState) {
        this.emotionalState = emotionalState;
    }

    /**
     * Gets the emoji description.
     *
     * @return The emoji description.
     */
    public String getEmojiDescription() {
        return emojiDescription;
    }

    /**
     * Sets the emoji description.
     *
     * @param emojiDescription The emoji description to set.
     */
    public void setEmojiDescription(String emojiDescription) {
        this.emojiDescription = emojiDescription;
    }

    /**
     * Gets the timestamp.
     *
     * @return The timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp.
     *
     * @param timestamp The timestamp to set.
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the color associated with the mood.
     *
     * @return The color.
     */
    public int getColor() {
        return color;
    }

    /**
     * Sets the color associated with the mood.
     *
     * @param color The color to set.
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Gets the reason for the mood.
     *
     * @return The reason.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets the reason for the mood.
     *
     * @param reason The reason to set.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Gets the unique ID of the mood entry.
     *
     * @return The ID.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique ID of the mood entry.
     *
     * @param id The ID to set.
     */
    public Mood setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * Gets the group context.
     *
     * @return The group context.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Sets the group context.
     *
     * @param group The group context to set.
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * Gets the image URL.
     *
     * @return The image URL.
     */
    public String getImageUrl() {
        return imageUrl;
    }

    /**
     * Sets the image URL.
     *
     * @param imageUrl The image URL to set.
     */
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * Gets the user ID.
     *
     * @return The user ID.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the user ID.
     *
     * @param userId The user ID to set.
     */
    public Mood setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Gets the emoji drawable resource ID.
     *
     * @return The emoji drawable resource ID.
     */
    public int getEmojiDrawableId() {
        return emojiDrawableId;
    }

    /**
     * Sets the emoji drawable resource ID.
     *
     * @param emojiDrawableId The emoji drawable resource ID to set.
     */
    public void setEmojiDrawableId(int emojiDrawableId) {
        this.emojiDrawableId = emojiDrawableId;
    }
}