package com.example.impostersyndrom.spotify;

/**
 * Represents the response from Spotify's authentication API.
 * Contains the access token and related authentication information.
 *
 * @author Roshan
 */
public class SpotifyAuthResponse {
    /** The access token for API authorization */
    public String access_token;

    /** The type of token (typically "Bearer") */
    public String token_type;

    /** Duration in seconds until the token expires */
    public int expires_in;

    /** Error code if authentication fails */
    public String error;

    /** Human-readable error description */
    public String error_description;
}