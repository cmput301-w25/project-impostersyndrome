package com.example.impostersyndrom.view;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.network.SpotifyApiService;
import com.example.impostersyndrom.network.SpotifyRecommendationResponse;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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

public class MoodDetailActivity extends AppCompatActivity {

    private static final String TAG = "MoodDetailActivity";

    // UI Components
    private ImageView emojiView;
    private TextView timeView;
    private TextView reasonView;
    private TextView emojiDescView;
    private TextView groupView;
    private View emojiRectangle;
    private ImageView imageUrlView;
    private ImageButton backButton;
    private Button recommendSongButton;

    // Mood Data
    private String emoji;
    private Timestamp timestamp;
    private String reason;
    private String group;
    private int color;
    private String emojiDescription;
    private String imageUrl;

    // Spotify Integration
    private String accessToken;
    private SpotifyApiService spotifyApiService;

    // Spotify Authentication (for refreshing token)
    private static final String CLIENT_ID = "ae52ad97cfd5446299f8883b4a6a6236";
    private static final String CLIENT_SECRET = "b40c6d9bfabd4f6592f7fb3210ca2f59";

    // Mood to audio features mapping
    private static final Map<String, MoodAudioFeatures> MOOD_TO_AUDIO_FEATURES = new HashMap<>();

    static {
        // Valence (0.0 to 1.0): Higher = happier, lower = sadder
        // Energy (0.0 to 1.0): Higher = more energetic, lower = more relaxed
        MOOD_TO_AUDIO_FEATURES.put("emoji_happy", new MoodAudioFeatures("pop", 0.8f, 0.7f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_confused", new MoodAudioFeatures("chill", 0.4f, 0.3f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_disgust", new MoodAudioFeatures("rock", 0.3f, 0.5f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_angry", new MoodAudioFeatures("metal", 0.2f, 0.9f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_sad", new MoodAudioFeatures("sad", 0.2f, 0.3f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_fear", new MoodAudioFeatures("ambient", 0.3f, 0.4f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_shame", new MoodAudioFeatures("acoustic", 0.3f, 0.2f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_surprised", new MoodAudioFeatures("dance", 0.7f, 0.8f));
    }

    // Class to hold audio features for each mood
    private static class MoodAudioFeatures {
        String genre;
        float valence;
        float energy;

        MoodAudioFeatures(String genre, float valence, float energy) {
            this.genre = genre;
            this.valence = valence;
            this.energy = energy;
        }
    }

    // Retrofit service for Spotify authentication
    interface SpotifyAuthService {
        @FormUrlEncoded
        @POST("api/token")
        Call<SpotifyAuthResponse> getAccessToken(
                @Header("Authorization") String authorization,
                @Field("grant_type") String grantType
        );
    }

    // Data class for Spotify authentication response
    public static class SpotifyAuthResponse {
        String access_token;
        String token_type;
        int expires_in;
        String error;
        String error_description;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mood_detail);

        // Initialize UI components
        initializeViews();

        // Retrieve data from Intent
        retrieveIntentData();

        // Get Spotify access token from Intent
        accessToken = getIntent().getStringExtra("accessToken");

        // Initialize Retrofit for Spotify Web API
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        spotifyApiService = retrofit.create(SpotifyApiService.class);

        // Set up UI with mood data
        setupUI();

        // Set up back button click listener
        setupBackButton();

        // Set up recommend song button listener
        setupRecommendSongButton();
    }

    /**
     * Initializes all UI components.
     */
    private void initializeViews() {
        emojiView = findViewById(R.id.emojiView);
        timeView = findViewById(R.id.timeView);
        reasonView = findViewById(R.id.reasonView);
        emojiDescView = findViewById(R.id.emojiDescription);
        groupView = findViewById(R.id.groupView);
        emojiRectangle = findViewById(R.id.emojiRectangle);
        imageUrlView = findViewById(R.id.imageUrlView);
        backButton = findViewById(R.id.backButton);
        recommendSongButton = findViewById(R.id.recommendSongButton);
    }

    /**
     * Retrieves mood data from the Intent.
     */
    private void retrieveIntentData() {
        Intent intent = getIntent();
        emoji = intent.getStringExtra("emoji");
        timestamp = (Timestamp) intent.getParcelableExtra("timestamp");
        reason = intent.getStringExtra("reason");
        group = intent.getStringExtra("group");
        color = intent.getIntExtra("color", Color.WHITE);
        emojiDescription = intent.getStringExtra("emojiDescription");
        imageUrl = intent.getStringExtra("imageUrl");

        // Log received data for debugging
        logMoodData();
    }

    /**
     * Logs mood data for debugging purposes.
     */
    private void logMoodData() {
        Log.d(TAG, "Emoji: " + emoji);
        Log.d(TAG, "Reason: " + reason);
        Log.d(TAG, "Group: " + group);
        Log.d(TAG, "Emoji Description: " + emojiDescription);
        Log.d(TAG, "Image URL: " + (imageUrl != null ? imageUrl : "null"));
    }

    /**
     * Sets up the UI with mood data.
     */
    private void setupUI() {
        setEmojiImage();
        setTimestamp();
        setReason();
        setGroup();
        setEmojiDescription();
        loadImage();
        setRoundedBackground();
    }

    /**
     * Sets the custom emoji image.
     */
    private void setEmojiImage() {
        if (emoji != null) {
            int emojiResId = getResources().getIdentifier(emoji, "drawable", getPackageName());
            if (emojiResId != 0) {
                emojiView.setImageResource(emojiResId);
            } else {
                Log.e(TAG, "Could not find drawable resource for emoji: " + emoji);
            }
        }
    }

    /**
     * Sets the formatted timestamp.
     */
    private void setTimestamp() {
        if (timestamp != null) {
            String formattedTime = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault()).format(timestamp.toDate());
            timeView.setText(formattedTime);
        } else {
            timeView.setText("Unknown time");
        }
    }

    /**
     * Sets the mood reason.
     */
    private void setReason() {
        reasonView.setText(reason != null ? reason : "No reason provided");
    }

    /**
     * Sets the group context.
     */
    private void setGroup() {
        groupView.setText(group != null ? group : "No group provided");
    }

    /**
     * Sets the emoji description.
     */
    private void setEmojiDescription() {
        emojiDescView.setText(emojiDescription != null ? emojiDescription : "No emoji");
    }

    /**
     * Loads the image from the URL using Glide.
     */
    private void loadImage() {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageUrlView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Loading image from URL: " + imageUrl);

            Glide.with(this)
                    .load(imageUrl)
                    .into(imageUrlView);
        } else {
            Log.d(TAG, "No image URL provided, hiding ImageView");
            imageUrlView.setVisibility(View.GONE);
        }
    }

    /**
     * Applies a rounded background to the emoji rectangle.
     */
    private void setRoundedBackground() {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(2, Color.BLACK);
        emojiRectangle.setBackground(gradientDrawable);
    }

    /**
     * Sets up the back button click listener.
     */
    private void setupBackButton() {
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.putExtra("isMyMoods", getIntent().getBooleanExtra("isMyMoods", true));
            setResult(RESULT_OK, intent);
            finish();
        });
    }

    /**
     * Sets up the recommend song button click listener.
     */
    private void setupRecommendSongButton() {
        recommendSongButton.setOnClickListener(v -> fetchSongRecommendationWithRetry());
    }

    /**
     * Fetches a song recommendation with retry logic if the access token is not yet available.
     */
    private void fetchSongRecommendationWithRetry() {
        if (accessToken == null) {
            showToast("Spotify authentication in progress. Retrying...");
            fetchSpotifyAccessToken(); // Fetch a new token
            new Handler(Looper.getMainLooper()).postDelayed(this::fetchSongRecommendation, 2000); // Wait 2 seconds before retrying
        } else {
            fetchSongRecommendation();
        }
    }

    /**
     * Fetches a new Spotify access token if the current one is invalid or expired.
     */
    private void fetchSpotifyAccessToken() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://accounts.spotify.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        SpotifyAuthService authService = retrofit.create(SpotifyAuthService.class);

        String credentials = Credentials.basic(CLIENT_ID, CLIENT_SECRET);
        Call<SpotifyAuthResponse> call = authService.getAccessToken(credentials, "client_credentials");

        call.enqueue(new Callback<SpotifyAuthResponse>() {
            @Override
            public void onResponse(Call<SpotifyAuthResponse> call, Response<SpotifyAuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    accessToken = response.body().access_token;
                    Log.d(TAG, "Spotify access token fetched: " + accessToken);
                } else {
                    SpotifyAuthResponse errorResponse = response.body();
                    String errorMessage = "Spotify auth failed: " + response.code();
                    if (errorResponse != null && errorResponse.error != null) {
                        errorMessage += " - " + errorResponse.error + ": " + errorResponse.error_description;
                    } else {
                        errorMessage += " - " + response.message();
                    }
                    Log.e(TAG, errorMessage);
                    showToast("Failed to authenticate with Spotify: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<SpotifyAuthResponse> call, Throwable t) {
                Log.e(TAG, "Spotify auth error: " + t.getMessage());
                showToast("Error authenticating with Spotify: " + t.getMessage());
            }
        });
    }

    /**
     * Fetches a song recommendation based on the mood's emotional state using Spotify Web API.
     */
    private void fetchSongRecommendation() {
        if (accessToken == null) {
            showToast("Spotify authentication failed. Please try again later.");
            return;
        }

        // Get audio features for the mood
        MoodAudioFeatures features = MOOD_TO_AUDIO_FEATURES.getOrDefault(emoji, new MoodAudioFeatures("pop", 0.5f, 0.5f));
        String authHeader = "Bearer " + accessToken;

        Call<SpotifyRecommendationResponse> call = spotifyApiService.getRecommendations(
                authHeader,
                features.genre,
                features.valence,
                features.energy,
                1 // Limit to 1 recommendation
        );

        call.enqueue(new Callback<SpotifyRecommendationResponse>() {
            @Override
            public void onResponse(Call<SpotifyRecommendationResponse> call, Response<SpotifyRecommendationResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.isEmpty()) {
                    SpotifyRecommendationResponse.Track track = response.body().tracks.get(0);
                    String recommendedSong = track.name + " by " + track.artists.get(0).name;
                    showRecommendationDialog(recommendedSong);
                } else {
                    // Handle token expiry
                    if (response.code() == 401) { // Unauthorized
                        showToast("Spotify token expired. Refreshing...");
                        fetchSpotifyAccessToken(); // Fetch a new token
                        new Handler(Looper.getMainLooper()).postDelayed(() -> fetchSongRecommendation(), 2000);
                    } else {
                        showToast("No songs found for this mood.");
                        Log.e(TAG, "Spotify API error: " + response.message());
                    }
                }
            }

            @Override
            public void onFailure(Call<SpotifyRecommendationResponse> call, Throwable t) {
                showToast("Error fetching recommendation: " + t.getMessage());
                Log.e(TAG, "Spotify API failure: " + t.getMessage());
            }
        });
    }

    /**
     * Shows a dialog with the recommended song.
     */
    private void showRecommendationDialog(String songName) {
        Dialog recommendationDialog = new Dialog(this);
        recommendationDialog.setContentView(R.layout.dialog_song_recommendation);

        TextView songTextView = recommendationDialog.findViewById(R.id.recommendedSongTextView);
        Button closeButton = recommendationDialog.findViewById(R.id.closeButton);

        songTextView.setText("Recommended Song: " + songName);

        closeButton.setOnClickListener(v -> recommendationDialog.dismiss());

        Window window = recommendationDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setGravity(Gravity.CENTER);
        }

        recommendationDialog.show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}