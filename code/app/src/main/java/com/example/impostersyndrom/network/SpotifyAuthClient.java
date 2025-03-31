package com.example.impostersyndrom.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Client class for handling Spotify authentication API requests.
 * This class provides methods to get the SpotifyAuthService instance and client credentials.
 * @author Roshan
 */
public class SpotifyAuthClient {
    private static final String BASE_URL = "https://accounts.spotify.com/";
    private static final String CLIENT_ID = "ae52ad97cfd5446299f8883b4a6a6236";
    private static final String CLIENT_SECRET = "b40c6d9bfabd4f6592f7fb3210ca2f59";

    private static Retrofit retrofit;

    /**
     * Gets the SpotifyAuthService instance for making authentication requests.
     *
     * @return An implementation of SpotifyAuthService interface
     */
    public static SpotifyAuthService getAuthService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SpotifyAuthService.class);
    }

    /**
     * Gets the Spotify client ID.
     *
     * @return The client ID string
     */
    public static String getClientId() {
        return CLIENT_ID;
    }

    /**
     * Gets the Spotify client secret.
     *
     * @return The client secret string
     */
    public static String getClientSecret() {
        return CLIENT_SECRET;
    }
}