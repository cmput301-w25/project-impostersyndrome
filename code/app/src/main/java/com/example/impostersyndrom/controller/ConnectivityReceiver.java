package com.example.impostersyndrom.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.view.MainActivity;

import java.util.List;
import java.util.Set;

/**
 * BroadcastReceiver that handles network connectivity changes to sync offline data.
 *
 * @author Rayan Zhi
 */
public class ConnectivityReceiver extends BroadcastReceiver {

    /**
     * Called when a connectivity change is detected, syncing offline data when connection is restored.
     *
     * @param context The context in which the receiver is running
     * @param intent The Intent being received
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if (isConnected) {
                MoodDataManager moodDataManager = new MoodDataManager();

                // Sync offline added moods
                List<Mood> offlineMoods = moodDataManager.getOfflineMoodsList(context);
                if (!offlineMoods.isEmpty()) {
                    for (Mood mood : offlineMoods) {
                        moodDataManager.addMood(mood, new MoodDataManager.OnMoodAddedListener() {
                            @Override
                            public void onMoodAdded() {
                                Log.d("ConnectivityReceiver", "Offline mood synced successfully");
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e("ConnectivityReceiver", "Failed to sync offline mood: " + errorMessage);
                            }
                        });
                    }
                    moodDataManager.clearOfflineMoodsList(context);
                }

                // Sync offline edits
                List<MoodDataManager.OfflineEdit> offlineEdits = moodDataManager.getOfflineEdits(context);
                if (!offlineEdits.isEmpty()) {
                    for (MoodDataManager.OfflineEdit edit : offlineEdits) {
                        moodDataManager.updateMood(edit.moodId, edit.updates, new MoodDataManager.OnMoodUpdatedListener() {
                            @Override
                            public void onMoodUpdated() {
                                Log.d("ConnectivityReceiver", "Offline edit synced successfully");
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e("ConnectivityReceiver", "Failed to sync offline edit: " + errorMessage);
                            }
                        });
                    }
                    moodDataManager.clearOfflineEdits(context);
                }

                // Sync offline deletes
                Set<String> deleteIds = moodDataManager.getOfflineDeletes(context);
                if (!deleteIds.isEmpty()) {
                    for (String moodId : deleteIds) {
                        moodDataManager.deleteMood(moodId, new MoodDataManager.OnMoodDeletedListener() {
                            @Override
                            public void onMoodDeleted() {
                                Log.d("ConnectivityReceiver", "Offline delete synced successfully");
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Log.e("ConnectivityReceiver", "Failed to sync offline delete: " + errorMessage);
                            }
                        });
                    }
                    moodDataManager.clearOfflineDeletes(context);
                }
            }
        }
    }
}
