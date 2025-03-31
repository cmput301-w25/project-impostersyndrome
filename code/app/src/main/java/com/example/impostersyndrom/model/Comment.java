package com.example.impostersyndrom.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * Represents a comment associated with a mood, including metadata such as user information and replies.
 *
 * @author [Your Name]
 */
public class Comment implements Serializable {
    private String id;
    private String moodId;
    private String userId;
    private String username;
    private String text;
    private Date timestamp;
    private String parentId;
    private int replyCount;

    /**
     * Constructs a new Comment with the specified details for a top-level comment.
     *
     * @param moodId The ID of the mood this comment is associated with
     * @param userId The ID of the user who posted the comment
     * @param username The username of the user who posted the comment
     * @param text The content of the comment
     * @param timestamp The date and time the comment was posted
     */
    public Comment(String moodId, String userId, String username, String text, Date timestamp) {
        this.id = UUID.randomUUID().toString();
        this.moodId = moodId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
        this.parentId = null;
        this.replyCount = 0;
    }

    /**
     * Constructs a new Comment with default values, primarily for Firebase deserialization.
     */
    public Comment() {
        this.id = UUID.randomUUID().toString();
        this.moodId = "";
        this.userId = "";
        this.username = "";
        this.text = "";
        this.timestamp = new Date();
        this.parentId = null;
        this.replyCount = 0;
    }

    /**
     * Constructs a new Comment with the specified details, including a parent ID for replies.
     *
     * @param moodId The ID of the mood this comment is associated with
     * @param userId The ID of the user who posted the comment
     * @param username The username of the user who posted the comment
     * @param text The content of the comment
     * @param timestamp The date and time the comment was posted
     * @param parentId The ID of the parent comment if this is a reply, or null if top-level
     */
    public Comment(String moodId, String userId, String username, String text, Date timestamp, String parentId) {
        this.id = UUID.randomUUID().toString();
        this.moodId = moodId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
        this.parentId = parentId;
        this.replyCount = 0;
    }

    /**
     * Retrieves the unique identifier of the comment.
     *
     * @return The comment's ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the unique identifier of the comment.
     *
     * @param id The ID to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Retrieves the ID of the mood this comment is associated with.
     *
     * @return The mood ID
     */
    public String getMoodId() {
        return moodId;
    }

    /**
     * Sets the ID of the mood this comment is associated with.
     *
     * @param moodId The mood ID to set
     */
    public void setMoodId(String moodId) {
        this.moodId = moodId;
    }

    /**
     * Retrieves the ID of the user who posted the comment.
     *
     * @return The user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the ID of the user who posted the comment.
     *
     * @param userId The user ID to set
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * Retrieves the username of the user who posted the comment.
     *
     * @return The username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username of the user who posted the comment.
     *
     * @param username The username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Retrieves the content of the comment.
     *
     * @return The comment text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the content of the comment.
     *
     * @param text The text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Retrieves the timestamp of when the comment was posted.
     *
     * @return The timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp of when the comment was posted.
     *
     * @param timestamp The timestamp to set
     */
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Retrieves the ID of the parent comment if this is a reply, or null if top-level.
     *
     * @return The parent comment ID, or null
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Sets the ID of the parent comment if this is a reply.
     *
     * @param parentId The parent comment ID to set, or null for top-level
     */
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    /**
     * Retrieves the number of replies to this comment.
     *
     * @return The reply count
     */
    public int getReplyCount() {
        return replyCount;
    }

    /**
     * Sets the number of replies to this comment.
     *
     * @param replyCount The reply count to set
     */
    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }
}