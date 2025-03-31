package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.UserListAdapter;
import com.example.impostersyndrom.model.UserData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private EditText searchInput;
    private ListView userListView;
    private ImageButton addMoodButton, homeButton, profileButton, heartButton, clearSearch;
    private FirebaseFirestore db;
    private List<UserData> userList = new ArrayList<>();
    private UserListAdapter adapter;
    private FirebaseAuth auth;
    private ListenerRegistration searchListener;
    private static final String TAG = "SearchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize UI elements
        searchInput = findViewById(R.id.searchInput);
        userListView = findViewById(R.id.userListView);
        addMoodButton = findViewById(R.id.addMoodButton);
        homeButton = findViewById(R.id.homeButton);
        profileButton = findViewById(R.id.profileButton);
        clearSearch = findViewById(R.id.clearSearch);
        heartButton = findViewById(R.id.heartButton);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Set up adapter for ListView
        adapter = new UserListAdapter(this, userList);
        userListView.setAdapter(adapter);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    clearSearch.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Clear button VISIBLE");
                } else {
                    clearSearch.setVisibility(View.GONE);
                    Log.d(TAG, "Clear button GONE");
                }
                searchUsers(s.toString().trim());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        clearSearch.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearch.setVisibility(View.GONE);
            Log.d(TAG, "Clear button CLICKED & HIDDEN");
        });

        homeButton.setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, MainActivity.class));
            finish();
        });

        addMoodButton.setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, EmojiSelectionActivity.class));
        });

        heartButton.setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, FollowingActivity.class));
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, ProfileActivity.class));
        });
    }

    private void searchUsers(String query) {
        TextView noResultsText = findViewById(R.id.noResultsText);
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (query.isEmpty()) {
            userList.clear();
            adapter.notifyDataSetChanged();
            noResultsText.setVisibility(View.GONE);
            if (searchListener != null) {
                searchListener.remove();
                searchListener = null;
            }
            return;
        }

        if (currentUserId == null) {
            Log.e(TAG, "Current user ID is null, cannot search");
            Toast.makeText(this, "Please log in to search", Toast.LENGTH_SHORT).show();
            return;
        }

        if (searchListener != null) {
            searchListener.remove();
        }

        searchListener = db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error searching users: " + error.getMessage());
                        Toast.makeText(this, "Error searching users", Toast.LENGTH_SHORT).show();
                        noResultsText.setVisibility(View.VISIBLE);
                        return;
                    }

                    if (querySnapshot == null) {
                        Log.e(TAG, "Query snapshot is null");
                        noResultsText.setVisibility(View.VISIBLE);
                        return;
                    }

                    userList.clear();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String username = document.getString("username");
                        String profileImageUrl = document.getString("profileImageUrl");
                        String userId = document.getId();

                        Log.d(TAG, "Fetched user: " + username + ", Pfp: " + profileImageUrl);

                        if (username != null && !userId.equals(currentUserId)) {
                            userList.add(new UserData(username, profileImageUrl));
                        }
                    }
                    try {
                        adapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        Log.e(TAG, "Error notifying adapter: " + e.getMessage());
                    }

                    if (userList.isEmpty() && !query.isEmpty()) {
                        noResultsText.setVisibility(View.VISIBLE);
                    } else {
                        noResultsText.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchListener != null) {
            searchListener.remove();
        }
    }
}