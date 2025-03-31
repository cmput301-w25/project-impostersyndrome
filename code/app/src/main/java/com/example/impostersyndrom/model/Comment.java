package com.example.impostersyndrom.model;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;

public class Comment implements Serializable {
    private String id;
    private String moodId;
    private String userId;
    private String username;
    private String text;
    private Date timestamp;
    private String parentId;
    private int replyCount;

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
    // Implemented deserialized constructor with no argument in order to work with Firebase
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

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }
}
