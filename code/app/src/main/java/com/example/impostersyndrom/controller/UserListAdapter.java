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

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.model.FollowRequest;
import com.example.impostersyndrom.view.UserProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class UserListAdapter extends ArrayAdapter<String> {
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUsername;
    private static final String TAG = "UserListAdapter";

    public UserListAdapter(Context context, List<String> users) {
        super(context, 0, users);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch current user's username
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                    }
                });
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_list_item, parent, false);
        }

        String receiverUsername = getItem(position);
        TextView userNameTextView = convertView.findViewById(R.id.usernameTextView);
        ImageButton followButton = convertView.findViewById(R.id.followButton);
        Button unfollowButton = convertView.findViewById(R.id.unfollowButton);
        Button requestedButton = convertView.findViewById(R.id.requestedButton);

        userNameTextView.setText(receiverUsername);

        // DEBUG: Log the username we are searching for
        Log.d("DEBUG", "Searching for user: " + receiverUsername);

        db.collection("users")
                .whereEqualTo("username", receiverUsername)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String receiverId = querySnapshot.getDocuments().get(0).getId();

                        // DEBUG: Log the retrieved receiverId
                        Log.d("DEBUG", "Found receiverId for " + receiverUsername + ": " + receiverId);

                        // Set click listener on the username to navigate to user profile
                        userNameTextView.setOnClickListener(v -> {
                            Log.d(TAG, "Username clicked: " + receiverUsername);
                            navigateToUserProfile(receiverId, receiverUsername);
                        });

                        // Check follow status and set button visibility
                        checkFollowStatus(receiverId, receiverUsername, followButton, requestedButton, unfollowButton);

                        // Add Click Listeners for buttons
                        followButton.setOnClickListener(v -> {
                            Log.d("DEBUG", "Follow button clicked for: " + receiverUsername);
                            sendFollowRequest(receiverId, receiverUsername, followButton, requestedButton, unfollowButton);
                        });

                        requestedButton.setOnClickListener(v -> {
                            Log.d("DEBUG", "Requested button clicked for: " + receiverUsername);
                            cancelFollowRequest(receiverId, followButton, requestedButton);
                        });

                        unfollowButton.setOnClickListener(v -> {
                            Log.d("DEBUG", "Unfollow button clicked for: " + receiverUsername);
                            unfollowUser(receiverId, followButton, requestedButton, unfollowButton);
                        });

                    } else {
                        Log.e("DEBUG", "No user found with username: " + receiverUsername);
                    }
                })
                .addOnFailureListener(e -> Log.e("DEBUG", "Error fetching user document", e));

        return convertView;
    }

    private void navigateToUserProfile(String receiverId, String receiverUsername) {
        Log.d(TAG, "Attempting to navigate to profile for: " + receiverUsername);

        try {
            // Create intent to open UserProfileActivity
            Intent intent = new Intent(getContext(), UserProfileActivity.class);
            intent.putExtra("userId", receiverId); // Pass the userId
            intent.putExtra("username", receiverUsername); // Pass the username

            Log.d(TAG, "Starting UserProfileActivity with userId: " + receiverId);
            getContext().startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error starting UserProfileActivity", e);
            Toast.makeText(getContext(), "Error opening profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkFollowStatus(String receiverId, String receiverUsername, ImageButton followButton, Button requestedButton, Button unfollowButton) {
        if (receiverId == null || receiverId.isEmpty()) {
            Log.e("DEBUG", "❌ receiverId is NULL or EMPTY! Follow status cannot be checked.");
            return;
        }

        Log.d("DEBUG", "🔍 Checking follow status for receiverId: " + receiverId);

        // First, check if there's a pending follow request
        db.collection("follow_requests")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", receiverId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Log.d("DEBUG", "📌 Follow request exists! Showing Requested button.");
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
                                        Log.d("DEBUG", "✅ User is FOLLOWED! Showing Unfollow button.");
                                        followButton.setVisibility(View.GONE);
                                        requestedButton.setVisibility(View.GONE);
                                        unfollowButton.setVisibility(View.VISIBLE);
                                    } else {
                                        Log.d("DEBUG", "❌ User NOT found in followers collection. Showing Follow button.");
                                        followButton.setVisibility(View.VISIBLE);
                                        requestedButton.setVisibility(View.GONE);
                                        unfollowButton.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e("DEBUG", "⚠️ Error checking followers", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("DEBUG", "⚠️ Error checking follow requests", e));
    }

    private void sendFollowRequest(String receiverId, String receiverUsername, ImageButton followButton, Button requestedButton, Button unfollowButton) {
        FollowRequest followRequest = new FollowRequest(
                currentUserId, receiverId, currentUsername, receiverUsername, "pending"
        );

        db.collection("follow_requests")
                .add(followRequest)
                .addOnSuccessListener(documentReference -> {
                    followButton.setVisibility(View.GONE);
                    requestedButton.setVisibility(View.VISIBLE);
                    unfollowButton.setVisibility(View.GONE);
                });
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
                                });
                    }
                });
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
                                });
                    }
                });
    }
}