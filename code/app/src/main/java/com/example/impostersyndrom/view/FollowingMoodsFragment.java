package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.model.MoodFilter;
import com.example.impostersyndrom.model.MoodItem;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class FollowingMoodsFragment extends Fragment {
    private static final String TAG = "FollowingMoodsFragment";
    private TextView emptyMessage;
    private ListView moodListView;
    private MoodAdapter moodAdapter;
    private FirebaseFirestore db;
    private String userId;
    private List<DocumentSnapshot> moodDocs = new ArrayList<>();
    private boolean filterByRecentWeek = false;
    private String selectedEmotionalState = "";
    private String selectedReason = "";
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isFragmentActive = true;
    private AtomicInteger operationId = new AtomicInteger(0); // Track current operation

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following_moods, container, false);
        Log.d(TAG, "onCreateView called");

        emptyMessage = view.findViewById(R.id.emptyMessage);
        moodListView = view.findViewById(R.id.moodListView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        db = FirebaseFirestore.getInstance();
        userId = requireActivity().getIntent().getStringExtra("userId");
        Log.d(TAG, "userId: " + userId);

        // Configure swipe to refresh
        swipeRefreshLayout.setOnRefreshListener(this::fetchFollowingMoods);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        fetchFollowingMoods(); // Initial fetch
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        Log.d(TAG, "Fragment view destroyed");
    }

    public void fetchFollowingMoods() {
        if (!isFragmentActive || userId == null) {
            Log.d(TAG, "Fetch skipped: Fragment inactive or user ID null");
            if (userId == null) showMessage("User not logged in");
            updateEmptyState();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        Log.d(TAG, "Fetching following moods for userId: " + userId);
        emptyMessage.setText("Loading moods...");
        moodListView.setVisibility(View.GONE);
        if (moodAdapter != null) {
            moodAdapter.clear();
            moodListView.setAdapter(null);
            moodAdapter = null;
        }
        operationId.incrementAndGet(); // New fetch operation

        db.collection("following")
                .whereEqualTo("followerId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isFragmentActive) return;
                    List<String> followingIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String followingId = doc.getString("followingId");
                        if (followingId != null) {
                            followingIds.add(followingId);
                        }
                    }

                    if (followingIds.isEmpty()) {
                        updateEmptyState("You're not following anyone.");
                        swipeRefreshLayout.setRefreshing(false);
                        return;
                    }
                    fetchLatestMoodsFromFollowedUsers(followingIds);
                })
                .addOnFailureListener(e -> {
                    if (!isFragmentActive) return;
                    Log.e(TAG, "Failed to fetch following list: " + e.getMessage());
                    showMessage("Failed to fetch following list: " + e.getMessage());
                    updateEmptyState();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void fetchLatestMoodsFromFollowedUsers(List<String> followingIds) {
        if (!isFragmentActive) return;
        List<DocumentSnapshot> allMoods = new ArrayList<>();
        final int[] completedQueries = {0};
        int currentOpId = operationId.get(); // Capture current operation ID

        if (followingIds == null || followingIds.isEmpty()) {
            Log.d(TAG, "No followed users.");
            setupMoodAdapter(Collections.emptyList());
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        for (String followedUserId : followingIds) {
            db.collection("moods")
                    .whereEqualTo("userId", followedUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!isFragmentActive || currentOpId != operationId.get()) {
                            Log.d(TAG, "Ignoring stale fetch result, opId: " + currentOpId + ", current: " + operationId.get());
                            return;
                        }
                        List<DocumentSnapshot> filteredMoods = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Boolean isPrivate = doc.contains("privateMood") ? doc.getBoolean("privateMood") : false;
                            if (!Boolean.TRUE.equals(isPrivate)) {
                                filteredMoods.add(doc);
                            }
                            if (filteredMoods.size() == 3) break; // Max 3 per user
                        }
                        allMoods.addAll(filteredMoods);
                        completedQueries[0]++;

                        if (completedQueries[0] == followingIds.size()) {
                            if (currentOpId != operationId.get()) {
                                Log.d(TAG, "Discarding stale fetch results, opId: " + currentOpId + ", current: " + operationId.get());
                                return;
                            }
                            finalizeFollowingMoods(allMoods);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isFragmentActive || currentOpId != operationId.get()) return;
                        Log.e(TAG, "Failed to fetch moods for user " + followedUserId + ": " + e.getMessage());
                        completedQueries[0]++;
                        if (completedQueries[0] == followingIds.size()) {
                            finalizeFollowingMoods(allMoods);
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
        }
    }

    private void finalizeFollowingMoods(List<DocumentSnapshot> allMoods) {
        if (!isFragmentActive) return;
        allMoods.sort((m1, m2) -> {
            Timestamp t1 = m1.getTimestamp("timestamp");
            Timestamp t2 = m2.getTimestamp("timestamp");
            if (t1 == null || t2 == null) return 0;
            return Long.compare(t2.toDate().getTime(), t1.toDate().getTime());
        });
        moodDocs = allMoods; // Update moodDocs for filtering
        applyCurrentFilter();
    }

    private void applyCurrentFilter() {
        if (!isFragmentActive) return;
        Log.d(TAG, "Applying filter - filterByRecentWeek: " + filterByRecentWeek + ", emotionalState: " + selectedEmotionalState + ", reason: " + selectedReason);
        MoodFilter moodFilter = new MoodFilter();
        List<DocumentSnapshot> filteredMoods = moodFilter.applyFilter(moodDocs, filterByRecentWeek, selectedEmotionalState, selectedReason);
        setupMoodAdapter(filteredMoods);
    }

    private void setupMoodAdapter(List<DocumentSnapshot> moodDocsToDisplay) {
        if (!isFragmentActive) return;
        int currentOpId = operationId.incrementAndGet(); // New operation ID
        Log.d(TAG, "Setting up adapter with " + moodDocsToDisplay.size() + " docs, opId: " + currentOpId);

        if (moodDocsToDisplay.isEmpty()) {
            Log.d(TAG, "No moods to display after filtering, opId: " + currentOpId);
            updateEmptyState();
            return;
        }

        List<MoodItem> moodItems = new ArrayList<>(Collections.nCopies(moodDocsToDisplay.size(), null));
        final int[] completedQueries = {0};

        for (int i = 0; i < moodDocsToDisplay.size(); i++) {
            final int position = i;
            DocumentSnapshot moodDoc = moodDocsToDisplay.get(i);
            String moodUserId = moodDoc.getString("userId");
            if (moodUserId == null) {
                completedQueries[0]++;
                continue;
            }

            db.collection("users").document(moodUserId)
                    .get()
                    .addOnSuccessListener(userDoc -> {
                        if (!isFragmentActive || currentOpId != operationId.get()) {
                            Log.d(TAG, "Ignoring stale query result, opId: " + currentOpId + ", current: " + operationId.get());
                            return;
                        }
                        String username = userDoc.getString("username");
                        moodItems.set(position, new MoodItem(moodDoc, "@" + username));
                        completedQueries[0]++;

                        if (completedQueries[0] == moodDocsToDisplay.size()) {
                            moodItems.removeIf(item -> item == null);
                            Log.d(TAG, "Processed all queries, " + moodItems.size() + " items remain, opId: " + currentOpId);
                            if (currentOpId != operationId.get()) {
                                Log.d(TAG, "Discarding stale results, opId: " + currentOpId + ", current: " + operationId.get());
                                return;
                            }
                            if (moodItems.isEmpty()) {
                                updateEmptyState();
                            } else {
                                showMoodList(moodItems, moodDocsToDisplay);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!isFragmentActive || currentOpId != operationId.get()) return;
                        Log.e(TAG, "Error fetching user details: " + e.getMessage());
                        showMessage("Error fetching user details: " + e.getMessage());
                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocsToDisplay.size()) {
                            if (currentOpId != operationId.get()) return;
                            updateEmptyState();
                        }
                    });
        }
    }

    private void showMoodList(List<MoodItem> moodItems, List<DocumentSnapshot> moodDocsToDisplay) {
        if (!isFragmentActive) return;
        Log.d(TAG, "Showing mood list with " + moodItems.size() + " items");
        emptyMessage.setVisibility(View.GONE);
        moodListView.setVisibility(View.VISIBLE);

        moodAdapter = new MoodAdapter(requireContext(), moodItems, true);
        moodListView.setAdapter(moodAdapter);
        moodListView.invalidate();

        moodListView.setOnItemClickListener((parent, view, pos, id) -> {
            DocumentSnapshot selectedMood = moodDocsToDisplay.get(pos);
            ((MainActivity) requireActivity()).navigateToMoodDetail(selectedMood);
        });
    }

    private void updateEmptyState() {
        updateEmptyState(null);
    }

    private void updateEmptyState(String customMessage) {
        if (!isFragmentActive) return;
        Log.d(TAG, "Updating empty state - filterByRecentWeek: " + filterByRecentWeek + ", emotionalState: " + selectedEmotionalState + ", reason: " + selectedReason);
        moodListView.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);

        if (customMessage != null) {
            emptyMessage.setText(customMessage);
        } else if (!selectedEmotionalState.isEmpty() || !selectedReason.isEmpty() || filterByRecentWeek) {
            emptyMessage.setText("No moods match your filters.");
        } else {
            emptyMessage.setText("No moods to display.");
        }
    }

    public void applyFilter(String emotionalState) {
        if (!isFragmentActive) return;
        this.selectedEmotionalState = emotionalState;
        applyCurrentFilter();
    }

    public void applyFilter(String emotionalState, String reason) {
        if (!isFragmentActive) return;
        this.selectedEmotionalState = emotionalState;
        this.selectedReason = reason;
        applyCurrentFilter();
    }

    public void setFilterByRecentWeek(boolean filterByRecentWeek) {
        if (!isFragmentActive) return;
        this.filterByRecentWeek = filterByRecentWeek;
        applyCurrentFilter();
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

    private void showMessage(String message) {
        View view = getView();
        if (view != null && !isDetached()) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchFollowingMoods();
    }
}