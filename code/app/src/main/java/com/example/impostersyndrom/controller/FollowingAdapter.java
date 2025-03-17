package com.example.impostersyndrom.controller;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.impostersyndrom.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;

public class FollowingAdapter extends ArrayAdapter<String> {
    private FirebaseFirestore db;
    private String currentUserId;
    private List<String> followingUsers;
    private TextView emptyMessage;

    public FollowingAdapter(Context context, List<String> users, String currentUserId) {
        super(context, 0, users);
        db = FirebaseFirestore.getInstance();
        this.currentUserId = currentUserId;
        this.followingUsers = users;
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

        // ✅ Set Unfollow Click Listener
        unfollowButton.setOnClickListener(v -> {
            Log.d("FollowingAdapter", "Unfollow button clicked for: " + username);

            // 🔹 Step 1: Find receiver ID from "users" collection
            db.collection("users")
                    .whereEqualTo("username", username)
                    .get()
                    .addOnSuccessListener(userQuery -> {
                        if (!userQuery.isEmpty()) {
                            String receiverId = userQuery.getDocuments().get(0).getId();
                            Log.d("FollowingAdapter", "Found receiverId: " + receiverId);

                            // 🔹 Step 2: Find and delete the follow relationship in Firestore
                            db.collection("following")
                                    .whereEqualTo("followerId", currentUserId)
                                    .whereEqualTo("followingId", receiverId)
                                    .get()
                                    .addOnSuccessListener(followQuery -> {
                                        if (!followQuery.isEmpty()) {
                                            String followId = followQuery.getDocuments().get(0).getId();
                                            Log.d("FollowingAdapter", "Deleting follow document ID: " + followId);

                                            db.collection("following").document(followId).delete()
                                                    .addOnSuccessListener(aVoid -> {
                                                        Log.d("FollowingAdapter", "Successfully unfollowed: " + username);

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
                                                        Log.e("FollowingAdapter", "Error deleting follow document", e);
                                                        Toast.makeText(getContext(), "Error unfollowing", Toast.LENGTH_SHORT).show();
                                                    });
                                        } else {
                                            Log.e("FollowingAdapter", "No matching follow document found");
                                            Toast.makeText(getContext(), "Error: No match found", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e("FollowingAdapter", "Error searching follow collection", e));

                        } else {
                            Log.e("FollowingAdapter", "No user found with username: " + username);
                            Toast.makeText(getContext(), "Error: User not found", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FollowingAdapter", "Error fetching user document", e));
        });

        return convertView;
    }
}
