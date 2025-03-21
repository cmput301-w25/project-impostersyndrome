package com.example.impostersyndrom.view;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.impostersyndrom.R;

import java.util.ArrayList;
import java.util.List;

public class MoodCardAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_MOOD_DETAILS = 0;
    private static final int VIEW_TYPE_SONG_RECOMMENDATION = 1;

    private final Context context;
    private final List<Integer> cardTypes;

    // Callbacks for binding data
    private MoodDetailsViewHolder.OnBindMoodDetailsListener moodDetailsListener;
    private SongRecommendationViewHolder.OnBindSongRecommendationListener songRecommendationListener;

    public MoodCardAdapter(Context context) {
        this.context = context;
        this.cardTypes = new ArrayList<>();
        // Add the two card types
        cardTypes.add(VIEW_TYPE_MOOD_DETAILS);
        cardTypes.add(VIEW_TYPE_SONG_RECOMMENDATION);
    }

    public void setMoodDetailsListener(MoodDetailsViewHolder.OnBindMoodDetailsListener listener) {
        this.moodDetailsListener = listener;
    }

    public void setSongRecommendationListener(SongRecommendationViewHolder.OnBindSongRecommendationListener listener) {
        this.songRecommendationListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return cardTypes.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_MOOD_DETAILS) {
            View view = inflater.inflate(R.layout.item_mood_card, parent, false);
            return new MoodDetailsViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_song_recommendation_card, parent, false);
            return new SongRecommendationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MoodDetailsViewHolder) {
            if (moodDetailsListener != null) {
                moodDetailsListener.onBind((MoodDetailsViewHolder) holder);
            }
        } else if (holder instanceof SongRecommendationViewHolder) {
            if (songRecommendationListener != null) {
                songRecommendationListener.onBind((SongRecommendationViewHolder) holder);
            }
        }
    }

    @Override
    public int getItemCount() {
        return cardTypes.size();
    }

    // ViewHolder for Mood Details Card
    static class MoodDetailsViewHolder extends RecyclerView.ViewHolder {
        ImageView emojiView;
        TextView timeView;
        TextView reasonView;
        TextView emojiDescView;
        TextView groupView;
        View emojiRectangle;
        ImageView imageUrlView;

        interface OnBindMoodDetailsListener {
            void onBind(MoodDetailsViewHolder holder);
        }

        MoodDetailsViewHolder(View itemView) {
            super(itemView);
            emojiView = itemView.findViewById(R.id.emojiView);
            timeView = itemView.findViewById(R.id.timeView);
            reasonView = itemView.findViewById(R.id.reasonView);
            emojiDescView = itemView.findViewById(R.id.emojiDescription);
            groupView = itemView.findViewById(R.id.groupView);
            emojiRectangle = itemView.findViewById(R.id.emojiRectangle);
            imageUrlView = itemView.findViewById(R.id.imageUrlView);
        }
    }

    // ViewHolder for Song Recommendation Card
    static class SongRecommendationViewHolder extends RecyclerView.ViewHolder {
        TextView songNameTextView;
        TextView artistNameTextView;
        View nextSongButton;

        interface OnBindSongRecommendationListener {
            void onBind(SongRecommendationViewHolder holder);
        }

        SongRecommendationViewHolder(View itemView) {
            super(itemView);
            songNameTextView = itemView.findViewById(R.id.songNameTextView);
            artistNameTextView = itemView.findViewById(R.id.artistNameTextView);
            nextSongButton = itemView.findViewById(R.id.nextSongButton);
        }
    }
}