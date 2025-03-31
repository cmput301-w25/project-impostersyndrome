package com.example.impostersyndrom.spotify;

import android.util.Log;

import com.example.impostersyndrom.network.SpotifyApiService;
import com.example.impostersyndrom.network.SpotifyRecommendationResponse;

import okhttp3.Credentials;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Manages Spotify API interactions including authentication and music recommendations.
 * Implements singleton pattern to maintain a single instance throughout the app.
 *
 * @author Roshan
 */
public class SpotifyManager {

    private static final String TAG = "SpotifyManager";
    private static SpotifyManager instance;

    // Spotify credentials
    private String clientId;
    private String clientSecret;
    private String accessToken;

    // Retrofit instances
    private Retrofit authRetrofit;
    private Retrofit apiRetrofit;
    private SpotifyApiService spotifyApiService;

    /**
     * Interface for Spotify authentication service.
     */
    interface SpotifyAuthService {
        @FormUrlEncoded
        @POST("api/token")
        Call<SpotifyAuthResponse> getAccessToken(
                @Header("Authorization") String authorization,
                @Field("grant_type") String grantType
        );
    }

    /**
     * Private constructor for singleton pattern.
     */
    private SpotifyManager() {
        // Private constructor for singleton
    }

    /**
     * Gets the singleton instance of SpotifyManager.
     * @return The singleton instance
     */
    public static SpotifyManager getInstance() {
        if (instance == null) {
            instance = new SpotifyManager();
        }
        return instance;
    }

    /**
     * Initializes the SpotifyManager with client credentials.
     * @param clientId The Spotify API client ID
     * @param clientSecret The Spotify API client secret
     */
    public void initialize(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;

        // Initialize Retrofit for authentication
        authRetrofit = new Retrofit.Builder()
                .baseUrl("https://accounts.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Initialize Retrofit for API calls
        apiRetrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        spotifyApiService = apiRetrofit.create(SpotifyApiService.class);

        // Fetch access token
        fetchAccessToken();
    }

    /**
     * Fetches an access token from Spotify's authentication service.
     */
    private void fetchAccessToken() {
        SpotifyAuthService authService = authRetrofit.create(SpotifyAuthService.class);
        String credentials = Credentials.basic(clientId, clientSecret);
        Call<SpotifyAuthResponse> call = authService.getAccessToken(credentials, "client_credentials");

        call.enqueue(new Callback<SpotifyAuthResponse>() {
            @Override
            public void onResponse(Call<SpotifyAuthResponse> call, Response<SpotifyAuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    accessToken = response.body().access_token;
                    Log.d(TAG, "Spotify access token fetched: " + accessToken);
                } else {
                    Log.e(TAG, "Spotify auth failed: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<SpotifyAuthResponse> call, Throwable t) {
                Log.e(TAG, "Spotify auth error: " + t.getMessage());
            }
        });
    }

    /**
     * Gets the current access token.
     * @return The access token, or null if not yet fetched
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Fetches music recommendations from Spotify based on audio features.
     * @param genre The seed genre for recommendations
     * @param valence The target valence (0-1)
     * @param energy The target energy (0-1)
     * @param callback The callback to handle the response or failure
     */
    public void fetchRecommendations(String genre, float valence, float energy, Callback<SpotifyRecommendationResponse> callback) {
        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Access token is null or empty");
            return;
        }

        String authHeader = "Bearer " + accessToken;
        Call<SpotifyRecommendationResponse> call = spotifyApiService.getRecommendations(
                authHeader,
                genre,
                "3t5xRXzsuZmMDkQzgY2RtW", // Seed artist ID
                valence,
                energy,
                50
        );

        call.enqueue(new Callback<SpotifyRecommendationResponse>() {
            @Override
            public void onResponse(Call<SpotifyRecommendationResponse> call, Response<SpotifyRecommendationResponse> response) {
                Log.d(TAG, "Recommendation API response code: " + response.code());
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<SpotifyRecommendationResponse> call, Throwable t) {
                Log.e(TAG, "Recommendation fetch error: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }

    /**
     * Searches for tracks matching a specific genre.
     * @param genre The genre to search for
     * @param callback The callback to handle the response or failure
     */
    public void searchTracks(String genre, Callback<SpotifyApiService.SearchResponse> callback) {
        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Access token is null or empty");
            return;
        }

        String authHeader = "Bearer " + accessToken;
        String query = "genre:" + genre;
        Call<SpotifyApiService.SearchResponse> call = spotifyApiService.searchTracks(authHeader, query, "track", 50);

        call.enqueue(new Callback<SpotifyApiService.SearchResponse>() {
            @Override
            public void onResponse(Call<SpotifyApiService.SearchResponse> call, Response<SpotifyApiService.SearchResponse> response) {
                Log.d(TAG, "Search API response code: " + response.code());
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<SpotifyApiService.SearchResponse> call, Throwable t) {
                Log.e(TAG, "Search error: " + t.getMessage());
                callback.onFailure(call, t);
            }
        });
    }
}