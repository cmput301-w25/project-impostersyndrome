package com.example.impostersyndrom.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Interface defining the Spotify Web API endpoints.
 * This service provides methods to interact with various Spotify API features.
 * @author Roshan
 */
public interface SpotifyApiService {
    /**
     * Gets track recommendations based on specified criteria.
     *
     * @param authorization The access token for authorization
     * @param seedGenres Comma-separated list of genres to use as seeds
     * @param seedArtists Comma-separated list of artist IDs to use as seeds
     * @param targetValence Target valence (musical positiveness) for recommendations
     * @param targetEnergy Target energy level for recommendations
     * @param limit Number of tracks to return
     * @return A Call object that can be used to execute the request asynchronously
     */
    @GET("v1/recommendations")
    Call<SpotifyRecommendationResponse> getRecommendations(
            @Header("Authorization") String authorization,
            @Query("seed_genres") String seedGenres,
            @Query("seed_artists") String seedArtists,
            @Query("target_valence") float targetValence,
            @Query("target_energy") float targetEnergy,
            @Query("limit") int limit
    );

    /**
     * Searches for tracks on Spotify.
     *
     * @param authorization The access token for authorization
     * @param query The search query string
     * @param type The type of items to search for (e.g., "track")
     * @param limit Number of items to return
     * @return A Call object that can be used to execute the request asynchronously
     */
    @GET("v1/search")
    Call<SearchResponse> searchTracks(
            @Header("Authorization") String authorization,
            @Query("q") String query,
            @Query("type") String type,
            @Query("limit") int limit
    );

    /**
     * Gets artist information by ID.
     *
     * @param authorization The access token for authorization
     * @param artistId The Spotify artist ID
     * @return A Call object that can be used to execute the request asynchronously
     */
    @GET("v1/artists/{id}")
    Call<ArtistResponse> getArtist(
            @Header("Authorization") String authorization,
            @Path("id") String artistId
    );

    /**
     * Represents the response structure for Spotify search results.
     */
    public static class SearchResponse {
        @SerializedName("tracks")
        public Tracks tracks;

        /**
         * Contains the list of tracks from a search response.
         */
        public static class Tracks {
            @SerializedName("items")
            public List<SpotifyRecommendationResponse.Track> items;
        }
    }

    /**
     * Represents the response structure for artist information.
     */
    public static class ArtistResponse {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;
    }
}