package com.example.impostersyndrom.model;

/**
 * Represents basic user data including username and profile image URL.
 *
 * @author [Your Name]
 */
public class UserData {
    public String username;
    public String profileImageUrl;

    /**
     * Constructs a new UserData instance with the specified username and profile image URL.
     *
     * @param username The username of the user
     * @param profileImageUrl The URL of the user's profile image
     */
    public UserData(String username, String profileImageUrl) {
        this.username = username;
        this.profileImageUrl = profileImageUrl;
    }
}