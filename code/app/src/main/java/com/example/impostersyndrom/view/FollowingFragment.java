package com.example.impostersyndrom.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.ListView;
import android.widget.TextView;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.FollowingAdapter;
import com.example.impostersyndrom.model.UserData; // Import UserData
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
/**
 * A Fragment that displays the list of users the current user is following.
 * Handles real-time updates to the following list and user details from Firestore.
 * Shows an empty state message when not following anyone.
 *
 * @author [Your Name]
 */
public class FollowingFragment extends Fragment {
    private ListView listView;
    private TextView emptyMessage;
    private List<UserData> followingUsers = new ArrayList<>();
    private FollowingAdapter followingAdapter;
    private FirebaseFirestore db;
    private String currentUserId;
    private ListenerRegistration followingListener;

    public FollowingFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);
        listView = view.findViewById(R.id.listView);
        emptyMessage = view.findViewById(R.id.emptyMessage);
        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getUid();

        // Initialize adapter with List<UserData>
        followingAdapter = new FollowingAdapter(requireContext(), followingUsers, currentUserId);
        followingAdapter.setEmptyMessageView(emptyMessage);
        listView.setAdapter(followingAdapter);

        loadFollowingUsers();
        return view;
    }

    private void loadFollowingUsers() {
        if (followingListener != null) {
            followingListener.remove();
        }

        followingListener = db.collection("following")
                .whereEqualTo("followerId", currentUserId)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        emptyMessage.setText("Failed to load following list.");
                        emptyMessage.setVisibility(View.VISIBLE);
                        Log.e("FollowingFragment", "Error: " + error.getMessage());
                        return;
                    }

                    followingUsers.clear();
                    List<String> userIds = new ArrayList<>();

                    // Get the list of user IDs we are following
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String followingId = doc.getString("followingId");
                        if (followingId != null) {
                            userIds.add(followingId);
                        }
                    }

                    // Step 2: Fetch usernames from the "users" collection
                    if (userIds.isEmpty()) {
                        emptyMessage.setText("You're not following anyone yet");
                        emptyMessage.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                        followingAdapter.notifyDataSetChanged();
                    } else {
                        emptyMessage.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                        fetchUserDetails(userIds);
                    }
                });
    }

    private void fetchUserDetails(List<String> userIds) {
        followingUsers.clear();
        for (String userId : userIds) {
            db.collection("users").document(userId)
                    .addSnapshotListener((userDoc, error) -> {
                        if (error != null) {
                            Log.e("FollowingFragment", "Error fetching user data: " + error.getMessage());
                            return;
                        }

                        if (userDoc != null && userDoc.exists()) {
                            String username = userDoc.getString("username");
                            String profileImageUrl = userDoc.getString("profileImageUrl");
                            if (username != null) {
                                boolean userExists = false;
                                for (UserData existingUser : followingUsers) {
                                    if (existingUser.username.equals(username)) {
                                        existingUser.profileImageUrl = profileImageUrl; // Update existing pfp
                                        userExists = true;
                                        break;
                                    }
                                }
                                if (!userExists) {
                                    followingUsers.add(new UserData(username, profileImageUrl));
                                }
                                followingAdapter.notifyDataSetChanged();
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (followingListener != null) {
            followingListener.remove();
        }
    }

    /**
     * Displays a Snackbar message.
     *
     * @param message The message to display.
     */
    private void showMessage(String message) {
        if (getView() != null) {
            Snackbar.make(getView(), message, Snackbar.LENGTH_LONG)
                    .setAction("OK", null)
                    .show();
        }
    }
}