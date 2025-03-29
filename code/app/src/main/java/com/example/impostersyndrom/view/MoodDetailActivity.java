package com.example.impostersyndrom.view;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.CommentsAdapter;
import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.model.Comment;
import com.example.impostersyndrom.model.CommentDataManager;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.example.impostersyndrom.network.SpotifyApiService;
import com.example.impostersyndrom.network.SpotifyRecommendationResponse;
import com.example.impostersyndrom.spotify.MoodAudioMapper;
import com.example.impostersyndrom.spotify.SpotifyManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private String moodId;
    private String emoji;
    private Timestamp timestamp;
    private String reason;
    private String group;
    private int color;
    private String emojiDescription;
    private String imageUrl;
    private Double latitude;
    private Double longitude;

    // Spotify Integration
    private String accessToken;
    private SpotifyManager spotifyManager;
    private MoodAudioMapper moodAudioMapper;

    // Recommendation Tracking
    private List<SpotifyRecommendationResponse.Track> recommendedTracks = new ArrayList<>();
    private List<SpotifyRecommendationResponse.Track> shownTracksHistory = new ArrayList<>();
    private int currentTrackIndex = -1;
    private Set<String> shownTrackIds = new HashSet<>();
    private SpotifyRecommendationResponse.Track currentTrack;
    private boolean isFetchingRecommendations = false;

    // Comments
    private RecyclerView commentsRecyclerView;
    private EditText commentEditText;
    private ImageButton sendCommentButton;
    private CommentsAdapter commentsAdapter;
    private CommentDataManager commentDataManager;

    // Current user info (user is guaranteed logged in)
    private String currentUserId;
    private String currentUsername;

    // Adapter for ViewPager2 (Spotify code remains unchanged)
    private MoodCardAdapter cardAdapter;

    // Profile manager to fetch username from Firestore
    private ProfileDataManager profileDataManager;

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

        // COMMENT CHANGE: Get current user info for comments
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUserId = user.getUid();
        currentUsername = user.getDisplayName();
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = "Anonymous";
        }
        profileDataManager = new ProfileDataManager();
        profileDataManager.fetchUserProfile(currentUserId, new ProfileDataManager.OnProfileFetchedListener() {
            @Override
            public void onProfileFetched(DocumentSnapshot profileDoc) {
                String fetchedName = profileDoc.getString("username");
                if (fetchedName != null && !fetchedName.isEmpty()) {
                    currentUsername = fetchedName;
                    Log.d(TAG, "Username updated from profile: " + currentUsername);
                }
            }
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Failed to fetch profile: " + errorMessage);
            }
        });
        // END COMMENT CHANGE

        if (!initializeViews()) {
            showMessage("Error initializing views.");
            finish();
            return;
        }
        retrieveIntentData();
        // COMMENT CHANGE: Retrieve mood extras (including moodId)
        moodId = getIntent().getStringExtra("moodId");
        if (moodId == null) {
            moodId = "";
            Log.e(TAG, "No moodId passed; comments may not function correctly.");
        }
        emoji = getIntent().getStringExtra("emoji");
        timestamp = getIntent().getParcelableExtra("timestamp");
        reason = getIntent().getStringExtra("reason");
        group = getIntent().getStringExtra("group");
        color = getIntent().getIntExtra("color", Color.WHITE);
        emojiDescription = getIntent().getStringExtra("emojiDescription");
        imageUrl = getIntent().getStringExtra("imageUrl");
        accessToken = getIntent().getStringExtra("accessToken");
        Log.d(TAG, "Access token received: " + (accessToken != null ? accessToken : "null"));

        spotifyManager = SpotifyManager.getInstance();
        moodAudioMapper = new MoodAudioMapper();

        setupViewPager();

        if (recommendedTracks.isEmpty() && !isFetchingRecommendations) {
            fetchSongRecommendation();
        }

        // COMMENT CHANGE: Setup Comments UI components
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentEditText = findViewById(R.id.commentEditText);
        sendCommentButton = findViewById(R.id.sendCommentButton);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter();
        // NEW: Pass currentUserId to the adapter so it can hide the delete button for others' comments
        commentsAdapter.setCurrentUserId(currentUserId);
        commentsRecyclerView.setAdapter(commentsAdapter);
        commentDataManager = new CommentDataManager();
        // END COMMENT CHANGE

        // COMMENT CHANGE: Setup delete listener for comments
        commentsAdapter.setOnCommentDeleteListener(comment -> {
            if (!currentUserId.equals(comment.getUserId())) {
                showMessage("You can only delete your own comments.");
                return;
            }
            new AlertDialog.Builder(MoodDetailActivity.this)
                    .setTitle("Delete Comment")
                    .setMessage("Are you sure you want to delete this comment?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        commentDataManager.deleteCommentAndReplies(comment.getMoodId(), comment.getId(), new CommentDataManager.OnCommentDeletedListener() {
                            @Override
                            public void onCommentDeleted() {
                                showMessage("Comment deleted");
                                if (comment.getParentId() == null) {
                                    // It's a top-level comment: just refresh all comments
                                    fetchComments();
                                } else {
                                    // It's a reply: find the parent comment in the adapter
                                    for (Comment c : commentsAdapter.getComments()) {
                                        if (c.getId().equals(comment.getParentId())) {
                                            updateRepliesForParent(c);
                                            break;
                                        }
                                    }
                                }
                            }
                            @Override
                            public void onError(String errorMessage) {
                                showMessage("Error deleting comment: " + errorMessage);
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // END COMMENT CHANGE

        // COMMENT CHANGE: Setup send comment button
        sendCommentButton.setOnClickListener(v -> {
            String text = commentEditText.getText().toString().trim();
            if (text.isEmpty()) {
                showMessage("Please enter a comment");
                return;
            }
            addComment(text);
        });
        // END COMMENT CHANGE

        // COMMENT CHANGE: Fetch existing comments for this mood on activity open
        fetchComments();
        // END COMMENT CHANGE

        setupBackButton();

        commentsAdapter.setOnReplyListener(parentComment -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MoodDetailActivity.this);
            builder.setTitle("Reply to " + parentComment.getUsername());
            final EditText input = new EditText(MoodDetailActivity.this);
            builder.setView(input);
            builder.setPositiveButton("Send", (dialog, which) -> {
                String replyText = input.getText().toString().trim();
                if (!replyText.isEmpty()) {
                    // Create a reply using the constructor that includes a parentId
                    Comment replyComment = new Comment(parentComment.getMoodId(), currentUserId, currentUsername, replyText, new Date(), parentComment.getId());
                    commentDataManager.addComment(parentComment.getMoodId(), replyComment, new CommentDataManager.OnCommentAddedListener() {
                        @Override
                        public void onCommentAdded() {
                            showMessage("Reply added");
                            // Update the parent's local replyCount
                            parentComment.setReplyCount(parentComment.getReplyCount() + 1);
                            // Immediately update the UI for this comment's replies:
                            updateRepliesForParent(parentComment);
                        }
                        @Override
                        public void onError(String errorMessage) {
                            showMessage("Error adding reply: " + errorMessage);
                        }
                    });
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });
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

        // Check if this is an update from EditMoodActivity
        Mood updatedMood = (Mood) intent.getSerializableExtra("updatedMood");
        if (updatedMood != null) {
            emoji = updatedMood.getEmotionalState();
            Date date = updatedMood.getTimestamp();
            timestamp = date != null ? new Timestamp(date) : null;
            reason = updatedMood.getReason();
            group = updatedMood.getGroup();
            color = updatedMood.getColor();
            emojiDescription = updatedMood.getEmojiDescription();
            imageUrl = updatedMood.getImageUrl();
            latitude = updatedMood.getLatitude();
            longitude = updatedMood.getLongitude();
            Log.d(TAG, "Retrieved from updatedMood - Latitude: " + latitude + ", Longitude: " + longitude);
        } else {
            emoji = intent.getStringExtra("emoji");
            timestamp = intent.getParcelableExtra("timestamp");
            reason = intent.getStringExtra("reason");
            group = intent.getStringExtra("group");
            color = intent.getIntExtra("color", Color.WHITE);
            emojiDescription = intent.getStringExtra("emojiDescription");
            imageUrl = intent.getStringExtra("imageUrl");
            latitude = intent.hasExtra("latitude") ? intent.getDoubleExtra("latitude", 0.0) : null;
            longitude = intent.hasExtra("longitude") ? intent.getDoubleExtra("longitude", 0.0) : null;
            Log.d(TAG, "Retrieved from Intent extras - Latitude: " + latitude + ", Longitude: " + longitude);
        }
        logMoodData();
    }

    // COMMENT CHANGE: Updated addComment() to append the new comment instead of re-fetching all
    private void addComment(String text) {
        Comment newComment = new Comment(moodId, currentUserId, currentUsername, text, new Date());
        // Note: For a top-level comment, newComment.parentId will be null.
        commentDataManager.addComment(moodId, newComment, new CommentDataManager.OnCommentAddedListener() {
            @Override
            public void onCommentAdded() {
                showMessage("Comment added");
                commentEditText.setText("");
                commentsAdapter.addComment(newComment);
            }
            @Override
            public void onError(String errorMessage) {
                showMessage("Error adding comment: " + errorMessage);
            }
        });
    }
    // END COMMENT CHANGE

    private void fetchComments() {
        commentDataManager.fetchComments(moodId, new CommentDataManager.OnCommentsFetchedListener() {
            @Override
            public void onCommentsFetched(List<Comment> comments) {
                Log.d(TAG, "Fetched " + comments.size() + " comments for moodId: " + moodId);
                commentsAdapter.setComments(comments);
            }
            @Override
            public void onError(String errorMessage) {
                showMessage("Error fetching comments: " + errorMessage);

            }
        });
    }

    // Helper method to update replies for a given parent comment
    private void updateRepliesForParent(Comment parentComment) {
        commentDataManager.fetchReplies(parentComment.getMoodId(), parentComment.getId(),
                new CommentDataManager.OnRepliesFetchedListener() {
                    @Override
                    public void onRepliesFetched(List<Comment> replies) {
                        commentsAdapter.updateRepliesForComment(parentComment, replies);
                    }
                    @Override
                    public void onError(String errorMessage) {
                        showMessage("Error refreshing replies: " + errorMessage);
                    }
                });
    }


    private void logMoodData() {
        Log.d(TAG, "Mood ID: " + moodId);
        Log.d(TAG, "Emoji: " + emoji);
        Log.d(TAG, "Timestamp: " + (timestamp != null ? timestamp.toString() : "null"));
        Log.d(TAG, "Reason: " + reason);
        Log.d(TAG, "Group: " + group);
        Log.d(TAG, "Emoji Description: " + emojiDescription);
        Log.d(TAG, "Image URL: " + (imageUrl != null ? imageUrl : "null"));
        Log.d(TAG, "Latitude: " + latitude);
        Log.d(TAG, "Longitude: " + longitude);
    }

    private void setupViewPager() {
        cardAdapter = new MoodCardAdapter(this);

        cardAdapter.setMoodDetailsListener(holder -> {
            Log.d(TAG, "Binding Mood Details Card");

            if (emoji != null && holder.emojiView != null) {
                int emojiResId = getResources().getIdentifier(emoji, "drawable", getPackageName());
                if (emojiResId != 0) {
                    holder.emojiView.setImageResource(emojiResId);
                } else {
                    Log.e(TAG, "Could not find drawable resource for emoji: " + emoji);
                }
            }

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

            if (holder.reasonView != null) {
                holder.reasonView.setText(reason != null ? reason : "No reason provided");
            }

            if (holder.groupView != null) {
                holder.groupView.setText(group != null ? group : "No group provided");
            }

            if (holder.emojiDescView != null) {
                holder.emojiDescView.setText(emojiDescription != null ? emojiDescription : "No emoji");
            }

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

            if (holder.emojiRectangle != null) {
                GradientDrawable gradientDrawable = new GradientDrawable();
                gradientDrawable.setShape(GradientDrawable.RECTANGLE);
                gradientDrawable.setCornerRadius(50);
                gradientDrawable.setColor(color);
                gradientDrawable.setStroke(2, Color.BLACK);
                holder.emojiRectangle.setBackground(gradientDrawable);
            }

            // Location button logic
            if (holder.locationButton != null) {
                holder.locationButton.setVisibility(View.VISIBLE); // Always show the button
                holder.locationButton.setOnClickListener(v -> {
                    Log.d(TAG, "Location button clicked - Current lat: " + latitude + ", lon: " + longitude);
                    if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
                        Log.d(TAG, "Valid location found, opening map with lat: " + latitude + ", lon: " + longitude);
                        openMap();
                    } else {
                        Log.d(TAG, "No valid location: lat=" + latitude + ", lon=" + longitude);
                    }
                });
            } else {
                Log.e(TAG, "locationButton is null in MoodDetailsViewHolder");
            }
        });

        cardAdapter.setSongRecommendationListener(holder -> {
            Log.d(TAG, "Binding Song Recommendation Card");
            if (!recommendedTracks.isEmpty()) {
                displayTrackAtCurrentIndex(holder);
            } else if (isFetchingRecommendations) {
                holder.songNameTextView.setText("Loading song...");
                holder.artistNameTextView.setText("");
                holder.albumArtImageView.setImageResource(R.drawable.ic_music_note);
                holder.playOnSpotifyButton.setEnabled(false);
                holder.prevSongButton.setEnabled(false);
                holder.nextSongButton.setEnabled(false);
            } else {
                fetchSongRecommendation();
            }

            holder.nextSongButton.setOnClickListener(v -> {
                if (recommendedTracks.isEmpty() || shownTrackIds.size() >= recommendedTracks.size()) {
                    fetchSongRecommendation();
                } else {
                    displayNextTrack(holder);
                }
            });

            holder.prevSongButton.setOnClickListener(v -> {
                displayPreviousTrack(holder);
            });

            if (currentTrack != null) {
                String trackUri = "spotify:track:" + currentTrack.id;
                holder.playOnSpotifyButton.setOnClickListener(v -> playTrackOnSpotify(trackUri));
                holder.playOnSpotifyButton.setEnabled(true);
                holder.prevSongButton.setEnabled(currentTrackIndex > 0);
                holder.nextSongButton.setEnabled(true);
            } else {
                holder.playOnSpotifyButton.setEnabled(false);
                holder.prevSongButton.setEnabled(false);
                holder.nextSongButton.setEnabled(false);
            }
        });

        viewPager.setAdapter(cardAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setBackgroundColor(Color.BLACK);
        viewPager.setUserInputEnabled(true);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Log.d(TAG, "Page selected: " + position);
            }
        });
    }

    private void fetchSongRecommendation() {
        String genre = moodAudioMapper.getGenre(emoji);
        float valence = moodAudioMapper.getValence(emoji);
        float energy = moodAudioMapper.getEnergy(emoji);

        isFetchingRecommendations = true;
        cardAdapter.notifyItemChanged(1);

        spotifyManager.fetchRecommendations(genre, valence, energy, new Callback<SpotifyRecommendationResponse>() {
            @Override
            public void onResponse(Call<SpotifyRecommendationResponse> call, Response<SpotifyRecommendationResponse> response) {
                isFetchingRecommendations = false;
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.isEmpty()) {
                    recommendedTracks.clear();
                    shownTrackIds.clear();
                    shownTracksHistory.clear();
                    currentTrackIndex = -1;
                    recommendedTracks.addAll(response.body().tracks);
                    displayNextTrack(null);
                    cardAdapter.notifyItemChanged(1);
                } else {
                    String errorMessage = "Failed to fetch recommendation: " + response.code() + " - " + response.message();
                    Log.e(TAG, errorMessage);
                    if (response.code() == 401) {
                        showMessage("Spotify session expired. Please reopen this mood.");
                    } else {
                        fetchSongUsingSearch(genre);
                    }
                    cardAdapter.notifyItemChanged(1);
                }
            }

            @Override
            public void onFailure(Call<SpotifyRecommendationResponse> call, Throwable t) {
                isFetchingRecommendations = false;
                Log.e(TAG, "Recommendation fetch error: " + t.getMessage());
                cardAdapter.notifyItemChanged(1);
            }
        });
    }

    private void displayNextTrack(MoodCardAdapter.SongRecommendationViewHolder holder) {
        List<SpotifyRecommendationResponse.Track> unshownTracks = new ArrayList<>();
        for (SpotifyRecommendationResponse.Track track : recommendedTracks) {
            if (!shownTrackIds.contains(track.id)) {
                unshownTracks.add(track);
            }
        }

        if (unshownTracks.isEmpty()) {
            fetchSongRecommendation();
            return;
        }

        Random random = new Random();
        int randomIndex = random.nextInt(unshownTracks.size());
        SpotifyRecommendationResponse.Track selectedTrack = unshownTracks.get(randomIndex);

        shownTrackIds.add(selectedTrack.id);
        shownTracksHistory.add(selectedTrack);
        currentTrackIndex = shownTracksHistory.size() - 1;
        currentTrack = selectedTrack;

        updateTrackDisplay(holder);
    }

    private void displayPreviousTrack(MoodCardAdapter.SongRecommendationViewHolder holder) {
        if (currentTrackIndex <= 0) {
            return;
        }

        currentTrackIndex--;
        currentTrack = shownTracksHistory.get(currentTrackIndex);
        updateTrackDisplay(holder);
    }

    private void displayTrackAtCurrentIndex(MoodCardAdapter.SongRecommendationViewHolder holder) {
        if (currentTrackIndex >= 0 && currentTrackIndex < shownTracksHistory.size()) {
            currentTrack = shownTracksHistory.get(currentTrackIndex);
            updateTrackDisplay(holder);
        } else if (!shownTracksHistory.isEmpty()) {
            currentTrackIndex = shownTracksHistory.size() - 1;
            currentTrack = shownTracksHistory.get(currentTrackIndex);
            updateTrackDisplay(holder);
        } else {
            displayNextTrack(holder);
        }
    }

    private void updateTrackDisplay(MoodCardAdapter.SongRecommendationViewHolder holder) {
        if (holder != null && currentTrack != null) {
            holder.songNameTextView.setText(currentTrack.name);
            holder.artistNameTextView.setText(currentTrack.artists.get(0).name);

            if (currentTrack.album != null && currentTrack.album.images != null && !currentTrack.album.images.isEmpty()) {
                String albumCoverUrl = currentTrack.album.images.get(1).url;
                Glide.with(this)
                        .load(albumCoverUrl)
                        .placeholder(R.drawable.ic_music_note)
                        .error(R.drawable.ic_music_note)
                        .into(holder.albumArtImageView);
            } else {
                Glide.with(this)
                        .load(R.drawable.ic_music_note)
                        .into(holder.albumArtImageView);
            }

            String trackUri = "spotify:track:" + currentTrack.id;
            holder.playOnSpotifyButton.setOnClickListener(v -> playTrackOnSpotify(trackUri));
            holder.playOnSpotifyButton.setEnabled(true);

            holder.prevSongButton.setEnabled(currentTrackIndex > 0);
            holder.nextSongButton.setEnabled(true);
        }
    }

    private void fetchSongUsingSearch(String genre) {
        isFetchingRecommendations = true;
        cardAdapter.notifyItemChanged(1);

        spotifyManager.searchTracks(genre, new Callback<SpotifyApiService.SearchResponse>() {
            @Override
            public void onResponse(Call<SpotifyApiService.SearchResponse> call, Response<SpotifyApiService.SearchResponse> response) {
                isFetchingRecommendations = false;
                if (response.isSuccessful() && response.body() != null && !response.body().tracks.items.isEmpty()) {
                    recommendedTracks.clear();
                    shownTrackIds.clear();
                    shownTracksHistory.clear();
                    currentTrackIndex = -1;
                    recommendedTracks.addAll(response.body().tracks.items);
                    displayNextTrack(null);
                    cardAdapter.notifyItemChanged(1);
                } else {
                    Log.e(TAG, "No songs found: " + response.code() + " - " + response.message());
                    cardAdapter.notifyItemChanged(1);
                }
            }

            @Override
            public void onFailure(Call<SpotifyApiService.SearchResponse> call, Throwable t) {
                isFetchingRecommendations = false;
                Log.e(TAG, "Search error: " + t.getMessage());
                cardAdapter.notifyItemChanged(1);
            }
        });
    }

    private void playTrackOnSpotify(String trackUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(trackUri));
        intent.setPackage("com.spotify.music");
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + getPackageName()));

        try {
            startActivity(intent);
        } catch (Exception e) {
            String trackId = trackUri.replace("spotify:track:", "");
            String webUrl = "https://open.spotify.com/track/" + trackId;
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            try {
                startActivity(webIntent);
                Log.d(TAG, "Web URL intent launched successfully.");
            } catch (android.content.ActivityNotFoundException ex) {
                Log.e(TAG, "No app available to handle web URL: " + ex.getMessage());
                showMessage("Spotify is not installed. Redirecting to install...");

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")));
                } catch (Exception ex2) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music")));
                }
            }

        }
    }

    private void openMap() {
        if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
            Intent intent = new Intent(this, MoodLocationMapActivity.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("emoji", emoji); // Pass the emoji to display on the map
            startActivity(intent);
            Log.d(TAG, "Navigating to MoodLocationMapActivity with lat: " + latitude + ", lon: " + longitude);
        } else {

            Log.e(TAG, "No valid location data: lat=" + latitude + ", lon=" + longitude);

        }
    }

    private void setupBackButton() {
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                try {
                    boolean isMyMoods = getIntent().getBooleanExtra("isMyMoods", true);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("isMyMoods", isMyMoods);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "Error handling back button: " + e.getMessage(), e);
                    finish();
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        try {
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
            if (viewPager != null) {
                viewPager.unregisterOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {});
            }
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
