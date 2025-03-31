package com.example.impostersyndrom.model;

/**
 * Represents a singleton instance of the current user in the application.
 * Stores the user's unique ID and provides methods to access and modify it.
 * Follows the Singleton design pattern to ensure only one instance exists throughout the app.
 *
 * @author Ali Zain
 */
public class User {
    private static User instance = null; // Singleton instance of the User class
    private String userId = null; // The unique ID of the user

    /**
     * Private constructor to prevent instantiation from outside the class.
     * Ensures the Singleton pattern by restricting direct object creation.
     */
    private User() {}

    /**
     * Retrieves the singleton instance of the User class.
     * Creates a new instance if none exists.
     *
     * @return The singleton instance of the User class
     */
    public static User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    /**
     * Retrieves the unique ID of the user.
     *
     * @return The user's unique ID, or null if not set
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique ID of the user.
     *
     * @param userId The unique ID to set for the user
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
}