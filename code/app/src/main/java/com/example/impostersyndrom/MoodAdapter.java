package com.example.impostersyndrom;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

public class MoodAdapter extends ArrayAdapter<DocumentSnapshot> {
    private Context context;
    private List<DocumentSnapshot> moodDocs;

    public MoodAdapter(Context context, List<DocumentSnapshot> moodDocs) {
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

        DocumentSnapshot moodDoc = moodDocs.get(position);
        Map<String, Object> data = moodDoc.getData();
        if (data != null) {
            String emoji = (String) data.get("emotionalState");
            String description = (String) data.get("emojiDescription");
            Timestamp timestamp = (Timestamp) data.get("timestamp"); // using timestamp instead of Date
            String reason = (String) data.get("reason");
            int color = data.get("color") != null ? ((Long) data.get("color")).intValue() : Color.WHITE; // Retrieve color

            TextView emojiView = convertView.findViewById(R.id.emojiView);
            TextView emojiDescription = convertView.findViewById(R.id.emojiDescription);
            TextView timeView = convertView.findViewById(R.id.timeView);
            TextView reasonView = convertView.findViewById(R.id.reasonView);
            View rootLayout = convertView.findViewById(R.id.rootLayout);

            emojiView.setText(emoji != null ? emoji : "❓");
            emojiDescription.setText(description != null ? description : "No description");

            // timestamp to date
            Date date = timestamp != null ? timestamp.toDate() : null;
            timeView.setText(date != null ?
                    new SimpleDateFormat("dd-MM-yyyy | HH:mm", Locale.getDefault()).format(date) :
                    "Unknown time");

            reasonView.setText(reason != null ? reason : "No reason provided");

            // apply round background color
            setRoundedBackground(rootLayout, color);
        }

        return convertView;
    }

    // method to set rounded background with dynamic color
    private void setRoundedBackground(View view, int color) {
        GradientDrawable gradientDrawable = new GradientDrawable();
        gradientDrawable.setShape(GradientDrawable.RECTANGLE);
        gradientDrawable.setCornerRadius(50);
        gradientDrawable.setColor(color);
        gradientDrawable.setStroke(2, Color.BLACK);

        view.setBackground(gradientDrawable);
    }
}