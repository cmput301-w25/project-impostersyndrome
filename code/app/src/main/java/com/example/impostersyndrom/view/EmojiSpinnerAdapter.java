package com.example.impostersyndrom.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.impostersyndrom.R;

import java.util.List;

public class EmojiSpinnerAdapter extends ArrayAdapter<String> {
    private Context context;
    private List<String> emotionalStates; // List of emotional state descriptions
    private List<Integer> emojiDrawables; // List of emoji drawable resource IDs

    public EmojiSpinnerAdapter(@NonNull Context context, List<String> emotionalStates, List<Integer> emojiDrawables) {
        super(context, android.R.layout.simple_spinner_item, emotionalStates);
        this.context = context;
        this.emotionalStates = emotionalStates;
        this.emojiDrawables = emojiDrawables;
        setDropDownViewResource(R.layout.custom_spinner_item); // Use custom layout for dropdown
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createCustomView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createCustomView(position, convertView, parent);
    }

    private View createCustomView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.custom_spinner_item, parent, false);
        }

        // Get the emoji drawable and text for the current position
        ImageView emojiImageView = convertView.findViewById(R.id.emojiImageView);
        TextView emojiTextView = convertView.findViewById(R.id.emojiTextView);

        if (position == 0) {
            // First item is the "no filter" option
            emojiImageView.setImageDrawable(null);
            emojiTextView.setText("All Moods");
        } else {
            // Set the emoji drawable and text
            int drawableId = emojiDrawables.get(position - 1); // Adjust for the "no filter" option
            Drawable emojiDrawable = context.getResources().getDrawable(drawableId);
            emojiImageView.setImageDrawable(emojiDrawable);
            emojiTextView.setText(emotionalStates.get(position));
        }

        return convertView;
    }
}