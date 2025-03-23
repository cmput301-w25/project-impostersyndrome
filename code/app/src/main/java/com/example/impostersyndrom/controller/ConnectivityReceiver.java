package com.example.impostersyndrom.controller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.model.MoodDataManager;
import com.example.impostersyndrom.view.MainActivity;

import java.util.List;

public class ConnectivityReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("ConnectivityReceiver", "onReceive called");
        if (!NetworkUtils.isOffline(context)) {
            Log.d("ConnectivityReceiver", "Back online, syncing moods...");

            MoodDataManager moodDataManager = new MoodDataManager();
            List<Mood> offlineMoods = moodDataManager.getOfflineMoods(context);

            if (!offlineMoods.isEmpty()) {
                for (Mood mood : offlineMoods) {
                    moodDataManager.addMood(mood, new MoodDataManager.OnMoodAddedListener() {
                        @Override
                        public void onMoodAdded() {
                            Log.d("Sync", "Mood synced: " + mood.getReason());
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e("Sync", "Failed to sync mood: " + errorMessage);
                        }
                    });
                }

                // Clear and refresh only if something was synced
                moodDataManager.clearOfflineMoods(context);

                Intent refreshIntent = new Intent(context, MainActivity.class);
                refreshIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(refreshIntent);
            } else {
                Log.d("Sync", "No offline moods to sync.");
            }
        }
    }
}

