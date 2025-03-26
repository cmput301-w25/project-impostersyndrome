package com.example.impostersyndrom.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SpotifyAuthClient {
    private static final String BASE_URL = "https://accounts.spotify.com/";
    private static final String CLIENT_ID = "ae52ad97cfd5446299f8883b4a6a6236"; // Replace with your Client ID
    private static final String CLIENT_SECRET = "b40c6d9bfabd4f6592f7fb3210ca2f59"; // Replace with your Client Secret

    private static Retrofit retrofit;

    public static SpotifyAuthService getAuthService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SpotifyAuthService.class);
    }

    public static String getClientId() {
        return CLIENT_ID;
    }

    public static String getClientSecret() {
        return CLIENT_SECRET;
    }
}