package com.example.impostersyndrom.model;

/**
 * Represents a follow request between two users with associated metadata.
 *
 * @author [Your Name]
 */
public class FollowRequest {
    private String senderId;
    private String receiverId;
    private String senderUsername;
    private String receiverUsername;
    private String status;

    /**
     * Default constructor for Firebase deserialization.
     */
    public FollowRequest() {}

    /**
     * Constructs a new FollowRequest with the specified details.
     *
     * @param senderId The ID of the user sending the request
     * @param receiverId The ID of the user receiving the request
     * @param senderUsername The username of the sender
     * @param receiverUsername The username of the receiver
     * @param status The status of the request (e.g., "pending", "accepted")
     */
    public FollowRequest(String senderId, String receiverId, String senderUsername, String receiverUsername, String status) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.senderUsername = senderUsername;
        this.receiverUsername = receiverUsername;
        this.status = status;
    }

    /**
     * Retrieves the ID of the sender.
     *
     * @return The sender's ID
     */
    public String getSenderId() { return senderId; }

    /**
     * Retrieves the ID of the receiver.
     *
     * @return The receiver's ID
     */
    public String getReceiverId() { return receiverId; }

    /**
     * Retrieves the username of the sender.
     *
     * @return The sender's username
     */
    public String getSenderUsername() { return senderUsername; }

    /**
     * Retrieves the username of the receiver.
     *
     * @return The receiver's username
     */
    public String getReceiverUsername() { return receiverUsername; }

    /**
     * Retrieves the status of the follow request.
     *
     * @return The request status
     */
    public String getStatus() { return status; }
}