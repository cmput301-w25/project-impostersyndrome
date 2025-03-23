package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.model.MoodFilter;
import com.example.impostersyndrom.model.MoodItem;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FollowingMoodsFragment extends Fragment {

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
        View view = inflater.inflate(R.layout.fragment_following_moods, container, false);
        Log.d("FollowingMoodsFragment", "onCreateView called");

        db = FirebaseFirestore.getInstance();
        userId = requireActivity().getIntent().getStringExtra("userId");
        Log.d("FollowingMoodsFragment", "userId: " + userId);

        moodListView = view.findViewById(R.id.moodListView);
        fetchFollowingMoods(); // Initial fetch

        return view;
    }

    public void fetchFollowingMoods() {
        if (userId == null) {
            Log.e("FollowingMoodsFragment", "userId is null, cannot fetch moods");
            showToast("User not logged in");
            return;
        }

        Log.d("FollowingMoodsFragment", "Fetching following moods for userId: " + userId);
        db.collection("following")
                .whereEqualTo("followerId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> followingIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String followingId = doc.getString("followingId");
                        if (followingId != null) {
                            followingIds.add(followingId);
                        }
                    }

                    if (followingIds.isEmpty()) {
                        Log.d("FollowingMoodsFragment", "No users followed");
                        showToast("You're not following anyone!");
                        moodListView.setAdapter(null);
                        return;
                    }

                    fetchLatestMoodsFromFollowedUsers(followingIds);
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingMoodsFragment", "Failed to fetch following list: " + e.getMessage());
                    showToast("Failed to fetch following list: " + e.getMessage());
                    moodListView.setAdapter(null); // Clear list on failure
                });
    }

    private void fetchLatestMoodsFromFollowedUsers(List<String> followingIds) {
        List<DocumentSnapshot> allMoods = new ArrayList<>();
        final int[] completedQueries = {0};

        for (String followedUserId : followingIds) {
            db.collection("moods")
                    .whereEqualTo("userId", followedUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        allMoods.addAll(snapshot.getDocuments());
                        completedQueries[0]++;
                        if (completedQueries[0] == followingIds.size()) {
                            allMoods.sort((m1, m2) -> {
                                Timestamp t1 = m1.getTimestamp("timestamp");
                                Timestamp t2 = m2.getTimestamp("timestamp");
                                if (t1 == null || t2 == null) return 0;
                                return Long.compare(t2.toDate().getTime(), t1.toDate().getTime());
                            });
                            moodDocs = allMoods;
                            Log.d("FollowingMoodsFragment", "Fetched " + moodDocs.size() + " moods");
                            applyFilter(selectedEmotionalState); // Apply current filter
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedQueries[0]++;
                        if (completedQueries[0] == followingIds.size()) {
                            if (!allMoods.isEmpty()) {
                                allMoods.sort((m1, m2) -> {
                                    Timestamp t1 = m1.getTimestamp("timestamp");
                                    Timestamp t2 = m2.getTimestamp("timestamp");
                                    if (t1 == null || t2 == null) return 0;
                                    return Long.compare(t2.toDate().getTime(), t1.toDate().getTime());
                                });
                                moodDocs = allMoods;
                                Log.d("FollowingMoodsFragment", "Fetched " + moodDocs.size() + " moods after partial failure");
                                applyFilter(selectedEmotionalState); // Apply current filter
                            } else {
                                Log.e("FollowingMoodsFragment", "Failed to fetch moods: " + e.getMessage());
                                moodListView.setAdapter(null);
                                showToast("No moods fetched");
                            }
                        }
                    });
        }
    }

    private void setupMoodAdapter(List<DocumentSnapshot> moodDocs) {
        List<MoodItem> moodItems = new ArrayList<>(Collections.nCopies(moodDocs.size(), null));
        final int[] completedQueries = {0};

        if (moodDocs.isEmpty()) {
            moodListView.setAdapter(null);
            Log.d("FollowingMoodsFragment", "No moods to display, clearing adapter");
            showToast("No moods to display");
            return;
        }

        Log.d("FollowingMoodsFragment", "Setting up adapter with " + moodDocs.size() + " items");
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
                        String username = userDoc.getString("username");
                        moodItems.set(position, new MoodItem(moodDoc, "@" + username));

                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocs.size()) {
                            moodItems.removeIf(item -> item == null);
                            if (moodItems.isEmpty()) {
                                moodListView.setAdapter(null);
                                Log.d("FollowingMoodsFragment", "All items null, clearing adapter");
                                showToast("No moods to display");
                            } else {
                                moodAdapter = new MoodAdapter(requireContext(), moodItems, true);
                                moodListView.setAdapter(moodAdapter);
                                Log.d("FollowingMoodsFragment", "Adapter set with " + moodItems.size() + " items");
                                moodListView.invalidate(); // Force redraw

                                moodListView.setOnItemClickListener((parent, view, pos, id) -> {
                                    DocumentSnapshot selectedMood = moodDocs.get(pos);
                                    ((MainActivity) requireActivity()).navigateToMoodDetail(selectedMood);
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowingMoodsFragment", "Error fetching user details: " + e.getMessage());
                        showToast("Error fetching user details: " + e.getMessage());
                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocs.size()) {
                            moodItems.removeIf(item -> item == null);
                            moodListView.setAdapter(null); // Clear list on failure
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
}