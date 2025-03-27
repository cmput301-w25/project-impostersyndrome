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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FollowingMoodsFragment extends Fragment {
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
        View view = inflater.inflate(R.layout.fragment_following_moods, container, false);
        Log.d("FollowingMoodsFragment", "onCreateView called");
        emptyMessage = view.findViewById(R.id.emptyMessage);
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
            showMessage("User not logged in");
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
                        showMessage("You're not following anyone!");
                        moodListView.setAdapter(null);
                        moodListView.setVisibility(View.GONE);
                        emptyMessage.setText("You're not following anyone.");
                        emptyMessage.setVisibility(View.VISIBLE);
                        return;
                    }
                    emptyMessage.setVisibility(View.GONE);
                    fetchLatestMoodsFromFollowedUsers(followingIds);
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowingMoodsFragment", "Failed to fetch following list: " + e.getMessage());
                    showMessage("Failed to fetch following list: " + e.getMessage());
                    moodListView.setAdapter(null); // Clear list on failure
                });
    }

    private void fetchLatestMoodsFromFollowedUsers(List<String> followingIds) {
        List<DocumentSnapshot> allMoods = new ArrayList<>();
        final int[] completedQueries = {0};

        if (followingIds == null || followingIds.isEmpty()) {
            Log.d("FollowingMoodsFragment", "No followed users.");
            setupMoodAdapter(Collections.emptyList());
            return;
        }

        for (String followedUserId : followingIds) {
            db.collection("moods")
                    .whereEqualTo("userId", followedUserId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener(snapshot -> {
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
                            finalizeFollowingMoods(allMoods);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowingMoodsFragment", "Failed to fetch moods for user " + followedUserId + ": " + e.getMessage());
                        completedQueries[0]++;

                        if (completedQueries[0] == followingIds.size()) {
                            finalizeFollowingMoods(allMoods);
                        }
                    });
        }
    }

    private void finalizeFollowingMoods(List<DocumentSnapshot> allMoods) {
        allMoods.sort((m1, m2) -> {
            Timestamp t1 = m1.getTimestamp("timestamp");
            Timestamp t2 = m2.getTimestamp("timestamp");
            if (t1 == null || t2 == null) return 0;
            return Long.compare(t2.toDate().getTime(), t1.toDate().getTime());
        });

        setupMoodAdapter(allMoods);
    }

    private void setupMoodAdapter(List<DocumentSnapshot> moodDocs) {
        this.moodDocs = moodDocs;
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
                                moodListView.setVisibility(View.GONE);
                                emptyMessage.setVisibility(View.VISIBLE);
                                Log.d("FollowingMoodsFragment", "All items null, showing empty message");
                                Log.d("FollowingMoodsFragment", "All items null, clearing adapter");
                                showMessage("No moods to display");
                            } else {
                                emptyMessage.setVisibility(View.GONE);
                                moodListView.setVisibility(View.VISIBLE);
                                moodAdapter = new MoodAdapter(requireContext(), moodItems, true);
                                moodListView.setAdapter(moodAdapter);
                                Log.d("FollowingMoodsFragment", "Adapter set with " + moodItems.size() + " items");
                                moodListView.invalidate();

                                moodListView.setOnItemClickListener((parent, view, pos, id) -> {
                                    DocumentSnapshot selectedMood = moodDocs.get(pos);
                                    ((MainActivity) requireActivity()).navigateToMoodDetail(selectedMood);
                                });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("FollowingMoodsFragment", "Error fetching user details: " + e.getMessage());
                        showMessage("Error fetching user details: " + e.getMessage());
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

    /**
     * Displays a Snackbar message.
     *
     * @param message The message to display.
     */
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
        fetchFollowingMoods(); // refresh the list when user comes back
    }


}