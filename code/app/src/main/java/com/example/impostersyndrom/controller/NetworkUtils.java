package com.example.impostersyndrom.controller;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Utility class for checking network connectivity status.
 *
 * @author [Your Name]
 */
public class NetworkUtils {

    /**
     * Checks if the device is currently offline.
     *
     * @param context The context used to access system services
     * @return True if the device is offline, false if connected
     */
    public static boolean isOffline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return !(activeNetwork != null && activeNetwork.isConnected());
    }
}