package com.example.impostersyndrom;

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

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodAdapter extends ArrayAdapter {
    private Context context;
    private List moodDocs;

    public MoodAdapter(Context context, List moodDocs) {
        super(context, R.layout.item_mood, moodDocs);
        this.context = context;
        this.moodDocs = moodDocs;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.item_mood, parent, false);
        }

        DocumentSnapshot moodDoc = (DocumentSnapshot) moodDocs.get(position);
        Map data = moodDoc.getData();
        if (data != null) {
            String emoji = (String) data.get("emotionalState");
            Timestamp timestamp = (Timestamp) data.get("timestamp");
            String reason = (String) data.get("reason");
            String group = (String) data.get("group"); // Retrieve group from Firestore
            int color = data.get("color") != null ? ((Long) data.get("color")).intValue() : Color.WHITE;

            // Initialize views
            ImageView emojiView = convertView.findViewById(R.id.emojiView);
            TextView timeView = convertView.findViewById(R.id.timeView);
            TextView reasonView = convertView.findViewById(R.id.reasonView);
            TextView groupView = convertView.findViewById(R.id.groupView); // Group TextView
            View rootLayout = convertView.findViewById(R.id.rootLayout);

            // Set the custom emoji image
            if (emoji != null) {
                int emojiResId = context.getResources().getIdentifier(emoji, "drawable", context.getPackageName());
                emojiView.setImageResource(emojiResId);
            }

            // Set the time
            Date date = timestamp != null ? timestamp.toDate() : null;
            timeView.setText(date != null ?
                    new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault()).format(date) :
                    "Unknown time");

            // Set the reason
            reasonView.setText(reason != null ? reason : "No reason provided");

            // Set the group
            groupView.setText(group != null ? group : "No group provided");

            // Apply rounded background color
            setRoundedBackground(rootLayout, color);
        }

        return convertView;
    }

    // Method to set rounded background with dynamic color
    private void setRoundedBackground(View view, int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50); // Rounded corners (50dp radius)
        gradientDrawable.setColor(color); // Set the background color
        gradientDrawable.setStroke(2, Color.BLACK); // Set the border (2dp width, black color)

        // Set the GradientDrawable as the background
        view.setBackground(gradientDrawable);
    }
}