package com.example.impostersyndrom.model;

import android.util.Log;
import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MoodDataManager {
    private final CollectionReference moodsRef;


    public MoodDataManager() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        moodsRef = db.collection("moods");
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
                            try {
                                // Delete the image from Firebase Storage
                                StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                                imageRef.delete()
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("Firebase Storage", "Image deleted successfully");
                                            deleteMoodDocument(moodId, listener);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("Firebase Storage", "Failed to delete image", e);
                                            // Even if image deletion fails, we should still try to delete the document
                                            deleteMoodDocument(moodId, listener);
                                        });
                            } catch (IllegalArgumentException e) {
                                Log.e("Firebase Storage", "Invalid storage URL: " + imageUrl, e);
                                // If the URL is invalid, just delete the document
                                deleteMoodDocument(moodId, listener);
                            }
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

    public void saveMoodOffline(Context context, Mood mood) {
        SharedPreferences prefs = context.getSharedPreferences("OfflineMoods", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("moodList", "[]");
        List<Mood> moodList = gson.fromJson(json, new TypeToken<List<Mood>>() {}.getType());
        if (moodList == null) {
            moodList = new ArrayList<>();
        }
        moodList.add(mood);
        prefs.edit().putString("moodList", gson.toJson(moodList)).apply();
        Log.d("OfflineMood", "Offline mood stored. Total offline moods: " + moodList.size());
    }

    public List<Mood> getOfflineMoodsList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("OfflineMoods", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("moodList", "[]");
        return gson.fromJson(json, new TypeToken<List<Mood>>() {}.getType());
    }

    public void clearOfflineMoodsList(Context context) {
        context.getSharedPreferences("OfflineMoods", Context.MODE_PRIVATE)
                .edit().remove("moodList").apply();
    }

    // Offline edits
    public void saveOfflineEdit(Context context, String moodId, Map<String, Object> updates) {
        SharedPreferences prefs = context.getSharedPreferences("OfflineEdits", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("editList", "[]");
        List<OfflineEdit> editList = gson.fromJson(json, new TypeToken<List<OfflineEdit>>(){}.getType());
        if (editList == null) {
            editList = new ArrayList<>();
        }
        editList.add(new OfflineEdit(moodId, updates));
        prefs.edit().putString("editList", gson.toJson(editList)).apply();
        Log.d("OfflineEdit", "Saved offline edit for moodId: " + moodId);
    }

    public List<OfflineEdit> getOfflineEdits(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("OfflineEdits", Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = prefs.getString("editList", "[]");
        return gson.fromJson(json, new TypeToken<List<OfflineEdit>>(){}.getType());
    }

    public void clearOfflineEdits(Context context) {
        context.getSharedPreferences("OfflineEdits", Context.MODE_PRIVATE)
                .edit().remove("editList").apply();
    }

    // Offline deletes
    public void saveOfflineDelete(Context context, String moodId) {
        SharedPreferences prefs = context.getSharedPreferences("OfflineDeletes", Context.MODE_PRIVATE);
        Set<String> deletes = new HashSet<>(prefs.getStringSet("deleteSet", new HashSet<>()));
        deletes.add(moodId);
        prefs.edit().putStringSet("deleteSet", deletes).apply();
        Log.d("OfflineDelete", "Saved offline delete for moodId: " + moodId);
    }

    public Set<String> getOfflineDeletes(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("OfflineDeletes", Context.MODE_PRIVATE);
        return prefs.getStringSet("deleteSet", new HashSet<>());
    }

    public void clearOfflineDeletes(Context context) {
        context.getSharedPreferences("OfflineDeletes", Context.MODE_PRIVATE)
                .edit().remove("deleteSet").apply();
    }

    // Helper class
    public static class OfflineEdit {
        public String moodId;
        public Map<String, Object> updates;

        public OfflineEdit(String moodId, Map<String, Object> updates) {
            this.moodId = moodId;
            this.updates = updates;
        }
    }

    public static class OfflineMood {
        public String emoji;
        public String reason;
        public String group;
        public int color;
        public String imageUrl;
        public long timestamp;
        public boolean privateMood;

        public OfflineMood(String emoji, String reason, String group, int color, String imageUrl, long timestamp, boolean privateMood) {
            this.emoji = emoji;
            this.reason = reason;
            this.group = group;
            this.color = color;
            this.imageUrl = imageUrl;
            this.timestamp = timestamp;
            this.privateMood = privateMood;
        }
    }

    private static final String PREF_OFFLINE_EDITS = "offline_edits";
    private static final String PREF_OFFLINE_DELETES = "offline_deletes";
    private static final String PREF_OFFLINE_MOODS = "offline_moods";
    private static final String PREF_OFFLINE_MOODS_COUNT = "offline_moods_count";

    public void saveOfflineMood(Context context, OfflineMood mood) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_MOODS, Context.MODE_PRIVATE);
        int count = prefs.getInt(PREF_OFFLINE_MOODS_COUNT, 0);
        String moodJson = new Gson().toJson(mood);
        prefs.edit()
                .putString("mood_" + count, moodJson)
                .putInt(PREF_OFFLINE_MOODS_COUNT, count + 1)
                .apply();
    }

    public List<OfflineMood> getOfflineMoods(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_MOODS, Context.MODE_PRIVATE);
        int count = prefs.getInt(PREF_OFFLINE_MOODS_COUNT, 0);
        List<OfflineMood> moods = new ArrayList<>();
        Gson gson = new Gson();

        for (int i = 0; i < count; i++) {
            String moodJson = prefs.getString("mood_" + i, null);
            if (moodJson != null) {
                OfflineMood mood = gson.fromJson(moodJson, OfflineMood.class);
                moods.add(mood);
            }
        }

        return moods;
    }

    public void clearOfflineMoods(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_OFFLINE_MOODS, Context.MODE_PRIVATE);
        int count = prefs.getInt(PREF_OFFLINE_MOODS_COUNT, 0);
        SharedPreferences.Editor editor = prefs.edit();
        
        for (int i = 0; i < count; i++) {
            editor.remove("mood_" + i);
        }
        
        editor.putInt(PREF_OFFLINE_MOODS_COUNT, 0)
                .apply();
    }
}