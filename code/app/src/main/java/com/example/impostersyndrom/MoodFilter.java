package com.example.impostersyndrom;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MoodFilter {

    /**
     * Filters moods from the last 7 days.
     *
     * @param moodDocs The list of mood documents to filter.
     * @return A list of moods from the last 7 days.
     */
    public List<DocumentSnapshot> filterByRecentWeek(List<DocumentSnapshot> moodDocs) {
        List<DocumentSnapshot> filteredMoods = new ArrayList<>();

        // Calculate the timestamp for 7 days ago
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        long oneWeekAgo = calendar.getTimeInMillis();

        // Filter moods from the last 7 days
        for (DocumentSnapshot moodDoc : moodDocs) {
            Timestamp timestamp = moodDoc.getTimestamp("timestamp");
            if (timestamp != null && timestamp.toDate().getTime() >= oneWeekAgo) {
                filteredMoods.add(moodDoc);
            }
        }

        return filteredMoods;
    }
}