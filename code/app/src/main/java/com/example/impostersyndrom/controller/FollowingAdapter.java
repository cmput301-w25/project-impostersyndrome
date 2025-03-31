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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.UserData;
import com.example.impostersyndrom.view.UserProfileActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Adapter for displaying a list of users the current user is following, with options to unfollow and view profiles.
 *
 * @author [Your Name]
 *
 */
public class FollowingAdapter extends ArrayAdapter<UserData> {
    private FirebaseFirestore db;
    private String currentUserId;
    private List<UserData> followingUsers;
    private TextView emptyMessage;
    private static final String TAG = "FollowingAdapter";

    /**
     * Constructs a new FollowingAdapter.
     *
     * @param context The context in which the adapter is running
     * @param users The list of users being followed
     * @param currentUserId The ID of the current user
     */
    public FollowingAdapter(Context context, List<UserData> users, String currentUserId) {
        super(context, 0, users);
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = currentUserId;
        this.followingUsers = users;
    }

    /**
     * Sets the TextView to display when the following list is empty.
     *
     * @param emptyMessage The TextView to show an empty message
     */
    public void setEmptyMessageView(TextView emptyMessage) {
        this.emptyMessage = emptyMessage;
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
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_following, parent, false);
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