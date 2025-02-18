package com.example.impostersyndrom;

import java.io.Serializable;
import java.util.Date;

public class Mood implements Serializable {

    private String emotionalState;
    private String emojiDescription;
    private Date timestamp;
    private int color;
    private String reason;

    public Mood(String emotionalState, String emojiDescription, Date timestamp, int color, String reason) {
        this.emotionalState = emotionalState;
        this.emojiDescription = emojiDescription;
        this.timestamp = timestamp;
        this.color = color;
        this.reason = reason;
    }

    // Getters and Setters
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
    public String getReason() {return reason;}

    public void setReason(String reason) {this.reason = reason;}
}

