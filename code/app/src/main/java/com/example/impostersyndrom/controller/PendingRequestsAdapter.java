package com.example.impostersyndrom.controller;

import android.content.Context;
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

        acceptButton.setOnClickListener(v -> acceptFollowRequest(senderUsername));
        declineButton.setOnClickListener(v -> declineFollowRequest(senderUsername));

        return convertView;
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
