package com.example.impostersyndrom.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

public interface SpotifyApiService {
    @GET("v1/recommendations")
    Call<SpotifyRecommendationResponse> getRecommendations(
            @Header("Authorization") String authorization,
            @Query("seed_genres") String seedGenres,
            @Query("target_valence") float targetValence,
            @Query("target_energy") float targetEnergy,
            @Query("limit") int limit
    );
}