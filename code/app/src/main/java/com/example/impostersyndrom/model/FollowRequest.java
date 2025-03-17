package com.example.impostersyndrom.model;

public class FollowRequest {
    private String senderId;
    private String receiverId;
    private String senderUsername;
    private String receiverUsername;
    private String status; // "pending", "accepted"

    public FollowRequest() {}

    public FollowRequest(String senderId, String receiverId, String senderUsername, String receiverUsername, String status) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.status = status;
    }

    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public String getSenderUsername() { return senderUsername; }
    public String getReceiverUsername() { return receiverUsername; }
    public String getStatus() { return status; }
}
