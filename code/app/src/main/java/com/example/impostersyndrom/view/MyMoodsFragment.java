package com.example.impostersyndrom.view;

import android.graphics.Color;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MyMoodsFragment extends Fragment {
    private static final String TAG = "MyMoodsFragment";
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
    private boolean isFilterActive = false;
    private List<DocumentSnapshot> currentFilteredDocs = new ArrayList<>();
    private boolean isFragmentActive = true;
    private AtomicInteger operationId = new AtomicInteger(0); // Track current operation

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_moods, container, false);

        emptyMessage = view.findViewById(R.id.emptyMessage);
        moodListView = view.findViewById(R.id.moodListView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        db = FirebaseFirestore.getInstance();
        userId = requireActivity().getIntent().getStringExtra("userId");

        emptyMessage.setText("Loading moods...");
        swipeRefreshLayout.setOnRefreshListener(this::fetchMyMoods);
        swipeRefreshLayout.setColorSchemeColors(Color.BLACK, Color.BLACK, Color.BLACK);

        fetchMyMoods();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        Log.d(TAG, "Fragment view destroyed");
    }

    public void fetchMyMoods() {
        if (!isFragmentActive || userId == null) {
            Log.d(TAG, "Fetch skipped: Fragment inactive or user ID null");
            if (userId == null) showMessage("User not logged in");
            updateEmptyState();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        Log.d(TAG, "Fetching moods for user: " + userId);
        emptyMessage.setText("Loading moods...");
        moodListView.setVisibility(View.GONE);
        if (moodAdapter != null) {
            moodAdapter.clear();
            moodListView.setAdapter(null);
            moodAdapter = null;
        }
        operationId.incrementAndGet(); // New fetch operation

        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!isFragmentActive) return;
                    moodDocs = snapshot.getDocuments();
                    Log.d(TAG, "Fetched " + moodDocs.size() + " moods");
                    applyCurrentFilter();
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    if (!isFragmentActive) return;
                    Log.e(TAG, "Fetch failed: " + e.getMessage());
                    showMessage("Failed to fetch moods: " + e.getMessage());
                    updateEmptyState();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void applyCurrentFilter() {
        if (!isFragmentActive) return;
        Log.d(TAG, "Applying filter - isFilterActive: " + isFilterActive + ", emotionalState: " + selectedEmotionalState + ", reason: " + selectedReason + ", recentWeek: " + filterByRecentWeek);
        if (isFilterActive) {
            applyFilter(selectedEmotionalState, selectedReason);
        } else {
            currentFilteredDocs = new ArrayList<>(moodDocs);
            Log.d(TAG, "No filter active, using " + currentFilteredDocs.size() + " docs");
            setupMoodAdapter(currentFilteredDocs);
        }
    }

    private void setupMoodAdapter(List<DocumentSnapshot> moodDocsToDisplay) {
        if (!isFragmentActive) return;
        int currentOpId = operationId.incrementAndGet(); // New operation ID
        Log.d(TAG, "Setting up adapter with " + moodDocsToDisplay.size() + " docs, opId: " + currentOpId);
        if (moodDocsToDisplay == null || moodDocsToDisplay.isEmpty()) {
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
                        moodItems.set(position, new MoodItem(moodDoc, "")); // No username for My Moods
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
                        Log.e(TAG, "User fetch failed: " + e.getMessage());
                        showMessage("User fetch failed: " + e.getMessage());
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

        moodAdapter = new MoodAdapter(requireContext(), moodItems, false);
        moodListView.setAdapter(moodAdapter);

        moodListView.setOnItemClickListener((parent, view, pos, id) -> {
            DocumentSnapshot selectedMood = moodDocsToDisplay.get(pos);
            ((MainActivity) requireActivity()).navigateToMoodDetail(selectedMood);
        });

        moodListView.setOnItemLongClickListener((parent, view, pos, id) -> {
            DocumentSnapshot selectedMood = moodDocsToDisplay.get(pos);
            ((MainActivity) requireActivity()).showBottomSheetDialog(selectedMood);
            return true;
        });
    }

    private void updateEmptyState() {
        if (!isFragmentActive) return;
        Log.d(TAG, "Updating empty state - isFilterActive: " + isFilterActive);
        moodListView.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);

        if (isFilterActive) {
            emptyMessage.setText("No moods match your filters");
        } else {
            emptyMessage.setText("No moods to display");
        }
    }

    public void applyFilter(String emotionalState, String reason) {
        if (!isFragmentActive) return;
        this.selectedEmotionalState = emotionalState;
        this.selectedReason = reason;
        this.isFilterActive = !emotionalState.isEmpty() || !reason.isEmpty() || filterByRecentWeek;

        Log.d(TAG, "Applying filter: emotionalState=" + emotionalState + ", reason=" + reason + ", recentWeek=" + filterByRecentWeek);
        MoodFilter moodFilter = new MoodFilter();
        currentFilteredDocs = moodFilter.applyFilter(moodDocs, filterByRecentWeek, emotionalState, reason);
        Log.d(TAG, "Filtered to " + currentFilteredDocs.size() + " docs");
        setupMoodAdapter(currentFilteredDocs);
    }

    public void applyFilter(String emotionalState) {
        applyFilter(emotionalState, "");
    }

    public void setFilterByRecentWeek(boolean filterByRecentWeek) {
        this.filterByRecentWeek = filterByRecentWeek;
        this.isFilterActive = filterByRecentWeek || !selectedEmotionalState.isEmpty() || !selectedReason.isEmpty();
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
}