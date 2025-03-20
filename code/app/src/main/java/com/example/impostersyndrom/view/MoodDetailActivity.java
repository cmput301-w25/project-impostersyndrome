package com.example.impostersyndrom.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.network.SpotifyApiService;
import com.example.impostersyndrom.network.SpotifyRecommendationResponse;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

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
    private TextView recommendedSongTextView;
    private LinearLayout recommendationRectangle; // Added for visibility control

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

    // Recommendation Tracking
    private List<SpotifyRecommendationResponse.Track> recommendedTracks = new ArrayList<>();
    private int currentTrackIndex = -1;

    // Mood to audio features mapping
    private static final Map<String, MoodAudioFeatures> MOOD_TO_AUDIO_FEATURES = new HashMap<>();

    static {
        MOOD_TO_AUDIO_FEATURES.put("emoji_happy", new MoodAudioFeatures("pop", 0.8f, 0.7f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_confused", new MoodAudioFeatures("indie-pop", 0.4f, 0.3f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_disgust", new MoodAudioFeatures("rock", 0.3f, 0.5f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_angry", new MoodAudioFeatures("metal", 0.2f, 0.9f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_sad", new MoodAudioFeatures("acoustic", 0.2f, 0.3f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_fear", new MoodAudioFeatures("ambient", 0.3f, 0.4f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_shame", new MoodAudioFeatures("folk", 0.3f, 0.2f));
        MOOD_TO_AUDIO_FEATURES.put("emoji_surprised", new MoodAudioFeatures("dance-pop", 0.7f, 0.8f));
    }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_mood_detail);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set content view: " + e.getMessage(), e);
            showToast("Error loading layout: " + e.getMessage());
            finish();
            return;
        }

        if (!initializeViews()) {
            showToast("Error initializing views.");
            finish();
            return;
        }

        retrieveIntentData();
        accessToken = getIntent().getStringExtra("accessToken");
        Log.d(TAG, "Access token received: " + (accessToken != null ? accessToken : "null"));

        try {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://api.spotify.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            spotifyApiService = retrofit.create(SpotifyApiService.class);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Retrofit: " + e.getMessage(), e);
            recommendedSongTextView.setText("Error initializing Spotify service.");
            recommendationRectangle.setVisibility(View.VISIBLE);
            return;
        }

        setupUI();
        setupBackButton();
        setupRecommendSongButton();
    }

    private boolean initializeViews() {
        try {
            emojiView = findViewById(R.id.emojiView);
            timeView = findViewById(R.id.timeView);
            reasonView = findViewById(R.id.reasonView);
            emojiDescView = findViewById(R.id.emojiDescription);
            groupView = findViewById(R.id.groupView);
            emojiRectangle = findViewById(R.id.emojiRectangle);
            imageUrlView = findViewById(R.id.imageUrlView);
            backButton = findViewById(R.id.backButton);
            recommendSongButton = findViewById(R.id.recommendSongButton);
            recommendedSongTextView = findViewById(R.id.recommendedSongTextView);
            recommendationRectangle = findViewById(R.id.recommendationRectangle);

            if (emojiView == null || timeView == null || reasonView == null || emojiDescView == null ||
                    groupView == null || emojiRectangle == null || imageUrlView == null || backButton == null ||
                    recommendSongButton == null || recommendedSongTextView == null || recommendationRectangle == null) {
                Log.e(TAG, "One or more views not found in layout.");
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            return false;
        }
    }

    private void retrieveIntentData() {
        Intent intent = getIntent();
        if (intent == null) {
            Log.e(TAG, "Intent is null.");
            return;
        }
        emoji = intent.getStringExtra("emoji");
        timestamp = intent.getParcelableExtra("timestamp");
        reason = intent.getStringExtra("reason");
        group = intent.getStringExtra("group");
        color = intent.getIntExtra("color", Color.WHITE);
        emojiDescription = intent.getStringExtra("emojiDescription");
        imageUrl = intent.getStringExtra("imageUrl");

        logMoodData();
    }

    private void logMoodData() {
        Log.d(TAG, "Emoji: " + emoji);
        Log.d(TAG, "Reason: " + reason);
        Log.d(TAG, "Group: " + group);
        Log.d(TAG, "Emoji Description: " + emojiDescription);
        Log.d(TAG, "Image URL: " + (imageUrl != null ? imageUrl : "null"));
    }

    private void setupUI() {
        setEmojiImage();
        setTimestamp();
        setReason();
        setGroup();
        setEmojiDescription();
        loadImage();
        setRoundedBackground();
    }

    private void setEmojiImage() {
        if (emoji != null && emojiView != null) {
            int emojiResId = getResources().getIdentifier(emoji, "drawable", getPackageName());
            if (emojiResId != 0) {
                emojiView.setImageResource(emojiResId);
            } else {
                Log.e(TAG, "Could not find drawable resource for emoji: " + emoji);
            }
        }
    }

    private void setTimestamp() {
        if (timeView == null) return;
        if (timestamp != null) {
            try {
                String formattedTime = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault()).format(timestamp.toDate());
                timeView.setText(formattedTime);
            } catch (Exception e) {
                Log.e(TAG, "Error formatting timestamp: " + e.getMessage());
                timeView.setText("Invalid time");
            }
        } else {
            timeView.setText("Unknown time");
        }
    }

    private void setReason() {
        if (reasonView != null) {
            reasonView.setText(reason != null ? reason : "No reason provided");
        }
    }

    private void setGroup() {
        if (groupView != null) {
            groupView.setText(group != null ? group : "No group provided");
        }
    }

    private void setEmojiDescription() {
        if (emojiDescView != null) {
            emojiDescView.setText(emojiDescription != null ? emojiDescription : "No emoji");
        }
    }

    private void loadImage() {
        if (imageUrlView == null) return;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageUrlView.setVisibility(View.VISIBLE);
            Log.d(TAG, "Loading image from URL: " + imageUrl);
            try {
                Glide.with(this)
                        .load(imageUrl)
                        .into(imageUrlView);
            } catch (Exception e) {
                Log.e(TAG, "Glide failed to load image: " + e.getMessage());
                imageUrlView.setVisibility(View.GONE);
            }
        } else {
            Log.d(TAG, "No image URL provided, hiding ImageView");
            imageUrlView.setVisibility(View.GONE);
        }
    }

    private void setRoundedBackground() {
        if (emojiRectangle == null) return;
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(2, Color.BLACK);
        emojiRectangle.setBackground(gradientDrawable);
    }

    private void setupBackButton() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.putExtra("isMyMoods", getIntent().getBooleanExtra("isMyMoods", true));
                setResult(RESULT_OK, intent);
                finish();
            });
        }
    }

    private void setupRecommendSongButton() {
        if (recommendSongButton != null) {
            recommendSongButton.setOnClickListener(v -> showNextRecommendation());
        }
    }

    private void showNextRecommendation() {
        Log.d(TAG, "showNextRecommendation called, currentTrackIndex: " + currentTrackIndex + ", track size: " + recommendedTracks.size());
        if (accessToken == null || accessToken.isEmpty()) {
            recommendedSongTextView.setText("Spotify authentication required. Please try again later.");
            recommendationRectangle.setVisibility(View.VISIBLE);
            showToast("Spotify authentication required.");
            return;
        }

        if (recommendedTracks.isEmpty() || currentTrackIndex >= recommendedTracks.size() - 1) {
            fetchSongRecommendation();
        } else {
            currentTrackIndex++;
            displayCurrentTrack();
        }
    }

    private void fetchSongRecommendation() {
        MoodAudioFeatures features = MOOD_TO_AUDIO_FEATURES.getOrDefault(emoji,
                new MoodAudioFeatures("pop", 0.5f, 0.5f));
        String authHeader = "Bearer " + accessToken;

        Log.d(TAG, "Fetching recommendation with genre: " + features.genre +
                ", valence: " + features.valence + ", energy: " + features.energy);

        Call<SpotifyRecommendationResponse> call = spotifyApiService.getRecommendations(
                authHeader,
                features.genre,
                "3t5xRXzsuZmMDkQzgY2RtW",
                features.valence,
                features.energy,
                10
        );

        call.enqueue(new Callback<SpotifyRecommendationResponse>() {
            @Override
            public void onResponse(Call<SpotifyRecommendationResponse> call, Response<SpotifyRecommendationResponse> response) {
                Log.d(TAG, "Recommendation API response code: " + response.code());
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.isEmpty()) {
                    recommendedTracks.clear();
                    recommendedTracks.addAll(response.body().tracks);
                    currentTrackIndex = 0;
                    Log.d(TAG, "Fetched " + recommendedTracks.size() + " tracks");
                    displayCurrentTrack();
                } else {
                    String errorMessage = "Failed to fetch recommendation: " + response.code() + " - " + response.message();
                    recommendedSongTextView.setText(errorMessage);
                    recommendationRectangle.setVisibility(View.VISIBLE);
                    Log.e(TAG, errorMessage);
                    if (response.code() == 401) {
                        showToast("Spotify session expired. Please reopen this mood.");
                    } else {
                        fetchSongUsingSearch(features.genre);
                    }
                }
            }

            @Override
            public void onFailure(Call<SpotifyRecommendationResponse> call, Throwable t) {
                recommendedSongTextView.setText("Error fetching recommendation: " + t.getMessage());
                recommendationRectangle.setVisibility(View.VISIBLE);
                Log.e(TAG, "Recommendation fetch error: " + t.getMessage());
            }
        });
    }

    private void displayCurrentTrack() {
        Log.d(TAG, "displayCurrentTrack called, index: " + currentTrackIndex + ", tracks size: " + recommendedTracks.size());
        if (currentTrackIndex >= 0 && currentTrackIndex < recommendedTracks.size()) {
            SpotifyRecommendationResponse.Track track = recommendedTracks.get(currentTrackIndex);
            String recommendedSong = track.name + " by " + track.artists.get(0).name;
            recommendedSongTextView.setText(recommendedSong);
            recommendationRectangle.setVisibility(View.VISIBLE);
            Log.d(TAG, "Displayed: " + recommendedSong);
        } else {
            Log.e(TAG, "Invalid track index or empty track list");
            recommendedSongTextView.setText("No recommendation available");
            recommendationRectangle.setVisibility(View.VISIBLE);
        }
    }

    private void fetchSongUsingSearch(String genre) {
        String authHeader = "Bearer " + accessToken;
        String query = "genre:" + genre;
        Call<SpotifyApiService.SearchResponse> call = spotifyApiService.searchTracks(authHeader, query, "track", 10);

        call.enqueue(new Callback<SpotifyApiService.SearchResponse>() {
            @Override
            public void onResponse(Call<SpotifyApiService.SearchResponse> call, Response<SpotifyApiService.SearchResponse> response) {
                Log.d(TAG, "Search API response code: " + response.code());
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.items.isEmpty()) {
                    recommendedTracks.clear();
                    recommendedTracks.addAll(response.body().tracks.items);
                    currentTrackIndex = 0;
                    Log.d(TAG, "Fetched " + recommendedTracks.size() + " search results");
                    displayCurrentTrack();
                } else {
                    String errorMessage = "No songs found: " + response.code() + " - " + response.message();
                    recommendedSongTextView.setText(errorMessage);
                    recommendationRectangle.setVisibility(View.VISIBLE);
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<SpotifyApiService.SearchResponse> call, Throwable t) {
                recommendedSongTextView.setText("Error searching for songs: " + t.getMessage());
                recommendationRectangle.setVisibility(View.VISIBLE);
                Log.e(TAG, "Search error: " + t.getMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}