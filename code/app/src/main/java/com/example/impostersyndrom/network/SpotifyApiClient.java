package com.example.impostersyndrom.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SpotifyApiClient {
    private static final String BASE_URL = "https://api.spotify.com/v1/";
    private static Retrofit retrofit;

    public static SpotifyApiService getApiService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(SpotifyApiService.class);
    }
}