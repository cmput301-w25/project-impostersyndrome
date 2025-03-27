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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.Comment;
import com.example.impostersyndrom.model.CommentDataManager;
import com.example.impostersyndrom.model.ProfileDataManager;
import com.example.impostersyndrom.network.SpotifyApiService;
import com.example.impostersyndrom.network.SpotifyRecommendationResponse;
import com.example.impostersyndrom.spotify.MoodAudioMapper;
import com.example.impostersyndrom.spotify.SpotifyManager;
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
        // NOTE: Ensure your XML layout is wrapped in a ScrollView or NestedScrollView so all content is visible.
        try {
            setContentView(R.layout.activity_mood_detail);
        } catch (Exception e) {
            Log.e(TAG, "Failed to set content view: " + e.getMessage(), e);
            showToast("Error loading layout: " + e.getMessage());
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
            showToast("Error initializing views.");
            finish();
            return;
        }

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
        // END COMMENT CHANGE

        logMoodData();

        // Spotify and ViewPager setup (unchanged)
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
        commentsRecyclerView.setAdapter(commentsAdapter);
        commentDataManager = new CommentDataManager();
        // END COMMENT CHANGE

        // COMMENT CHANGE: Setup send comment button
        sendCommentButton.setOnClickListener(v -> {
            String text = commentEditText.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(MoodDetailActivity.this, "Please enter a comment", Toast.LENGTH_SHORT).show();
                return;
            }
            addComment(text);
        });
        // END COMMENT CHANGE

        // COMMENT CHANGE: Fetch existing comments for this mood on activity open
        fetchComments();
        // END COMMENT CHANGE

        setupBackButton();
    }

    private boolean initializeViews() {
        try {
            backButton = findViewById(R.id.backButton);
            viewPager = findViewById(R.id.viewPager);
            if (backButton == null || viewPager == null) {
                Log.e(TAG, "One or more required views not found in layout.");
                return false;
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views: " + e.getMessage(), e);
            return false;
        }
    }

    // COMMENT CHANGE: Updated addComment() to append the new comment instead of re-fetching all
    private void addComment(String text) {
        Comment newComment = new Comment(moodId, currentUserId, currentUsername, text, new Date());
        commentDataManager.addComment(newComment, new CommentDataManager.OnCommentAddedListener() {
            @Override
            public void onCommentAdded() {
                showToast("Comment added");
                commentEditText.setText("");
                // Instead of fetching all comments, append the new comment to the adapter.
                commentsAdapter.addComment(newComment);
            }
            @Override
            public void onError(String errorMessage) {
                showToast("Error adding comment: " + errorMessage);
            }
        });
    }
    // END COMMENT CHANGE

    // COMMENT CHANGE: Updated fetchComments() to fetch once when mood is clicked.
    private void fetchComments() {
        commentDataManager.fetchComments(moodId, new CommentDataManager.OnCommentsFetchedListener() {
            @Override
            public void onCommentsFetched(List<Comment> comments) {
                Log.d(TAG, "Fetched " + comments.size() + " comments for moodId: " + moodId);
                // Set the adapter's list (this should call notifyDataSetChanged() in your adapter)
                commentsAdapter.setComments(comments);
            }
            @Override
            public void onError(String errorMessage) {
                showToast("Error fetching comments: " + errorMessage);
            }
        });
    }
    // END COMMENT CHANGE

    private void logMoodData() {
        Log.d(TAG, "Mood ID: " + moodId);
        Log.d(TAG, "Emoji: " + emoji);
        Log.d(TAG, "Reason: " + reason);
        Log.d(TAG, "Group: " + group);
        Log.d(TAG, "Emoji Description: " + emojiDescription);
        Log.d(TAG, "Image URL: " + (imageUrl != null ? imageUrl : "null"));
    }

    private void setupViewPager() {
        cardAdapter = new MoodCardAdapter(this);

        // Setup the "Mood Details" card
        cardAdapter.setMoodDetailsListener(holder -> {
            // Bind your mood data to holder...
            if (emoji != null && holder.emojiView != null) {
                int emojiResId = getResources().getIdentifier(emoji, "drawable", getPackageName());
                if (emojiResId != 0) {
                    holder.emojiView.setImageResource(emojiResId);
                } else {
                    Log.e(TAG, "Could not find drawable for emoji: " + emoji);
                }
            }

            if (holder.timeView != null && timestamp != null) {
                try {
                    String formattedTime = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault())
                            .format(timestamp.toDate());
                    holder.timeView.setText(formattedTime);
                } catch (Exception e) {
                    holder.timeView.setText("Invalid time");
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
                    try {
                        Glide.with(this).load(imageUrl).into(holder.imageUrlView);
                    } catch (Exception e) {
                        holder.imageUrlView.setVisibility(View.GONE);
                    }
                } else {
                    holder.imageUrlView.setVisibility(View.GONE);
                }
            }

            // Rounded background
            if (holder.emojiRectangle != null) {
                GradientDrawable gd = new GradientDrawable();
                gd.setShape(GradientDrawable.RECTANGLE);
                gd.setCornerRadius(50);
                gd.setColor(color);
                gd.setStroke(2, Color.BLACK);
                holder.emojiRectangle.setBackground(gd);
            }
        });

        // Setup the "Song Recommendation" card
        cardAdapter.setSongRecommendationListener(holder -> {
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

        // Attach adapter to ViewPager
        viewPager.setAdapter(cardAdapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setBackgroundColor(Color.BLACK);
        viewPager.setUserInputEnabled(true);

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
                    if (response.code() == 401) {
                        showToast("Spotify session expired. Please reopen this mood.");
                    } else {
                        fetchSongUsingSearch(genre);
                    }
                    cardAdapter.notifyItemChanged(1);
                }
            }

            @Override
            public void onFailure(Call<SpotifyRecommendationResponse> call, Throwable t) {
                isFetchingRecommendations = false;
                cardAdapter.notifyItemChanged(1);
            }
        });
    }

    private void displayNextTrack(MoodCardAdapter.SongRecommendationViewHolder holder) {
        // Filter out tracks already shown
        List<SpotifyRecommendationResponse.Track> unshown = new ArrayList<>();
        for (SpotifyRecommendationResponse.Track track : recommendedTracks) {
            if (!shownTrackIds.contains(track.id)) {
                unshown.add(track);
            }
        }
        if (unshown.isEmpty()) {
            fetchSongRecommendation();
            return;
        }
        Random random = new Random();
        int randomIndex = random.nextInt(unshown.size());
        SpotifyRecommendationResponse.Track selected = unshown.get(randomIndex);

        shownTrackIds.add(selected.id);
        shownTracksHistory.add(selected);
        currentTrackIndex = shownTracksHistory.size() - 1;
        currentTrack = selected;

        updateTrackDisplay(holder);
    }

    private void displayPreviousTrack(MoodCardAdapter.SongRecommendationViewHolder holder) {
        if (currentTrackIndex <= 0) return;
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
                Glide.with(this).load(R.drawable.ic_music_note).into(holder.albumArtImageView);
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
                    cardAdapter.notifyItemChanged(1);
                }
            }
            @Override
            public void onFailure(Call<SpotifyApiService.SearchResponse> call, Throwable t) {
                isFetchingRecommendations = false;
                cardAdapter.notifyItemChanged(1);
            }
        });
    }

    private void playTrackOnSpotify(String trackUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(trackUri));
        intent.setPackage("com.spotify.music");
        intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + getPackageName()));

        try {
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            String trackId = trackUri.replace("spotify:track:", "");
            String webUrl = "https://open.spotify.com/track/" + trackId;
            Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUrl));
            try {
                startActivity(webIntent);
            } catch (android.content.ActivityNotFoundException ex2) {
                showToast("Spotify is not installed. Redirecting to install...");
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.spotify.music")));
                } catch (android.content.ActivityNotFoundException ex3) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.spotify.music")));
                }
            }
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
            super.onBackPressed();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
