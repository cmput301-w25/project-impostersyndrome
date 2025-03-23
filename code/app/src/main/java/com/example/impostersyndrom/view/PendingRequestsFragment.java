package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.PendingRequestsAdapter;
import com.example.impostersyndrom.model.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PendingRequestsFragment extends Fragment {
    private ListView listView;
    private TextView emptyMessage;
    private List<UserData> pendingRequests = new ArrayList<>(); // Changed to List<UserData>
    private PendingRequestsAdapter pendingRequestsAdapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUsername;
    private ListenerRegistration pendingListener;
    private static final String TAG = "PendingRequestsFragment";

    public PendingRequestsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        listView = view.findViewById(R.id.listView);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null");
            emptyMessage.setText("Please log in to view pending requests");
            emptyMessage.setVisibility(View.VISIBLE);
            return view;
        }

        // Get current username and initialize adapter
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                        pendingRequestsAdapter = new PendingRequestsAdapter(requireContext(), pendingRequests, currentUsername);
                        listView.setAdapter(pendingRequestsAdapter);
                        loadPendingRequests();
                    } else {
                        Log.e(TAG, "Current user document not found");
                        emptyMessage.setText("User data not found");
                        emptyMessage.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching username: " + e.getMessage());
                    emptyMessage.setText("Failed to load user data");
                    emptyMessage.setVisibility(View.VISIBLE);
                });

        return view;
    }

    private void loadPendingRequests() {
        if (pendingListener != null) {
            pendingListener.remove();
        }

        pendingListener = db.collection("follow_requests")
                .whereEqualTo("receiverId", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading pending requests: " + error.getMessage());
                        emptyMessage.setText("Failed to load pending requests");
                        emptyMessage.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (querySnapshot == null) {
                        Log.e(TAG, "Query snapshot is null");
                        return;
                    }

                    pendingRequests.clear();
                    List<String> senderIds = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String senderId = doc.getString("senderId");
                        if (senderId != null) {
                            senderIds.add(senderId);
                        }
                    }

                    if (senderIds.isEmpty()) {
                        emptyMessage.setText("No pending follow requests");
                        emptyMessage.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                        pendingRequestsAdapter.notifyDataSetChanged();
                    } else {
                        emptyMessage.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                        fetchSenderDetails(senderIds);
                    }
                });
    }

    private void fetchSenderDetails(List<String> senderIds) {
        for (String senderId : senderIds) {
            db.collection("users").document(senderId)
                    .addSnapshotListener((userDoc, error) -> {
                        if (error != null) {
                            Log.e(TAG, "Error fetching sender data: " + error.getMessage());
                            return;
                        }

                        if (userDoc != null && userDoc.exists()) {
                            String username = userDoc.getString("username");
                            String profileImageUrl = userDoc.getString("profileImageUrl");
                            if (username != null) {
                                boolean userExists = false;
                                for (UserData existingUser : pendingRequests) {
                                    if (existingUser.username.equals(username)) {
                                        existingUser.profileImageUrl = profileImageUrl; // Update pfp
                                        userExists = true;
                                        break;
                                    }
                                }
                                if (!userExists) {
                                    pendingRequests.add(new UserData(username, profileImageUrl));
                                }
                                pendingRequestsAdapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (pendingListener != null) {
            pendingListener.remove();
        }
    }
}