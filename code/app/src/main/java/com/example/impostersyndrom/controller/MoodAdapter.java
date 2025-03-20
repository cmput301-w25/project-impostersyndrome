package com.example.impostersyndrom.controller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.MoodItem;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MoodAdapter extends ArrayAdapter<MoodItem> {
    private final boolean showUsername; // Flag to show/hide username

    public MoodAdapter(Context context, List<MoodItem> moods, boolean showUsername) {
        super(context, 0, moods);
        this.showUsername = showUsername;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_mood, parent, false);
        }

        MoodItem moodItem = getItem(position);

        TextView usernameView = convertView.findViewById(R.id.usernameView);
        TextView timeView = convertView.findViewById(R.id.timeView);
        TextView reasonView = convertView.findViewById(R.id.reasonView);
        ImageView emojiView = convertView.findViewById(R.id.emojiView);
        ImageView socialSituationIcon = convertView.findViewById(R.id.socialSituationIcon);
        View rootLayout = convertView.findViewById(R.id.rootLayout);

        if (moodItem != null) {
            // ✅ Show username only if "Following" tab is selected
            if (showUsername) {
                usernameView.setText(moodItem.getUsername());
                usernameView.setVisibility(View.VISIBLE);
            } else {
                usernameView.setVisibility(View.GONE);
            }

            // ✅ Set timestamp
            Timestamp timestamp = moodItem.getTimestamp();
            if (timestamp != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault());
                timeView.setText(sdf.format(timestamp.toDate()));
            } else {
                timeView.setText("Unknown time");
            }

            // ✅ Set reason
            reasonView.setText(moodItem.getReason());

            // ✅ Fix Emoji Loading
            String emojiKey = moodItem.getEmotionalState();
            int emojiResId = getContext().getResources().getIdentifier(emojiKey, "drawable", getContext().getPackageName());
            if (emojiResId != 0) {
                emojiView.setImageResource(emojiResId);
            }

            // ✅ Set background color dynamically
            int color = moodItem.getColor();
            GradientDrawable background = new GradientDrawable();
            background.setShape(GradientDrawable.RECTANGLE);
            background.setCornerRadius(50); // Rounded corners
            background.setColor(color);
            background.setStroke(2, Color.BLACK); // Border

            rootLayout.setBackground(background);

            // Reset the social situation icon state before setting the new value
            socialSituationIcon.setImageDrawable(null); // Clear any previous drawable
            socialSituationIcon.setVisibility(View.GONE); // Default to hidden

            // Set social situation icon
            String socialSituation = moodItem.getSocialSituation();
            switch (socialSituation) {
                case "Alone":
                    socialSituationIcon.setImageResource(R.drawable.ic_alone);
                    socialSituationIcon.setVisibility(View.VISIBLE);
                    break;
                case "With another person":
                    socialSituationIcon.setImageResource(R.drawable.ic_with_one);
                    socialSituationIcon.setVisibility(View.VISIBLE);
                    break;
                case "With several people":
                    socialSituationIcon.setImageResource(R.drawable.ic_with_several);
                    socialSituationIcon.setVisibility(View.VISIBLE);
                    break;
                case "With a crowd":
                    socialSituationIcon.setImageResource(R.drawable.ic_crowd);
                    socialSituationIcon.setVisibility(View.VISIBLE);
                    break;
                default:
                    // For "Unknown" or any unexpected value, keep the icon hidden
                    socialSituationIcon.setVisibility(View.GONE);
                    break;
            }
        } else {
            // If moodItem is null, ensure the icon is hidden
            socialSituationIcon.setImageDrawable(null);
            socialSituationIcon.setVisibility(View.GONE);
        }

        return convertView;
    }
}
