package com.example.impostersyndrom.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.network.SpotifyApiService;
import com.example.impostersyndrom.network.SpotifyRecommendationResponse;
import com.example.impostersyndrom.spotify.MoodAudioMapper;
import com.example.impostersyndrom.spotify.SpotifyManager;
import com.google.android.material.snackbar.Snackbar;
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
    private List<SpotifyRecommendationResponse.Track> shownTracksHistory = new ArrayList<>(); // Track history of shown tracks
    private int currentTrackIndex = -1; // Index of the currently displayed track in history
    private Set<String> shownTrackIds = new HashSet<>(); // Tracks shown in this session
    private SpotifyRecommendationResponse.Track currentTrack; // Track currently displayed
    private boolean isFetchingRecommendations = false; // Track if a fetch is in progress

    // Adapter for ViewPager2
    private MoodCardAdapter cardAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_mood_detail);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set content view: " + e.getMessage(), e);
            showMessage("Error loading layout: " + e.getMessage());
            finish();
            return;
        }

        if (!initializeViews()) {
            showMessage("Error initializing views.");
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

        // Preload song recommendations as soon as the activity is created
        if (recommendedTracks.isEmpty() && !isFetchingRecommendations) {
            fetchSongRecommendation();
        }

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
            // If recommendations are already fetched, display a track
            if (!recommendedTracks.isEmpty()) {
                displayTrackAtCurrentIndex(holder);
            } else if (isFetchingRecommendations) {
                // Show loading state while fetching
                holder.songNameTextView.setText("Loading song...");
                holder.artistNameTextView.setText("");
                holder.albumArtImageView.setImageResource(R.drawable.ic_music_note);
                holder.playOnSpotifyButton.setEnabled(false);
                holder.prevSongButton.setEnabled(false);
                holder.nextSongButton.setEnabled(false);
            } else {
                // If no fetch is in progress and no tracks are available, fetch now
                fetchSongRecommendation();
            }

            // Set up Next button listener
            holder.nextSongButton.setOnClickListener(v -> {
                if (recommendedTracks.isEmpty() || shownTrackIds.size() >= recommendedTracks.size()) {
                    fetchSongRecommendation();
                } else {
                    displayNextTrack(holder);
                }
            });

            // Set up Previous button listener
            holder.prevSongButton.setOnClickListener(v -> {
                displayPreviousTrack(holder);
            });

            // Set up Play on Spotify button listener
            if (currentTrack != null) {
                String trackUri = "spotify:track:" + currentTrack.id;
                Log.d(TAG, "Setting track URI for Play on Spotify button: " + trackUri);
                holder.playOnSpotifyButton.setOnClickListener(v -> playTrackOnSpotify(trackUri));
                holder.playOnSpotifyButton.setEnabled(true);
                // Enable navigation buttons based on history
                holder.prevSongButton.setEnabled(currentTrackIndex > 0);
                holder.nextSongButton.setEnabled(true);
            } else {
                holder.playOnSpotifyButton.setEnabled(false);
                holder.prevSongButton.setEnabled(false);
                holder.nextSongButton.setEnabled(false);
            }
        });

        // Configure ViewPager2
        viewPager.setAdapter(cardAdapter);
        viewPager.setOffscreenPageLimit(2); // Keep both pages in memory
        viewPager.setBackgroundColor(Color.BLACK); // Match the black background
        viewPager.setUserInputEnabled(true); // Ensure swiping is enabled

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

        isFetchingRecommendations = true;
        // Notify the adapter to update the UI to show loading state
        cardAdapter.notifyItemChanged(1);

        spotifyManager.fetchRecommendations(genre, valence, energy, new Callback<SpotifyRecommendationResponse>() {
            @Override
            public void onResponse(Call<SpotifyRecommendationResponse> call, Response<SpotifyRecommendationResponse> response) {
                isFetchingRecommendations = false;
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.isEmpty()) {
                    recommendedTracks.clear();
                    shownTrackIds.clear(); // Reset shown tracks for the new batch
                    shownTracksHistory.clear(); // Clear history for new batch
                    currentTrackIndex = -1;
                    recommendedTracks.addAll(response.body().tracks);
                    Log.d(TAG, "Fetched " + recommendedTracks.size() + " tracks");
                    // Display the first track
                    displayNextTrack(null);
                    // Update the Song Recommendation Card
                    cardAdapter.notifyItemChanged(1);
                } else {
                    String errorMessage = "Failed to fetch recommendation: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMessage);
                    if (response.code() == 401) {
                        showMessage("Spotify session expired. Please reopen this mood.");
                    } else {
                        fetchSongUsingSearch(genre);
                    }
                    // Update the UI to reflect the error state
                    cardAdapter.notifyItemChanged(1);
                }
            }

            @Override
            public void onFailure(Call<SpotifyRecommendationResponse> call, Throwable t) {
                isFetchingRecommendations = false;
                Log.e(TAG, "Recommendation fetch error: " + t.getMessage());
                // Update the UI to reflect the error state
                cardAdapter.notifyItemChanged(1);
            }
        });
    }

    private void displayNextTrack(MoodCardAdapter.SongRecommendationViewHolder holder) {
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

        // Update history and current track
        shownTrackIds.add(selectedTrack.id);
        shownTracksHistory.add(selectedTrack);
        currentTrackIndex = shownTracksHistory.size() - 1;
        currentTrack = selectedTrack;
        Log.d(TAG, "Selected track ID: " + selectedTrack.id);

        // Update the UI
        updateTrackDisplay(holder);
    }

    private void displayPreviousTrack(MoodCardAdapter.SongRecommendationViewHolder holder) {
        if (currentTrackIndex <= 0) {
            // No previous track to show
            return;
        }

        // Move back in history
        currentTrackIndex--;
        currentTrack = shownTracksHistory.get(currentTrackIndex);
        Log.d(TAG, "Showing previous track ID: " + currentTrack.id);

        // Update the UI
        updateTrackDisplay(holder);
    }

    private void displayTrackAtCurrentIndex(MoodCardAdapter.SongRecommendationViewHolder holder) {
        if (currentTrackIndex >= 0 && currentTrackIndex < shownTracksHistory.size()) {
            currentTrack = shownTracksHistory.get(currentTrackIndex);
            Log.d(TAG, "Displaying track at index " + currentTrackIndex + ": " + currentTrack.id);
            updateTrackDisplay(holder);
        } else if (!shownTracksHistory.isEmpty()) {
            // Fallback to the last track in history
            currentTrackIndex = shownTracksHistory.size() - 1;
            currentTrack = shownTracksHistory.get(currentTrackIndex);
            updateTrackDisplay(holder);
        } else {
            // No history yet, fetch a new track
            displayNextTrack(holder);
        }
    }

    private void updateTrackDisplay(MoodCardAdapter.SongRecommendationViewHolder holder) {
        if (holder != null && currentTrack != null) {
            holder.songNameTextView.setText(currentTrack.name);
            holder.artistNameTextView.setText(currentTrack.artists.get(0).name);
            Log.d(TAG, "Displayed: " + currentTrack.name + " by " + currentTrack.artists.get(0).name);

            // Load the album cover image
            if (currentTrack.album != null && currentTrack.album.images != null && !currentTrack.album.images.isEmpty()) {
                // Spotify typically provides images in descending order of size (e.g., 640x640, 300x300, 64x64)
                // Use the second image (300x300) for better performance
                String albumCoverUrl = currentTrack.album.images.get(1).url;
                Log.d(TAG, "Loading album cover from URL: " + albumCoverUrl);
                Glide.with(this)
                        .load(albumCoverUrl)
                        .placeholder(R.drawable.ic_music_note) // Show placeholder while loading
                        .error(R.drawable.ic_music_note) // Show placeholder if loading fails
                        .into(holder.albumArtImageView);
            } else {
                Log.w(TAG, "No album cover available for track: " + currentTrack.id);
                // Clear the ImageView and show the placeholder
                Glide.with(this)
                        .load(R.drawable.ic_music_note)
                        .into(holder.albumArtImageView);
            }

            // Update the Play on Spotify button with the current track's URI
            String trackUri = "spotify:track:" + currentTrack.id;
            Log.d(TAG, "Setting track URI for Play on Spotify button: " + trackUri);
            holder.playOnSpotifyButton.setOnClickListener(v -> playTrackOnSpotify(trackUri));
            holder.playOnSpotifyButton.setEnabled(true);

            // Enable/disable navigation buttons based on history
            holder.prevSongButton.setEnabled(currentTrackIndex > 0);
            holder.nextSongButton.setEnabled(true);
        }
    }

    private void fetchSongUsingSearch(String genre) {
        isFetchingRecommendations = true;
        // Notify the adapter to update the UI to show loading state
        cardAdapter.notifyItemChanged(1);

        spotifyManager.searchTracks(genre, new Callback<SpotifyApiService.SearchResponse>() {
            @Override
            public void onResponse(Call<SpotifyApiService.SearchResponse> call, Response<SpotifyApiService.SearchResponse> response) {
                isFetchingRecommendations = false;
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.items.isEmpty()) {
                    recommendedTracks.clear();
                    shownTrackIds.clear(); // Reset shown tracks for the new batch
                    shownTracksHistory.clear(); // Clear history for new batch
                    currentTrackIndex = -1;
                    recommendedTracks.addAll(response.body().tracks.items);
                    Log.d(TAG, "Fetched " + recommendedTracks.size() + " search results");
                    // Display the first track
                    displayNextTrack(null);
                    // Update the Song Recommendation Card
                    cardAdapter.notifyItemChanged(1);
                } else {
                    String errorMessage = "No songs found: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMessage);
                    // Update the UI to reflect the error state
                    cardAdapter.notifyItemChanged(1);
                }
            }

            @Override
            public void onFailure(Call<SpotifyApiService.SearchResponse> call, Throwable t) {
                isFetchingRecommendations = false;
                Log.e(TAG, "Search error: " + t.getMessage());
                // Update the UI to reflect the error state
                cardAdapter.notifyItemChanged(1);
            }
        });
    }

    private void playTrackOnSpotify(String trackUri) {
        Log.d(TAG, "playTrackOnSpotify called with URI: " + trackUri);

        // First, try the Spotify URI scheme (spotify:track:TRACK_ID)
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(trackUri));
        intent.setPackage("com.spotify.music"); // Explicitly target Spotify
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + getPackageName()));

        try {
            Log.d(TAG, "Attempting to launch Spotify with URI scheme...");
            startActivity(intent);
            Log.d(TAG, "Spotify URI intent launched successfully.");
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, "Failed to launch Spotify with URI scheme: " + e.getMessage());
            // Fallback to web URL
            String trackId = trackUri.replace("spotify:track:", "");
            String webUrl = "https://open.spotify.com/track/" + trackId;
            Log.d(TAG, "Falling back to web URL: " + webUrl);

            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            try {
                startActivity(webIntent);
                Log.d(TAG, "Web URL intent launched successfully.");
            } catch (android.content.ActivityNotFoundException ex) {
                Log.e(TAG, "No app available to handle web URL: " + ex.getMessage());
                showMessage("Spotify is not installed. Redirecting to install...");
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")));
                } catch (android.content.ActivityNotFoundException ex2) {
                    Log.e(TAG, "Play Store not available: " + ex2.getMessage());
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music")));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error while launching Spotify: " + e.getMessage());
            showMessage("An error occurred while trying to play the track.");
        }
    }

    private void setupBackButton() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                try {
                    // Create a new Intent only if needed to return data
                    boolean isMyMoods = getIntent().getBooleanExtra("isMyMoods", true);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("isMyMoods", isMyMoods);
                    setResult(RESULT_OK, resultIntent);

                    // Safely finish this activity
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error handling back button: " + e.getMessage(), e);
                    // Just finish the activity if there's an error
                    finish();
                }
            });
        }
    }

    // Override the system back button to ensure proper handling
    @Override
    public void onBackPressed() {
        try {
            // Create a new Intent only if needed to return data
            boolean isMyMoods = getIntent().getBooleanExtra("isMyMoods", true);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("isMyMoods", isMyMoods);
            setResult(RESULT_OK, resultIntent);

            super.onBackPressed();
        } catch (Exception e) {
            Log.e(TAG, "Error handling back button: " + e.getMessage(), e);
            super.onBackPressed();
        }
    }

    /**
     * Displays a Snackbar message.
     *
     * @param message The message to display.
     */
    private void showMessage(String message) {
        View rootView = findViewById(android.R.id.content);
        if (rootView != null && !isFinishing()) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        } else {
            Log.w(TAG, "Cannot show Snackbar: rootView is null or Activity is finishing");
        }
    }

    @Override
    protected void onDestroy() {
        try {
            // Clear any pending callbacks to prevent crashes
            if (viewPager != null) {
                viewPager.unregisterOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {});
            }

            // Clear Glide resources
            View contentView = findViewById(android.R.id.content);
            if (contentView != null) {
                Glide.with(this).clear(contentView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy: " + e.getMessage(), e);
        } finally {
            super.onDestroy();
        }
    }
}