package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.widget.ListView;
import android.widget.TextView;

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
    private PendingRequestsAdapter pendingAdapter;
    private FirebaseFirestore db;
    private String currentUserId;

    public PendingRequestsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        listView = view.findViewById(R.id.listView);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadPendingRequests();
        return view;
    }

    private void loadPendingRequests() {
        db.collection("follow_requests")
                .whereEqualTo("receiverId", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pendingRequests.clear();  // Clear old data to avoid duplicates

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        pendingRequests.add(doc.getString("senderUsername"));
                    }

                    if (pendingRequests.isEmpty()) {
                        emptyMessage.setText("No more requests");
                        emptyMessage.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                    } else {
                        emptyMessage.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }

                    pendingAdapter = new PendingRequestsAdapter(requireContext(), pendingRequests, currentUserId);
                    listView.setAdapter(pendingAdapter);
                })
                .addOnFailureListener(e -> {
                    emptyMessage.setText("Failed to load requests.");
                    emptyMessage.setVisibility(View.VISIBLE);
                });
    }

}
