package com.example.impostersyndrom.model;

/**
 * The User class represents a singleton instance of the current user in the application.
 * It stores the user's unique ID and provides methods to access and modify it.
 * This class follows the Singleton design pattern to ensure only one instance exists throughout the app.
 *
 * @author Ali Zain
 */
public class User {
    private static User instance = null; // Singleton instance of the User class
    private String userId = null; // The unique ID of the user

    /**
     * Private constructor to prevent instantiation from outside the class.
     * This ensures that the class follows the Singleton pattern.
     */
    private User() {}

    /**
     * Returns the singleton instance of the User class.
     * If the instance does not exist, it creates a new one.
     *
     * @return The singleton instance of the User class.
     */
    public static User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    /**
     * Returns the unique ID of the user.
     *
     * @return The user's unique ID, or null if not set.
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Sets the unique ID of the user.
     *
     * @param userId The unique ID to set for the user.
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }
}