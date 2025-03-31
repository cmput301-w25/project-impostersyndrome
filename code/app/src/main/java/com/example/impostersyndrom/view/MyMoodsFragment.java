package com.example.impostersyndrom.view;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.MoodAdapter;
import com.example.impostersyndrom.controller.NetworkUtils;
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
    private static final int PAGE_SIZE = 10; // Number of moods to load per page
    
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
    private AtomicInteger operationId = new AtomicInteger(0);
    private DocumentSnapshot lastVisibleDocument;
    private boolean isLoadingMore = false;
    private boolean hasMoreData = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_moods, container, false);

        emptyMessage = view.findViewById(R.id.emptyMessage);
        moodListView = view.findViewById(R.id.moodListView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        db = FirebaseFirestore.getInstance();
        userId = requireActivity().getIntent().getStringExtra("userId");

        emptyMessage.setText("Loading moods...");
        swipeRefreshLayout.setOnRefreshListener(this::refreshMoods);
        swipeRefreshLayout.setColorSchemeColors(Color.BLACK, Color.BLACK, Color.BLACK);

        // Setup scroll listener for pagination
        moodListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {}

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (!isLoadingMore && hasMoreData && !isFilterActive) {
                    if (firstVisibleItem + visibleItemCount >= totalItemCount - 5) {
                        loadMoreMoods();
                    }
                }
            }
        });

        fetchMyMoods();
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        Log.d(TAG, "Fragment view destroyed");
    }

    public void refreshMoods() {
        Log.d("MyMoodsFragment", "refreshMoods called");
        if (NetworkUtils.isOffline(requireContext())) {
            Log.d("MyMoodsFragment", "Offline mode - showing offline message");
            showMessage("You're offline. Your changes will sync when you're back online.");
            return;
        }
        fetchMyMoods();
    }

    public void fetchMyMoods() {
        if (isFilterByRecentWeek()) {
            fetchRecentWeekMoods();
            return;
        }

        if (NetworkUtils.isOffline(requireContext())) {
            Log.d("MyMoodsFragment", "Offline mode - showing offline message");
            showMessage("You're offline. Your changes will sync when you're back online.");
            return;
        }

        Log.d("MyMoodsFragment", "Starting to fetch moods");
        showLoading(true);
        moodDocs.clear();
        lastVisibleDocument = null;
        hasMoreData = true;
        isLoadingMore = false;

        Query query = db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("MyMoodsFragment", "Successfully fetched moods: " + queryDocumentSnapshots.size());
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastVisibleDocument = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                        hasMoreData = queryDocumentSnapshots.size() >= PAGE_SIZE;
                    } else {
                        hasMoreData = false;
                    }
                    moodDocs.addAll(queryDocumentSnapshots.getDocuments());
                    applyCurrentFilter();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Log.e("MyMoodsFragment", "Error fetching moods: " + e.getMessage());
                    showMessage("Error loading moods: " + e.getMessage());
                    showLoading(false);
                });
    }

    private void loadMoreMoods() {
        if (isLoadingMore || !hasMoreData) return;
        
        isLoadingMore = true;
        fetchMyMoods();
        isLoadingMore = false;
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

    private void showLoading(boolean show) {
        // Implementation of showLoading method
    }

    private void fetchRecentWeekMoods() {
        // Implementation of fetchRecentWeekMoods method
    }
}