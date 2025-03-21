package com.example.impostersyndrom.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.network.SpotifyApiService;
import com.example.impostersyndrom.network.SpotifyRecommendationResponse;
import com.example.impostersyndrom.spotify.MoodAudioMapper;
import com.example.impostersyndrom.spotify.SpotifyManager;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoodDetailActivity extends AppCompatActivity {

    private static final String TAG = "MoodDetailActivity";

    // UI Components
    private ImageButton backButton;
    private ViewPager2 viewPager;

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
    private SpotifyManager spotifyManager;
    private MoodAudioMapper moodAudioMapper;

    // Recommendation Tracking
    private List<SpotifyRecommendationResponse.Track> recommendedTracks = new ArrayList<>();
    private Set<String> shownTrackIds = new HashSet<>(); // Tracks shown in this session

    // Adapter for ViewPager2
    private MoodCardAdapter cardAdapter;

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

        // Initialize SpotifyManager and MoodAudioMapper
        spotifyManager = SpotifyManager.getInstance();
        moodAudioMapper = new MoodAudioMapper();

        setupViewPager();
        setupBackButton();
    }

    private boolean initializeViews() {
        try {
            backButton = findViewById(R.id.backButton);
            viewPager = findViewById(R.id.viewPager);

            if (backButton == null || viewPager == null) {
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

    private void setupViewPager() {
        cardAdapter = new MoodCardAdapter(this);

        // Set up listener for Mood Details Card
        cardAdapter.setMoodDetailsListener(holder -> {
            Log.d(TAG, "Binding Mood Details Card");
            // Bind emoji image
            if (emoji != null && holder.emojiView != null) {
                int emojiResId = getResources().getIdentifier(emoji, "drawable", getPackageName());
                if (emojiResId != 0) {
                    holder.emojiView.setImageResource(emojiResId);
                } else {
                    Log.e(TAG, "Could not find drawable resource for emoji: " + emoji);
                }
            }

            // Bind timestamp
            if (holder.timeView != null) {
                if (timestamp != null) {
                    try {
                        String formattedTime = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault()).format(timestamp.toDate());
                        holder.timeView.setText(formattedTime);
                    } catch (Exception e) {
                        Log.e(TAG, "Error formatting timestamp: " + e.getMessage());
                        holder.timeView.setText("Invalid time");
                    }
                } else {
                    holder.timeView.setText("Unknown time");
                }
            }

            // Bind reason
            if (holder.reasonView != null) {
                holder.reasonView.setText(reason != null ? reason : "No reason provided");
            }

            // Bind group
            if (holder.groupView != null) {
                holder.groupView.setText(group != null ? group : "No group provided");
            }

            // Bind emoji description
            if (holder.emojiDescView != null) {
                holder.emojiDescView.setText(emojiDescription != null ? emojiDescription : "No emoji");
            }

            // Bind image
            if (holder.imageUrlView != null) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    holder.imageUrlView.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Loading image from URL: " + imageUrl);
                    try {
                        Glide.with(this)
                                .load(imageUrl)
                                .into(holder.imageUrlView);
                    } catch (Exception e) {
                        Log.e(TAG, "Glide failed to load image: " + e.getMessage());
                        holder.imageUrlView.setVisibility(View.GONE);
                    }
                } else {
                    Log.d(TAG, "No image URL provided, hiding ImageView");
                    holder.imageUrlView.setVisibility(View.GONE);
                }
            }

            // Set rounded background
            if (holder.emojiRectangle != null) {
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                gradientDrawable.setCornerRadius(50);
                gradientDrawable.setColor(color);
                gradientDrawable.setStroke(2, Color.BLACK);
                holder.emojiRectangle.setBackground(gradientDrawable);
            }
        });

        // Set up listener for Song Recommendation Card
        cardAdapter.setSongRecommendationListener(holder -> {
            Log.d(TAG, "Binding Song Recommendation Card");
            // Fetch initial song recommendation if none are loaded
            if (recommendedTracks.isEmpty()) {
                fetchSongRecommendation();
            } else {
                displayRandomUnshownTrack(holder);
            }

            // Set up Next button listener
            holder.nextSongButton.setOnClickListener(v -> {
                if (recommendedTracks.isEmpty() || shownTrackIds.size() >= recommendedTracks.size()) {
                    fetchSongRecommendation();
                } else {
                    displayRandomUnshownTrack(holder);
                }
            });
        });

        // Configure ViewPager2
        viewPager.setAdapter(cardAdapter);
        viewPager.setOffscreenPageLimit(2); // Keep both pages in memory
        viewPager.setBackgroundColor(Color.BLACK); // Match the black background
        viewPager.setUserInputEnabled(true); // Ensure swiping is enabled

        // Temporarily remove the PageTransformer to debug swiping
        // viewPager.setPageTransformer((page, position) -> {
        //     page.setTranslationX(-position * page.getWidth());
        //     page.setAlpha(1.0f);
        //     page.setScaleY(1.0f);
        //     page.setScaleX(1.0f);
        // });

        // Add a listener to log page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.d(TAG, "Page selected: " + position + " (0 = Mood Details, 1 = Song Recommendation)");
            }
        });
    }

    private void fetchSongRecommendation() {
        String genre = moodAudioMapper.getGenre(emoji);
        float valence = moodAudioMapper.getValence(emoji);
        float energy = moodAudioMapper.getEnergy(emoji);

        Log.d(TAG, "Fetching recommendation with genre: " + genre +
                ", valence: " + valence + ", energy: " + energy);

        spotifyManager.fetchRecommendations(genre, valence, energy, new Callback<SpotifyRecommendationResponse>() {
            @Override
            public void onResponse(Call<SpotifyRecommendationResponse> call, Response<SpotifyRecommendationResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.isEmpty()) {
                    recommendedTracks.clear();
                    shownTrackIds.clear(); // Reset shown tracks for the new batch
                    recommendedTracks.addAll(response.body().tracks);
                    Log.d(TAG, "Fetched " + recommendedTracks.size() + " tracks");
                    displayRandomUnshownTrack(null);
                } else {
                    String errorMessage = "Failed to fetch recommendation: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMessage);
                    if (response.code() == 401) {
                        showToast("Spotify session expired. Please reopen this mood.");
                    } else {
                        fetchSongUsingSearch(genre);
                    }
                }
            }

            @Override
            public void onFailure(Call<SpotifyRecommendationResponse> call, Throwable t) {
                Log.e(TAG, "Recommendation fetch error: " + t.getMessage());
            }
        });
    }

    private void displayRandomUnshownTrack(MoodCardAdapter.SongRecommendationViewHolder holder) {
        // Filter out tracks that have already been shown
        List<SpotifyRecommendationResponse.Track> unshownTracks = new ArrayList<>();
        for (SpotifyRecommendationResponse.Track track : recommendedTracks) {
            if (!shownTrackIds.contains(track.id)) {
                unshownTracks.add(track);
            }
        }

        if (unshownTracks.isEmpty()) {
            // All tracks have been shown; fetch a new batch
            fetchSongRecommendation();
            return;
        }

        // Randomly select one of the unshown tracks
        Random random = new Random();
        int randomIndex = random.nextInt(unshownTracks.size());
        SpotifyRecommendationResponse.Track selectedTrack = unshownTracks.get(randomIndex);

        // Update the Song Recommendation Card if holder is provided
        if (holder != null) {
            holder.songNameTextView.setText(selectedTrack.name);
            holder.artistNameTextView.setText(selectedTrack.artists.get(0).name);
            Log.d(TAG, "Displayed: " + selectedTrack.name + " by " + selectedTrack.artists.get(0).name);
        }

        // Mark the track as shown
        shownTrackIds.add(selectedTrack.id);
    }

    private void fetchSongUsingSearch(String genre) {
        spotifyManager.searchTracks(genre, new Callback<SpotifyApiService.SearchResponse>() {
            @Override
            public void onResponse(Call<SpotifyApiService.SearchResponse> call, Response<SpotifyApiService.SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.items.isEmpty()) {
                    recommendedTracks.clear();
                    shownTrackIds.clear(); // Reset shown tracks for the new batch
                    recommendedTracks.addAll(response.body().tracks.items);
                    Log.d(TAG, "Fetched " + recommendedTracks.size() + " search results");
                    displayRandomUnshownTrack(null);
                } else {
                    String errorMessage = "No songs found: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMessage);
                }
            }

            @Override
            public void onFailure(Call<SpotifyApiService.SearchResponse> call, Throwable t) {
                Log.e(TAG, "Search error: " + t.getMessage());
            }
        });
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

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}