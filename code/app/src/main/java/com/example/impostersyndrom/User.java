package com.example.impostersyndrom;

public class User {
    private static User instance = null; // Singleton instance
    private String userId = null;

    // Private constructor to prevent instantiation
    private User() {}

    // Method to get the singleton instance
    public static User getInstance() {
        if (instance == null) {
            instance = new User();
        }
        return instance;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}