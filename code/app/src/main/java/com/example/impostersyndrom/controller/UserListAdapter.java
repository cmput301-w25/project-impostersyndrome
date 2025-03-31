package com.example.impostersyndrom.controller;

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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.FollowRequest;
import com.example.impostersyndrom.model.UserData;
import com.example.impostersyndrom.view.UserProfileActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class UserListAdapter extends ArrayAdapter<UserData> {
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUsername;
    private static final String TAG = "UserListAdapter";

    public UserListAdapter(Context context, List<UserData> users) {
        super(context, 0, users);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId != null) {
            db.collection("users").document(currentUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            currentUsername = documentSnapshot.getString("username");
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching current username", e));
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_list_item, parent, false);
        }

        UserData user = getItem(position);
        if (user == null) {
            Log.e(TAG, "User data at position " + position + " is null");
            return convertView;
        }

        TextView userNameTextView = convertView.findViewById(R.id.usernameTextView);
        ImageButton followButton = convertView.findViewById(R.id.followButton);
        Button unfollowButton = convertView.findViewById(R.id.unfollowButton);
        Button requestedButton = convertView.findViewById(R.id.requestedButton);
        ShapeableImageView profileImage = convertView.findViewById(R.id.pfpView); // Correct ID

        // Set username
        userNameTextView.setText(user.username);

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

        // Fetch receiverId for follow logic
        db.collection("users")
                .whereEqualTo("username", user.username)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String receiverId = querySnapshot.getDocuments().get(0).getId();

                        // Set click listener on the username to navigate to user profile
                        userNameTextView.setOnClickListener(v -> {
                            Log.d(TAG, "Username clicked: " + user.username);
                            navigateToUserProfile(receiverId, user.username);
                        });

                        // Check follow status and set button visibility
                        checkFollowStatus(receiverId, user.username, followButton, requestedButton, unfollowButton);

                        // Add Click Listeners for buttons
                        followButton.setOnClickListener(v -> {
                            Log.d(TAG, "Follow button clicked for: " + user.username);
                            sendFollowRequest(receiverId, user.username, followButton, requestedButton, unfollowButton);
                        });

                        requestedButton.setOnClickListener(v -> {
                            Log.d(TAG, "Requested button clicked for: " + user.username);
                            cancelFollowRequest(receiverId, followButton, requestedButton);
                        });

                        unfollowButton.setOnClickListener(v -> {
                            Log.d(TAG, "Unfollow button clicked for: " + user.username);
                            unfollowUser(receiverId, followButton, requestedButton, unfollowButton);
                        });
                    } else {
                        Log.e(TAG, "No user found with username: " + user.username);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user document: " + e.getMessage()));

        return convertView;
    }

    private void navigateToUserProfile(String receiverId, String receiverUsername) {
        Log.d(TAG, "Attempting to navigate to profile for: " + receiverUsername);
        try {
            Intent intent = new Intent(getContext(), UserProfileActivity.class);
            intent.putExtra("userId", receiverId);
            intent.putExtra("username", receiverUsername);
            getContext().startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting UserProfileActivity", e);
            Toast.makeText(getContext(), "Error opening profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkFollowStatus(String receiverId, String receiverUsername, ImageButton followButton, Button requestedButton, Button unfollowButton) {
        if (receiverId == null || receiverId.isEmpty()) {
            Log.e(TAG, "receiverId is NULL or EMPTY! Follow status cannot be checked.");
            return;
        }

        // Check if there's a pending follow request
        db.collection("follow_requests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", receiverId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        followButton.setVisibility(View.GONE);
                        requestedButton.setVisibility(View.VISIBLE);
                        unfollowButton.setVisibility(View.GONE);
                    } else {
                        // Check if the user is already followed
                        db.collection("following")
                                .whereEqualTo("followerId", currentUserId)
                                .whereEqualTo("followingId", receiverId)
                                .get()
                                .addOnSuccessListener(followQuery -> {
                                    if (!followQuery.isEmpty()) {
                                        followButton.setVisibility(View.GONE);
                                        requestedButton.setVisibility(View.GONE);
                                        unfollowButton.setVisibility(View.VISIBLE);
                                    } else {
                                        followButton.setVisibility(View.VISIBLE);
                                        requestedButton.setVisibility(View.GONE);
                                        unfollowButton.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error checking followers: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking follow requests: " + e.getMessage()));
    }

    private void sendFollowRequest(String receiverId, String receiverUsername, ImageButton followButton, Button requestedButton, Button unfollowButton) {
        if (currentUsername == null) {
            Log.e(TAG, "Current username is null, cannot send follow request");
            return;
        }

        FollowRequest followRequest = new FollowRequest(
                currentUserId, receiverId, currentUsername, receiverUsername, "pending"
        );

        db.collection("follow_requests")
                .add(followRequest)
                .addOnSuccessListener(documentReference -> {
                    followButton.setVisibility(View.GONE);
                    requestedButton.setVisibility(View.VISIBLE);
                    unfollowButton.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error sending follow request: " + e.getMessage()));
    }

    private void cancelFollowRequest(String receiverId, ImageButton followButton, Button requestedButton) {
        db.collection("follow_requests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", receiverId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String requestId = querySnapshot.getDocuments().get(0).getId();
                        db.collection("follow_requests").document(requestId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    followButton.setVisibility(View.VISIBLE);
                                    requestedButton.setVisibility(View.GONE);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error canceling follow request: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching follow request: " + e.getMessage()));
    }

    private void unfollowUser(String receiverId, ImageButton followButton, Button requestedButton, Button unfollowButton) {
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("followingId", receiverId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String followId = querySnapshot.getDocuments().get(0).getId();
                        db.collection("following").document(followId).delete()
                                .addOnSuccessListener(aVoid -> {
                                    followButton.setVisibility(View.VISIBLE);
                                    requestedButton.setVisibility(View.GONE);
                                    unfollowButton.setVisibility(View.GONE);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error unfollowing user: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching following data: " + e.getMessage()));
    }
}