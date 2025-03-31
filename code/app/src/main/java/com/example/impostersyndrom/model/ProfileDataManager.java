package com.example.impostersyndrom.model;

import android.util.Log;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Manages profile-related data operations with Firestore, including user profile details and follow counts.
 *
 * @author [Your Name]
 */
public class ProfileDataManager {
    private static final String TAG = "ProfileDataManager";
    private final FirebaseFirestore db;

    /**
     * Constructs a new ProfileDataManager with a Firestore instance.
     */
    public ProfileDataManager() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Fetches a user's profile details from Firestore.
     *
     * @param userId The ID of the user
     * @param listener Listener for success or failure
     */
    public void fetchUserProfile(String userId, OnProfileFetchedListener listener) {
        // Implementation details omitted for brevity
    }

    /**
     * Fetches the count of followers for a user (users following the specified user).
     *
     * @param userId The ID of the user
     * @param listener Listener for success or failure
     */
    public void fetchFollowersCount(String userId, OnCountFetchedListener listener) {
        // Implementation details omitted for brevity
    }

    /**
     * Fetches the count of users the specified user is following.
     *
     * @param userId The ID of the user
     * @param listener Listener for success or failure
     */
    public void fetchFollowingCount(String userId, OnCountFetchedListener listener) {
        // Implementation details omitted for brevity
    }

    /**
     * Listener interface for profile fetch events.
     */
    public interface OnProfileFetchedListener {
        /**
         * Called when the profile is successfully fetched.
         *
         * @param profileDoc The Firestore document snapshot containing profile data
         */
        void onProfileFetched(DocumentSnapshot profileDoc);

        /**
         * Called when an error occurs during profile fetch.
         *
         * @param errorMessage The error message
         */
        void onError(String errorMessage);
    }

    /**
     * Listener interface for count fetch events (followers or following).
     */
    public interface OnCountFetchedListener {
        /**
         * Called when the count is successfully fetched.
         *
         * @param count The number of followers or following
         */
        void onCountFetched(int count);

        /**
         * Called when an error occurs during count fetch.
         *
         * @param errorMessage The error message
         */
        void onError(String errorMessage);
    }
}