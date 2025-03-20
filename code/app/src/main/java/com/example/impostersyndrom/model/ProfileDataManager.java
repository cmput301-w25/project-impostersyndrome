package com.example.impostersyndrom.model;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileDataManager {
    private static final String TAG = "ProfileDataManager";
    private final FirebaseFirestore db;

    public ProfileDataManager() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetches user profile details from Firestore.
     *
     * @param userId   The ID of the user.
     * @param listener Listener for success or failure.
     */
    public void fetchUserProfile(String userId, OnProfileFetchedListener listener) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        listener.onProfileFetched(documentSnapshot);
                    } else {
                        listener.onError("User profile not found!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching profile data", e);
                    listener.onError("Failed to load profile data: " + e.getMessage());
                });
    }

    /**
     * Fetches the count of followers for a user.
     * Counts users who are following the specified user.
     *
     * @param userId   The ID of the user.
     * @param listener Listener for success or failure.
     */
    public void fetchFollowersCount(String userId, OnCountFetchedListener listener) {
        db.collection("following")
                .whereEqualTo("followingId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    listener.onCountFetched(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching followers count", e);
                    listener.onError("Failed to fetch followers count: " + e.getMessage());
                });
    }

    /**
     * Fetches the count of users the given user is following.
     * Counts users who the specified user is following.
     *
     * @param userId   The ID of the user.
     * @param listener Listener for success or failure.
     */
    public void fetchFollowingCount(String userId, OnCountFetchedListener listener) {
        db.collection("following")
                .whereEqualTo("followerId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    listener.onCountFetched(count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching following count", e);
                    listener.onError("Failed to fetch following count: " + e.getMessage());
                });
    }

    /**
     * Listener for profile fetch events.
     */
    public interface OnProfileFetchedListener {
        void onProfileFetched(DocumentSnapshot profileDoc);
        void onError(String errorMessage);
    }

    /**
     * Listener for count fetch events (followers/following).
     */
    public interface OnCountFetchedListener {
        void onCountFetched(int count);
        void onError(String errorMessage);
    }
}