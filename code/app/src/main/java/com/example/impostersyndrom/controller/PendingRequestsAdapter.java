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

public class PendingRequestsAdapter extends ArrayAdapter<UserData> {
    private final FirebaseFirestore db;
    private final String currentUserId;
    private final String currentUsername;
    private static final String TAG = "PendingRequestsAdapter";

    public PendingRequestsAdapter(Context context, List<UserData> users, String currentUsername) {
        super(context, 0, users);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        this.currentUsername = currentUsername;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_pending_request, parent, false);
        }

        UserData user = getItem(position);
        if (user == null) {
            Log.e(TAG, "User data at position " + position + " is null");
            return convertView;
        }

        TextView usernameText = convertView.findViewById(R.id.usernameTextView);
        Button acceptButton = convertView.findViewById(R.id.acceptButton);
        ImageButton declineButton = convertView.findViewById(R.id.rejectButton);
        ShapeableImageView profileImage = convertView.findViewById(R.id.profileImage);

        // Set username
        usernameText.setText(user.username);

        // Load profile picture
        if (user.profileImageUrl != null && !user.profileImageUrl.isEmpty()) {
            Glide.with(getContext())
                    .load(user.profileImageUrl)
                    .placeholder(R.drawable.img_default_person)
                    .error(R.drawable.img_default_person)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.img_default_person);
        }

        // Make the TextView clickable and ensure it has proper focus state
        usernameText.setClickable(true);
        usernameText.setFocusable(true);

        // Set click listener on the username to navigate to user profile
        usernameText.setOnClickListener(v -> {
            Log.d(TAG, "Username clicked: " + user.username);
            navigateToUserProfile(user.username);
        });

        // Add click listener to the entire row as well for better UX
        View finalConvertView = convertView;
        convertView.setOnClickListener(v -> {
            if (v == finalConvertView) {
                Log.d(TAG, "Row clicked: " + user.username);
                navigateToUserProfile(user.username);
            }
        });

        acceptButton.setOnClickListener(v -> acceptFollowRequest(user.username, acceptButton, declineButton));
        declineButton.setOnClickListener(v -> declineFollowRequest(user.username, acceptButton, declineButton));

        return convertView;
    }

    private void navigateToUserProfile(String username) {
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
                            getContext().startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Error starting UserProfileActivity", e);
                            showMessage("Error opening profile");
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

    private void acceptFollowRequest(String senderUsername, Button acceptButton, ImageButton declineButton) {
        db.collection("follow_requests")
                .whereEqualTo("senderUsername", senderUsername)
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String senderId = doc.getString("senderId");

                            Map<String, Object> followData = new HashMap<>();
                            followData.put("followerId", senderId);
                            followData.put("followerUsername", senderUsername);
                            followData.put("followingId", currentUserId);
                            followData.put("followingUsername", currentUsername);

                            db.collection("following").add(followData)
                                    .addOnSuccessListener(documentReference -> {
                                        removeRequest(senderUsername, acceptButton, declineButton);
                                        showMessage("Follow request accepted!");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error adding to following: " + e.getMessage());
                                        showMessage("Error accepting request");
                                    });
                        }
                    } else {
                        Log.e(TAG, "No matching follow request found for: " + senderUsername);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching follow request: " + e.getMessage());
                    showMessage("Failed to accept request");
                });
    }

    private void declineFollowRequest(String senderUsername, Button acceptButton, ImageButton declineButton) {
        removeRequest(senderUsername, acceptButton, declineButton);
        showMessage("Follow request declined");
    }

    private void removeRequest(String senderUsername, Button acceptButton, ImageButton declineButton) {
        db.collection("follow_requests")
                .whereEqualTo("senderUsername", senderUsername)
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        db.collection("follow_requests").document(doc.getId()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Follow request removed for: " + senderUsername);
                                    acceptButton.setVisibility(View.GONE);
                                    declineButton.setVisibility(View.GONE);
                                    // Find and remove the item from the list
                                    for (int i = 0; i < getCount(); i++) {
                                        UserData user = getItem(i);
                                        if (user != null && user.username.equals(senderUsername)) {
                                            remove(user);
                                            break;
                                        }
                                    }
                                    notifyDataSetChanged();
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error deleting follow request: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching follow request to remove: " + e.getMessage());
                    showMessage("Failed to remove request");
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