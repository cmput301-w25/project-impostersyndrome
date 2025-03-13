package com.example.impostersyndrom;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class MoodRepository{
    private final CollectionReference moodsRef;

    public MoodRepository() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        moodsRef = db.collection("moods");
    }

    public void addMood(Mood mood, OnMoodAddedListener listener) {
        DocumentReference docRef = moodsRef.document(mood.getId());
        docRef.set(mood)
                .addOnSuccessListener(aVoid -> listener.onMoodAdded())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public void updateMood(String moodId, Map<String, Object> updates, OnMoodUpdatedListener listener) {
        moodsRef.document(moodId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onMoodUpdated())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public interface OnMoodAddedListener {
        void onMoodAdded();
        void onError(String errorMessage);
    }

    public interface OnMoodUpdatedListener {
        void onMoodUpdated();
        void onError(String errorMessage);
    }
}
