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

public class FollowingAdapter extends ArrayAdapter<UserData> {
    public FirebaseFirestore db;
    private String currentUserId;
    private List<UserData> followingUsers;
    private TextView emptyMessage;
    private static final String TAG = "FollowingAdapter";

    public FollowingAdapter(Context context, List<UserData> users, String currentUserId) {
        super(context, 0, users);
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = currentUserId;
        this.followingUsers = users;
    }

    public void setEmptyMessageView(TextView emptyMessage) {
        this.emptyMessage = emptyMessage;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_following, parent, false);
        }

        UserData user = getItem(position);
        TextView usernameText = convertView.findViewById(R.id.usernameTextView);
        Button unfollowButton = convertView.findViewById(R.id.unfollowButton);
        ShapeableImageView profileImage = convertView.findViewById(R.id.profileImage);

        // Set username
        usernameText.setText(user.username);

        // Load profile picture
        if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
            Glide.with(getContext())
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.default_person)
                    .error(R.drawable.default_person)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.default_person);
        }

        // Make the TextView clickable
        usernameText.setClickable(true);
        usernameText.setFocusable(true);

        // Set click listener on the username to navigate to user profile
        usernameText.setOnClickListener(v -> {
            Log.d(TAG, "Username clicked: " + user.username);
            navigateToUserProfile(user.username);
        });

        // Add click listener to the entire row
        View finalConvertView = convertView;
        convertView.setOnClickListener(v -> {
            if (v == finalConvertView) {
                Log.d(TAG, "Row clicked: " + user.username);
                navigateToUserProfile(user.username);
            }
        });

        // Unfollow Click Listener
        unfollowButton.setOnClickListener(v -> {
            Log.d(TAG, "Unfollow button clicked for: " + user.username);

            // Step 1: Find receiver ID from "users" collection
            db.collection("users")
                    .whereEqualTo("username", user.username)
                    .get()
                    .addOnSuccessListener(userQuery -> {
                        if (!userQuery.isEmpty()) {
                            String receiverId = userQuery.getDocuments().get(0).getId();
                            Log.d(TAG, "Found receiverId: " + receiverId);

                            // Step 2: Find and delete the follow relationship
                            db.collection("following")
                                    .whereEqualTo("followerId", currentUserId)
                                    .whereEqualTo("followingId", receiverId)
                                    .get()
                                    .addOnSuccessListener(followQuery -> {
                                        if (!followQuery.isEmpty()) {
                                            String followId = followQuery.getDocuments().get(0).getId();
                                            Log.d(TAG, "Deleting follow document ID: " + followId);

                                            db.collection("following").document(followId).delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d(TAG, "Successfully unfollowed: " + user.username);
                                                        followingUsers.remove(position);
                                                        notifyDataSetChanged();

                                                        if (followingUsers.isEmpty() && emptyMessage != null) {
                                                            ((Activity) getContext()).runOnUiThread(() -> {
                                                                emptyMessage.setText("You're not following anyone yet");
                                                                emptyMessage.setVisibility(View.VISIBLE);
                                                            });
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Log.e(TAG, "Error deleting follow document", e);
                                                        showMessage("Error unfollowing");
                                                    });
                                        } else {
                                            Log.e(TAG, "No matching follow document found");
                                            showMessage("Error: No match found");
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error searching follow collection", e));
                        } else {
                            Log.e(TAG, "No user found with username: " + user.username);
                            showMessage("Error: User not found");
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching user document", e));
        });

        return convertView;
    }

    public void navigateToUserProfile(String username) {
        Log.d(TAG, "Attempting to navigate to profile for: " + username);

        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String userId = querySnapshot.getDocuments().get(0).getId();
                        Log.d(TAG, "Found user ID: " + userId + " for username: " + username);

                        try {
                            Intent intent = new Intent(getContext(), UserProfileActivity.class);
                            intent.putExtra("userId", userId);
                            intent.putExtra("username", username);
                            Log.d(TAG, "Starting UserProfileActivity with userId: " + userId);
                            getContext().startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Error starting UserProfileActivity", e);
                            showMessage("Error opening profile: " + e.getMessage());
                        }
                    } else {
                        Log.d(TAG, "User not found with username: " + username);
                        showMessage("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user: " + e.getMessage());
                    showMessage("Error finding user");
                });
    }

    /**
     * Displays a Snackbar message.
     *
     * @param message The message to display.
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
