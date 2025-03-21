package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.ListView;
import android.widget.TextView;
import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.FollowingAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FollowingFragment extends Fragment {
    private ListView listView;
    private TextView emptyMessage;
    private List<String> followingUsers = new ArrayList<>();
    private FollowingAdapter followingAdapter;
    private FirebaseFirestore db;
    private String currentUserId;

    public FollowingFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        listView = view.findViewById(R.id.listView);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Initialize adapter with empty list
        followingAdapter = new FollowingAdapter(requireContext(), followingUsers, currentUserId);
        followingAdapter.setEmptyMessageView(emptyMessage);
        listView.setAdapter(followingAdapter);

        loadFollowingUsers();
        return view;
    }

    private void loadFollowingUsers() {
        db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    followingUsers.clear(); // Clear old data to avoid duplicates

                    List<String> userIds = new ArrayList<>();

                    // Step 1: Get the list of user IDs we are following
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String followingId = doc.getString("followingId"); // Get the user ID
                        if (followingId != null) {
                            userIds.add(followingId);
                        }
                    }

                    // Step 2: Fetch usernames from the "users" collection
                    if (userIds.isEmpty()) {
                        emptyMessage.setText("You're not following anyone yet");
                        emptyMessage.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                    } else {
                        emptyMessage.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);

                        for (String userId : userIds) {
                            db.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        if (userDoc.exists()) {
                                            String username = userDoc.getString("username");
                                            if (username != null) {
                                                followingUsers.add(username);
                                            }
                                        }

                                        // Update adapter only when all usernames are fetched
                                        if (followingUsers.size() == userIds.size()) {
                                            followingAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("FollowingFragment", "Error fetching username: " + e.getMessage());
                                    });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    emptyMessage.setText("Failed to load following list.");
                    emptyMessage.setVisibility(View.VISIBLE);
                });
    }
}