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
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isFilterActive = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_moods, container, false);

        emptyMessage = view.findViewById(R.id.emptyMessage);
        db = FirebaseFirestore.getInstance();
        userId = requireActivity().getIntent().getStringExtra("userId");

        moodListView = view.findViewById(R.id.moodListView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        // Initial state - show loading
        moodListView.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText("Loading moods...");

        swipeRefreshLayout.setOnRefreshListener(this::fetchMyMoods);
        swipeRefreshLayout.setColorSchemeColors(Color.BLACK, Color.BLACK, Color.BLACK);

        fetchMyMoods();
        return view;
    }

    public void fetchMyMoods() {
        if (userId == null) {
            showMessage("User not logged in");
            updateEmptyState();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    moodDocs = snapshot.getDocuments();
                    applyCurrentFilter();
                    swipeRefreshLayout.setRefreshing(false);
                })
                .addOnFailureListener(e -> {
                    showMessage("Failed to fetch moods: " + e.getMessage());
                    updateEmptyState();
                    swipeRefreshLayout.setRefreshing(false);
                });
    }

    private void applyCurrentFilter() {
        if (isFilterActive) {
            applyFilter(selectedEmotionalState, selectedReason);
        } else {
            setupMoodAdapter(moodDocs);
        }
    }

    private void setupMoodAdapter(List<DocumentSnapshot> moodDocs) {
        if (moodDocs == null || moodDocs.isEmpty()) {
            updateEmptyState();
            return;
        }

        List<MoodItem> moodItems = new ArrayList<>(Collections.nCopies(moodDocs.size(), null));
        final int[] completedQueries = {0};

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
                        moodItems.set(position, new MoodItem(moodDoc, ""));
                        completedQueries[0]++;

                        if (completedQueries[0] == moodDocs.size()) {
                            moodItems.removeIf(item -> item == null);
                            if (moodItems.isEmpty()) {
                                updateEmptyState();
                            } else {
                                showMoodList(moodItems, moodDocs);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedQueries[0]++;
                        if (completedQueries[0] == moodDocs.size()) {
                            updateEmptyState();
                        }
                    });
        }
    }

    private void showMoodList(List<MoodItem> moodItems, List<DocumentSnapshot> moodDocs) {
        emptyMessage.setVisibility(View.GONE);
        moodListView.setVisibility(View.VISIBLE);

        moodAdapter = new MoodAdapter(requireContext(), moodItems, false);
        moodListView.setAdapter(moodAdapter);

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

    private void updateEmptyState() {
        moodListView.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);

        if (isFilterActive) {
            emptyMessage.setText("No moods match your filters");
        } else {
            emptyMessage.setText("No moods to display");
        }
    }

    public void applyFilter(String emotionalState, String reason) {
        this.selectedEmotionalState = emotionalState;
        this.selectedReason = reason;
        this.isFilterActive = !emotionalState.isEmpty() || !reason.isEmpty() || filterByRecentWeek;

        MoodFilter moodFilter = new MoodFilter();
        List<DocumentSnapshot> filteredMoods = moodFilter.applyFilter(moodDocs, filterByRecentWeek, emotionalState, reason);
        setupMoodAdapter(filteredMoods);
    }

    public void applyFilter(String emotionalState) {
        applyFilter(emotionalState, "");
    }

    public void setFilterByRecentWeek(boolean filterByRecentWeek) {
        this.filterByRecentWeek = filterByRecentWeek;
        this.isFilterActive = filterByRecentWeek;
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