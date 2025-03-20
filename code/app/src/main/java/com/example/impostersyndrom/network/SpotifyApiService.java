package com.example.impostersyndrom.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface SpotifyApiService {
    @GET("recommendations")
    Call<RecommendationResponse> getRecommendations(
            @Header("Authorization") String authorization,
            @Query("seed_genres") String seedGenres,
            @Query("target_valence") float targetValence,
            @Query("target_energy") float targetEnergy,
            @Query("target_danceability") float targetDanceability,
            @Query("limit") int limit
    );
}