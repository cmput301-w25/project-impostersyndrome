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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PendingRequestsFragment extends Fragment {
    private ListView listView;
    private TextView emptyMessage;
    private List<String> pendingRequests = new ArrayList<>();
    private PendingRequestsAdapter pendingRequestsAdapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private String currentUsername;

    public PendingRequestsFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        listView = view.findViewById(R.id.listView);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get current username
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUsername = documentSnapshot.getString("username");
                        loadPendingRequests();
                    }
                })
                .addOnFailureListener(e -> Log.e("PendingRequestsFragment", "Error fetching username: " + e.getMessage()));

        return view;
    }

    private void loadPendingRequests() {
        db.collection("follow_requests")
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pendingRequests.clear(); // Clear old data to avoid duplicates

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String senderUsername = doc.getString("senderUsername");
                        if (senderUsername != null) {
                            pendingRequests.add(senderUsername);
                        }
                    }

                    if (pendingRequests.isEmpty()) {
                        emptyMessage.setText("No pending follow requests");
                        emptyMessage.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                    } else {
                        emptyMessage.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);

                        pendingRequestsAdapter = new PendingRequestsAdapter(requireContext(), pendingRequests, currentUsername);
                        listView.setAdapter(pendingRequestsAdapter);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("PendingRequestsFragment", "Error loading pending requests: " + e.getMessage());
                    emptyMessage.setText("Failed to load pending requests.");
                    emptyMessage.setVisibility(View.VISIBLE);
                });
    }
}