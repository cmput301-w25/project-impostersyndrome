package com.example.impostersyndrom.controller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
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

/**
 * Adapter for displaying a list of mood items with customizable username visibility.
 *
 * @author Roshan
 * @author Rayan 
 */
public class MoodAdapter extends ArrayAdapter<MoodItem> {
    private final boolean showUsername; // Flag to show/hide username

    /**
     * Constructs a new MoodAdapter.
     *
     * @param context The context in which the adapter is running
     * @param moods The list of mood items to display
     * @param showUsername Flag to determine if usernames should be shown (true for Following tab)
     */
    public MoodAdapter(Context context, List<MoodItem> moods, boolean showUsername) {
        super(context, 0, moods);
        this.showUsername = showUsername;
    }

    /**
     * Provides a view for an AdapterView (ListView, GridView, etc.).
     *
     * @param position The position in the list of data
     * @param convertView The recycled view to populate, or null if none available
     * @param parent The parent ViewGroup that this view will be attached to
     * @return A View corresponding to the data at the specified position
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_mood, parent, false);
        }

        MoodItem moodItem = getItem(position);
        // Implementation details omitted for brevity
        return convertView;
    }
}
