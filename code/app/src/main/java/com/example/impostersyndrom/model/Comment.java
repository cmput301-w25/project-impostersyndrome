package com.example.impostersyndrom.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * The Comment class represents a comment on a mood.
 * It stores the comment text, the associated moodId,
 * the user who wrote the comment (userId and username),
 * the timestamp when the comment was posted, and optionally
 * a parentId if this comment is a reply to another comment.
 */
public class Comment implements Serializable {
    private String id;
    private String moodId;    // ID of the mood this comment is associated with
    private String userId;
    private String username;  // Display name of the user
    private String text;
    private Date timestamp;
    private String parentId;  // If null, this is a top-level comment; otherwise, it's a reply

    // Default constructor required for Firestore deserialization
    public Comment() {}

    /**
     * Constructor for a top-level comment.
     * @param moodId The ID of the mood.
     * @param userId The ID of the user posting the comment.
     * @param username The display name of the user.
     * @param text The comment text.
     * @param timestamp The time when the comment was posted.
     */
    public Comment(String moodId, String userId, String username, String text, Date timestamp) {
        this.id = UUID.randomUUID().toString();
        this.moodId = moodId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
        this.parentId = null;
    }

    /**
     * Constructor for a reply comment.
     * @param moodId The ID of the mood.
     * @param userId The ID of the user posting the reply.
     * @param username The display name of the user.
     * @param text The reply text.
     * @param timestamp The time when the reply was posted.
     * @param parentId The ID of the comment being replied to.
     */
    public Comment(String moodId, String userId, String username, String text, Date timestamp, String parentId) {
        this.id = UUID.randomUUID().toString();
        this.moodId = moodId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
        this.parentId = parentId;
    }

    // Getters and setters

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getMoodId() {
        return moodId;
    }
    public void setMoodId(String moodId) {
        this.moodId = moodId;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getParentId() {
        return parentId;
    }
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
