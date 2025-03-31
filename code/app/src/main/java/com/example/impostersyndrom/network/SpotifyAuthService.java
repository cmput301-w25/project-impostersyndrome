package com.example.impostersyndrom.network;

import com.example.impostersyndrom.spotify.SpotifyAuthResponse;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Interface defining the Spotify authentication service API endpoints.
 * This service is used to obtain access tokens from the Spotify Accounts service.
 * @author Roshan
 */
public interface SpotifyAuthService {
    /**
     * Retrieves an access token from the Spotify Accounts service.
     *
     * @param authorization The Base64 encoded client credentials (clientId:clientSecret)
     * @param grantType The grant type to use for authentication (typically "client_credentials")
     * @return A Call object that can be used to execute the request asynchronously
     */
    @FormUrlEncoded
    @POST("api/token")
    Call<SpotifyAuthResponse> getAccessToken(
            @Header("Authorization") String authorization,
            @Field("grant_type") String grantType
    );
}