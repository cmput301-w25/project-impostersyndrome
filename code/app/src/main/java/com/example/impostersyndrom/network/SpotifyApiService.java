package com.example.impostersyndrom.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface SpotifyApiService {
    @GET("v1/recommendations")
    Call<SpotifyRecommendationResponse> getRecommendations(
            @Header("Authorization") String authorization,
            @Query("seed_genres") String seedGenres,
            @Query("seed_artists") String seedArtists,
            @Query("target_valence") float targetValence,
            @Query("target_energy") float targetEnergy,
            @Query("limit") int limit
    );

    @GET("v1/search")
    Call<SearchResponse> searchTracks(
            @Header("Authorization") String authorization,
            @Query("q") String query,
            @Query("type") String type,
            @Query("limit") int limit
    );

    @GET("v1/artists/{id}")
    Call<ArtistResponse> getArtist(
            @Header("Authorization") String authorization,
            @Path("id") String artistId
    );

    // Response class for search endpoint
    public static class SearchResponse {
        @SerializedName("tracks")
        public Tracks tracks;

        public static class Tracks {
            @SerializedName("items")
            public List<SpotifyRecommendationResponse.Track> items;
        }
    }

    // Response class for artist endpoint
    public static class ArtistResponse {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;
    }
}