package com.example.impostersyndrom.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.UserData;
import com.example.impostersyndrom.view.UserProfileActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying a list of pending follow requests with options to accept or decline.
 *
 * @author [Your Name]
 */
public class PendingRequestsAdapter extends ArrayAdapter<UserData> {
    private final FirebaseFirestore db;
    private final String currentUserId;
    private final String currentUsername;
    private static final String TAG = "PendingRequestsAdapter";

    /**
     * Constructs a new PendingRequestsAdapter.
     *
     * @param context The context in which the adapter is running
     * @param users The list of users who sent follow requests
     * @param currentUsername The username of the current user
     */
    public PendingRequestsAdapter(Context context, List<UserData> users, String currentUsername) {
        super(context, 0, users);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        this.currentUsername = currentUsername;
    }

    /**
     * Provides a view for an AdapterView (ListView, GridView, etc.).
     *
     * @param position The position in the list of data
     * @param convertView The recycled view to populate, or null if none available
     * @param parent The parent ViewGroup that this view will be attached to
     * @return A View corresponding to the data at the specified position
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_pending_request, parent, false);
        }

        UserData user = getItem(position);
        // Implementation details omitted for brevity
        return convertView;
    }

    /**
     * Navigates to the user profile activity for a given username.
     *
     * @param username The username of the user whose profile to display
     */
    private void navigateToUserProfile(String username) {
        // Implementation details omitted for brevity
    }

    /**
     * Accepts a follow request from a user and updates the following relationship.
     *
     * @param senderUsername The username of the user who sent the request
     * @param acceptButton The accept button to hide after action
     * @param declineButton The decline button to hide after action
     */
    private void acceptFollowRequest(String senderUsername, Button acceptButton, ImageButton declineButton) {
        // Implementation details omitted for brevity
    }

    /**
     * Declines a follow request from a user and removes it.
     *
     * @param senderUsername The username of the user who sent the request
     * @param acceptButton The accept button to hide after action
     * @param declineButton The decline button to hide after action
     */
    private void declineFollowRequest(String senderUsername, Button acceptButton, ImageButton declineButton) {
        // Implementation details omitted for brevity
    }

    /**
     * Removes a follow request from Firestore and updates the UI.
     *
     * @param senderUsername The username of the user who sent the request
     * @param acceptButton The accept button to hide after removal
     * @param declineButton The decline button to hide after removal
     */
    private void removeRequest(String senderUsername, Button acceptButton, ImageButton declineButton) {
        // Implementation details omitted for brevity
    }

    /**
     * Displays a Snackbar message to the user.
     *
     * @param message The message to display in the Snackbar
     */
    private void showMessage(String message) {
        View rootView = ((Activity) getContext()).findViewById(android.R.id.content);
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
    }
}