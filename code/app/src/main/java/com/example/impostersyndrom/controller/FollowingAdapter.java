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

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.view.UserProfileActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class FollowingAdapter extends ArrayAdapter<String> {
    private FirebaseFirestore db;
    private String currentUserId;
    private List<String> followingUsers;
    private TextView emptyMessage;
    private View rootView; // Root view for displaying Snackbar
    private static final String TAG = "FollowingAdapter";

    public FollowingAdapter(Context context, List<String> users, String currentUserId, View rootView) {
        super(context, 0, users);
        db = FirebaseFirestore.getInstance();
        this.currentUserId = currentUserId;
        this.followingUsers = users;
        this.rootView = rootView; // Initialize rootView
    }

    public void setEmptyMessageView(TextView emptyMessage) {
        this.emptyMessage = emptyMessage;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_following, parent, false);
        }

        String username = getItem(position);
        TextView usernameText = convertView.findViewById(R.id.usernameTextView);
        Button unfollowButton = convertView.findViewById(R.id.unfollowButton);

        usernameText.setText(username);

        // Make the TextView clickable and ensure it has proper focus state
        usernameText.setClickable(true);
        usernameText.setFocusable(true);

        // Set click listener on the username to navigate to user profile
        usernameText.setOnClickListener(v -> {
            Log.d(TAG, "Username clicked: " + username);
            navigateToUserProfile(username);
        });

        // Add click listener to the entire row as well for better UX
        View finalConvertView = convertView;
        convertView.setOnClickListener(v -> {
            // Only handle click if it's not on the unfollow button
            if (v == finalConvertView) {
                Log.d(TAG, "Row clicked: " + username);
                navigateToUserProfile(username);
            }
        });

        // ✅ Set Unfollow Click Listener
        unfollowButton.setOnClickListener(v -> {
            Log.d(TAG, "Unfollow button clicked for: " + username);

            // 🔹 Step 1: Find receiver ID from "users" collection
            db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener(userQuery -> {
                        if (!userQuery.isEmpty()) {
                            String receiverId = userQuery.getDocuments().get(0).getId();
                            Log.d(TAG, "Found receiverId: " + receiverId);

                            // 🔹 Step 2: Find and delete the follow relationship in Firestore
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
                                                        Log.d(TAG, "Successfully unfollowed: " + username);

                                                        // Remove user from UI
                                                        followingUsers.remove(position);
                                                        notifyDataSetChanged();

                                                        // Show empty message if list is now empty
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
                            Log.e(TAG, "No user found with username: " + username);
                            showMessage("Error: User not found");
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching user document", e));
        });

        return convertView;
    }

    private void navigateToUserProfile(String username) {
        Log.d(TAG, "Attempting to navigate to profile for: " + username);

        // First get the user ID from the username
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        // Get the first document that matches the username
                        String userId = querySnapshot.getDocuments().get(0).getId();
                        Log.d(TAG, "Found user ID: " + userId + " for username: " + username);

                        try {
                            // Create intent to open UserProfileActivity
                            Intent intent = new Intent(getContext(), UserProfileActivity.class);
                            intent.putExtra("userId", userId); // Match the key expected by UserProfileActivity
                            intent.putExtra("username", username); // Match the key expected by UserProfileActivity

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
        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
    }
}