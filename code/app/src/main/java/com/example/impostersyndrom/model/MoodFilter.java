package com.example.impostersyndrom.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MoodFilter {

    /**
     * Filters moods by both recent week and emotional state.
     *
     * @param moodDocs           The list of mood documents to filter.
     * @param filterByRecentWeek Whether to filter by the recent week.
     * @param emotionalState     The emotional state to filter by (empty string for no filter).
     * @return The filtered list of mood documents.
     */
    public List<DocumentSnapshot> applyFilter(List<DocumentSnapshot> moodDocs, boolean filterByRecentWeek, String emotionalState) {
        List<DocumentSnapshot> filteredMoods = new ArrayList<>(moodDocs);

        if (filterByRecentWeek) {
            filteredMoods = filterByRecentWeek(filteredMoods);
        }

        if (!emotionalState.isEmpty()) {
            filteredMoods = filterByEmotionalState(filteredMoods, emotionalState);
        }

        return filteredMoods;
    }

    /**
     * Filters moods by both recent week, emotional state, and reason.
     *
     * @param moodDocs           The list of mood documents to filter.
     * @param filterByRecentWeek Whether to filter by the recent week.
     * @param emotionalState     The emotional state to filter by (empty string for no filter).
     * @param reason            The reason to filter by (empty string for no filter).
     * @return The filtered list of mood documents.
     */
    public List<DocumentSnapshot> applyFilter(List<DocumentSnapshot> moodDocs, boolean filterByRecentWeek, String emotionalState, String reason) {
        List<DocumentSnapshot> filteredMoods = new ArrayList<>(moodDocs);

        if (filterByRecentWeek) {
            filteredMoods = filterByRecentWeek(filteredMoods);
        }

        if (!emotionalState.isEmpty()) {
            filteredMoods = filterByEmotionalState(filteredMoods, emotionalState);
        }

        if (!reason.isEmpty()) {
            filteredMoods = filterByReason(filteredMoods, reason);
        }

        return filteredMoods;
    }

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

    /**
     * Filters moods by emotional state.
     *
     * @param moodDocs       The list of mood documents to filter.
     * @param emotionalState The emotional state to filter by.
     * @return A list of moods with the specified emotional state.
     */
    public List<DocumentSnapshot> filterByEmotionalState(List<DocumentSnapshot> moodDocs, String emotionalState) {
        List<DocumentSnapshot> filteredMoods = new ArrayList<>();

        for (DocumentSnapshot moodDoc : moodDocs) {
            String state = moodDoc.getString("emotionalState");
            if (state != null && state.equals(emotionalState)) {
                filteredMoods.add(moodDoc);
            }
        }

        return filteredMoods;
    }

    /**
     * Filters moods by reason.
     *
     * @param moodDocs The list of mood documents to filter.
     * @param reason   The reason to filter by.
     * @return A list of moods with the specified reason.
     */
    public List<DocumentSnapshot> filterByReason(List<DocumentSnapshot> moodDocs, String reason) {
        List<DocumentSnapshot> filteredMoods = new ArrayList<>();

        for (DocumentSnapshot moodDoc : moodDocs) {
            String moodReason = moodDoc.getString("reason");
            if (moodReason != null && moodReason.toLowerCase().contains(reason.toLowerCase())) {
                filteredMoods.add(moodDoc);
            }
        }

        return filteredMoods;
    }
}