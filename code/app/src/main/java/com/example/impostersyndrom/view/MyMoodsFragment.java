package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.model.MoodFilter;
import com.example.impostersyndrom.model.MoodItem;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MyMoodsFragment extends Fragment {
    private TextView emptyMessage;

    private ListView moodListView;
    private MoodAdapter moodAdapter;
    private FirebaseFirestore db;
    private String userId;
    private List<DocumentSnapshot> moodDocs = new ArrayList<>();
    private boolean filterByRecentWeek = false;
    private String selectedEmotionalState = "";
    private String selectedReason = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_moods, container, false);
        Log.d("MyMoodsFragment", "onCreateView called");
        emptyMessage = view.findViewById(R.id.emptyMessage);
        db = FirebaseFirestore.getInstance();
        userId = requireActivity().getIntent().getStringExtra("userId");
        Log.d("MyMoodsFragment", "userId: " + userId);

        moodListView = view.findViewById(R.id.moodListView);
        fetchMyMoods(); // Initial fetch

        return view;
    }

    public void fetchMyMoods() {
        if (userId == null) {
            Log.e("MyMoodsFragment", "userId is null, cannot fetch moods");
            showToast("User not logged in");
            return;
        }

        Log.d("MyMoodsFragment", "Fetching moods for userId: " + userId);
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    moodDocs = snapshot.getDocuments();
                    Log.d("MyMoodsFragment", "Fetched " + moodDocs.size() + " moods");
                    applyFilter(selectedEmotionalState); // Apply current filter
                })
                .addOnFailureListener(e -> {
                    Log.e("MyMoodsFragment", "Failed to fetch moods: " + e.getMessage());
                    showToast("Failed to fetch your moods: " + e.getMessage());
                    moodListView.setAdapter(null); // Clear list on failure
                });
    }

    private void setupMoodAdapter(List<DocumentSnapshot> moodDocs) {
        this.moodDocs = moodDocs; // keep moodDocs updated
        List<MoodItem> moodItems = new ArrayList<>(Collections.nCopies(moodDocs.size(), null));
        final int[] completedQueries = {0};

        if (moodDocs.isEmpty()) {
            moodListView.setAdapter(null);
            moodListView.setVisibility(View.GONE);

            if (!selectedEmotionalState.isEmpty() || !selectedReason.isEmpty() || filterByRecentWeek) {
                emptyMessage.setText("No moods match your filters.");
            } else {
                emptyMessage.setText("No moods to display.");
            }

            emptyMessage.setVisibility(View.VISIBLE);
            Log.d("FollowingMoodsFragment", "No moods to display, showing empty message");
            return;
        }


        emptyMessage.setVisibility(View.GONE);
        moodListView.setVisibility(View.VISIBLE);
        Log.d("MyMoodsFragment", "Setting up adapter with " + moodDocs.size() + " items");

        for (int i = 0; i < moodDocs.size(); i++) {
            final int position = i;
            DocumentSnapshot moodDoc = moodDocs.get(i);
            String moodUserId = moodDoc.getString("userId");
            if (moodUserId == null) {
                completedQueries[0]++;
                continue;
            }

            db.collection("users").document(moodUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        moodItems.set(position, new MoodItem(moodDoc, "")); // No username for My Moods
                        completedQueries[0]++;

                        if (completedQueries[0] == moodDocs.size()) {
                            moodItems.removeIf(item -> item == null);
                            if (moodItems.isEmpty()) {
                                moodListView.setAdapter(null);
                                moodListView.setVisibility(View.GONE);
                                emptyMessage.setVisibility(View.VISIBLE);
                                Log.d("MyMoodsFragment", "All items null, showing empty message");
                            } else {
                                emptyMessage.setVisibility(View.GONE);
                                moodListView.setVisibility(View.VISIBLE);
                                moodAdapter = new MoodAdapter(requireContext(), moodItems, false);
                                moodListView.setAdapter(moodAdapter);
                                Log.d("MyMoodsFragment", "Adapter set with " + moodItems.size() + " items");
                                moodListView.invalidate();

                                moodListView.setOnItemClickListener((parent, view, pos, id) -> {
                                    DocumentSnapshot selectedMood = moodDocs.get(pos);
                                    ((MainActivity) requireActivity()).navigateToMoodDetail(selectedMood);
                                });

                                moodListView.setOnItemLongClickListener((parent, view, pos, id) -> {
                                    DocumentSnapshot selectedMood = moodDocs.get(pos);
                                    ((MainActivity) requireActivity()).showBottomSheetDialog(selectedMood);
                                    return true;
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MyMoodsFragment", "Error fetching user details: " + e.getMessage());
                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocs.size()) {
                            moodItems.removeIf(item -> item == null);
                            moodListView.setAdapter(null);
                            moodListView.setVisibility(View.GONE);
                            emptyMessage.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }

    public void applyFilter(String emotionalState) {
        this.selectedEmotionalState = emotionalState;
        MoodFilter moodFilter = new MoodFilter();
        List<DocumentSnapshot> filteredMoods = moodFilter.applyFilter(moodDocs, filterByRecentWeek, emotionalState);
        setupMoodAdapter(filteredMoods);
    }

    public void applyFilter(String emotionalState, String reason) {
        this.selectedEmotionalState = emotionalState;
        this.selectedReason = reason;
        MoodFilter moodFilter = new MoodFilter();
        List<DocumentSnapshot> filteredMoods = moodFilter.applyFilter(moodDocs, filterByRecentWeek, emotionalState, reason);
        setupMoodAdapter(filteredMoods);
    }

    public void setFilterByRecentWeek(boolean filterByRecentWeek) {
        this.filterByRecentWeek = filterByRecentWeek;
        applyFilter(selectedEmotionalState);
    }

    public boolean isFilterByRecentWeek() {
        return filterByRecentWeek;
    }

    public String getSelectedEmotionalState() {
        return selectedEmotionalState;
    }

    public String getSelectedReason() {
        return selectedReason;
    }

    private void showToast(String message) {
        if (!isDetached()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchMyMoods(); // refresh the list when user comes back
    }

}