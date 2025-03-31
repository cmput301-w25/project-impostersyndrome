package com.example.impostersyndrom.model;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;
import java.util.Map;

public class MoodDataManager {
    private final CollectionReference moodsRef;
    private final FirebaseWrapper firebaseWrapper;

    public MoodDataManager() {
        this.firebaseWrapper = new FirebaseWrapper();
        moodsRef = firebaseWrapper.collection("moods");
    }

    // Constructor for testing
    public MoodDataManager(FirebaseWrapper firebaseWrapper) {
        this.firebaseWrapper = firebaseWrapper;
        moodsRef = firebaseWrapper.collection("moods");
    }

    /**
     * Adds a new mood to Firestore.
     *
     * @param mood     The mood to add.
     * @param listener Listener for success or failure.
     */
    public void addMood(Mood mood, OnMoodAddedListener listener) {
        DocumentReference docRef = moodsRef.document(mood.getId());
        docRef.set(mood)
                .addOnSuccessListener(aVoid -> listener.onMoodAdded())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Updates an existing mood in Firestore.
     *
     * @param moodId   The ID of the mood to update.
     * @param updates  The updates to apply.
     * @param listener Listener for success or failure.
     */
    public void updateMood(String moodId, Map<String, Object> updates, OnMoodUpdatedListener listener) {
        moodsRef.document(moodId)
                .update(updates)
                .addOnSuccessListener(aVoid -> listener.onMoodUpdated())
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    /**
     * Fetches moods for a specific user from Firestore.
     *
     * @param userId   The ID of the user.
     * @param listener Listener for success or failure.
     */
    public void fetchMoods(String userId, OnMoodsFetchedListener listener) {
        moodsRef.whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            listener.onMoodsFetched(snapshot.getDocuments());
                        } else {
                            listener.onError("No moods found!");
                        }
                    } else {
                        listener.onError("Failed to fetch moods!");
                    }
                });
    }

    /**
     * Deletes a mood and its associated image (if any) from Firestore and Firebase Storage.
     *
     * @param moodId   The ID of the mood to delete.
     * @param listener Listener for success or failure.
     */
    public void deleteMood(String moodId, OnMoodDeletedListener listener) {
        moodsRef.document(moodId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            // Delete the image from Firebase Storage
                            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                            imageRef.delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firebase Storage", "Image deleted successfully");
                                        deleteMoodDocument(moodId, listener);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("Firebase Storage", "Failed to delete image", e);
                                        listener.onError("Failed to delete image: " + e.getMessage());
                                    });
                        } else {
                            // Delete the mood document if there's no image
                            deleteMoodDocument(moodId, listener);
                        }
                    } else {
                        listener.onError("Mood document does not exist!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to fetch mood details", e);
                    listener.onError("Failed to fetch mood details: " + e.getMessage());
                });
    }

    /**
     * Deletes a mood document from Firestore.
     *
     * @param moodId   The ID of the mood to delete.
     * @param listener Listener for success or failure.
     */
    private void deleteMoodDocument(String moodId, OnMoodDeletedListener listener) {
        moodsRef.document(moodId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Mood deleted successfully");
                    listener.onMoodDeleted();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to delete mood", e);
                    listener.onError("Failed to delete mood: " + e.getMessage());
                });
    }

    /**
     * Listener for mood addition events.
     */
    public interface OnMoodAddedListener {
        void onMoodAdded();
        void onError(String errorMessage);
    }

    /**
     * Listener for mood update events.
     */
    public interface OnMoodUpdatedListener {
        void onMoodUpdated();
        void onError(String errorMessage);
    }

    /**
     * Listener for mood fetch events.
     */
    public interface OnMoodsFetchedListener {
        void onMoodsFetched(List<DocumentSnapshot> moodDocs);
        void onError(String errorMessage);
    }

    /**
     * Listener for mood deletion events.
     */
    public interface OnMoodDeletedListener {
        void onMoodDeleted();
        void onError(String errorMessage);
    }
}