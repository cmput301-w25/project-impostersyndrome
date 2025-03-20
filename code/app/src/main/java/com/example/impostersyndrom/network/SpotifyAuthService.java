package com.example.impostersyndrom.network;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface SpotifyAuthService {
    @FormUrlEncoded
    @POST("api/token")
    Call<SpotifyAuthResponse> getAccessToken(
            @Header("Authorization") String authorization,
            @Field("grant_type") String grantType
    );
}