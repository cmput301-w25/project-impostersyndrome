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
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.view.UserProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingRequestsAdapter extends ArrayAdapter<String> {
    private final FirebaseFirestore db;
    private final String currentUserId;
    private final String currentUsername;
    private static final String TAG = "PendingRequestsAdapter";

    public PendingRequestsAdapter(Context context, List<String> users, String currentUsername) {
        super(context, 0, users);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.currentUsername = currentUsername;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_pending_request, parent, false);
        }

        String senderUsername = getItem(position);
        TextView usernameText = convertView.findViewById(R.id.usernameTextView);
        Button acceptButton = convertView.findViewById(R.id.acceptButton);
        ImageButton declineButton = convertView.findViewById(R.id.rejectButton);

        usernameText.setText(senderUsername);

        // Make the TextView clickable and ensure it has proper focus state
        usernameText.setClickable(true);
        usernameText.setFocusable(true);

        // Set click listener on the username to navigate to user profile
        usernameText.setOnClickListener(v -> {
            Log.d(TAG, "Username clicked: " + senderUsername);
            navigateToUserProfile(senderUsername);
        });

        // Add click listener to the entire row as well for better UX
        View finalConvertView = convertView;
        convertView.setOnClickListener(v -> {
            // Only handle click if it's not on a button
            if (v == finalConvertView) {
                Log.d(TAG, "Row clicked: " + senderUsername);
                navigateToUserProfile(senderUsername);
            }
        });

        acceptButton.setOnClickListener(v -> acceptFollowRequest(senderUsername));
        declineButton.setOnClickListener(v -> declineFollowRequest(senderUsername));

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
                            Toast.makeText(getContext(), "Error opening profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.d(TAG, "User not found with username: " + username);
                        Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding user: " + e.getMessage());
                    Toast.makeText(getContext(), "Error finding user", Toast.LENGTH_SHORT).show();
                });
    }

    private void acceptFollowRequest(String senderUsername) {
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
                                        removeRequest(senderUsername);
                                        Toast.makeText(getContext(), "Follow request accepted!", Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(getContext(), "Error following user", Toast.LENGTH_SHORT).show());
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to accept request", Toast.LENGTH_SHORT).show());
    }

    private void declineFollowRequest(String senderUsername) {
        removeRequest(senderUsername);
        Toast.makeText(getContext(), "Follow request declined", Toast.LENGTH_SHORT).show();
    }

    private void removeRequest(String senderUsername) {
        db.collection("follow_requests")
                .whereEqualTo("senderUsername", senderUsername)
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        db.collection("follow_requests").document(doc.getId()).delete();
                    }
                    remove(senderUsername);
                    notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to remove request", Toast.LENGTH_SHORT).show());
    }
}