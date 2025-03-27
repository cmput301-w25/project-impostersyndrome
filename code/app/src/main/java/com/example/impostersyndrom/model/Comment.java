package com.example.impostersyndrom.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

/**
 * The Comment class represents a comment on a post.
 * It stores the comment text, the associated postId,
 * the user who wrote the comment (userId and username),
 * and the timestamp when the comment was posted.
 */
public class Comment implements Serializable {
    private String id;
    private String postId;    // ID of the post this comment is associated with
    private String userId;
    private String username;  // Optional: display name of the user
    private String text;
    private Date timestamp;

    public Comment() {
        // Default constructor required for Firestore deserialization
    }

    public Comment(String postId, String userId, String username, String text, Date timestamp) {
        this.id = UUID.randomUUID().toString();
        this.postId = postId;
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getPostId() {
        return postId;
    }
    public void setPostId(String postId) {
        this.postId = postId;
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
}
