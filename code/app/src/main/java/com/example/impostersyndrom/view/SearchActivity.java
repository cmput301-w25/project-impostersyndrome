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
import androidx.appcompat.app.AppCompatActivity;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.UserListAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private EditText searchInput;
    private ListView userListView;
    private ImageButton addMoodButton, homeButton, profileButton, heartButton, clearSearch;
    private FirebaseFirestore db;
    private List<String> userList = new ArrayList<>();
    private UserListAdapter adapter;
    private FirebaseAuth auth; // Firebase Auth for Logout

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
        heartButton = findViewById(R.id.heartButton); // Initialize heart button
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
                    Log.d("SearchActivity", "Clear button VISIBLE"); // Debugging log
                } else {
                    clearSearch.setVisibility(View.GONE);
                    Log.d("SearchActivity", "Clear button GONE"); // Debugging log
                }
                searchUsers(s.toString().trim());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // Clear text when "X" button is clicked
        clearSearch.setOnClickListener(v -> {
            searchInput.setText("");
            clearSearch.setVisibility(View.GONE);
            Log.d("SearchActivity", "Clear button CLICKED & HIDDEN"); // Debugging log
        });

        // Navigate to Main Page when Home Button is Pressed
        homeButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close SearchActivity
        });

        // Navigate to Add Mood Page
        addMoodButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, EmojiSelectionActivity.class);
            startActivity(intent);
        });

        // Navigate to Following Page when Heart Button is Pressed
        heartButton.setOnClickListener(v -> {
            Intent intent = new Intent(SearchActivity.this, FollowingActivity.class);
            startActivity(intent);
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(SearchActivity.this, ProfileActivity.class));
        });
    }

    private void searchUsers(String query) {
        TextView noResultsText = findViewById(R.id.noResultsText); // Get reference
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get logged-in user ID

        if (query.isEmpty()) {
            userList.clear();
            adapter.notifyDataSetChanged();
            noResultsText.setVisibility(View.GONE); // Hide "No results found."
            return;
        }

        db.collection("users")
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", query + "\uf8ff")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String username = document.getString("username");
                            String userId = document.getId(); // Get Firestore user ID

                            if (username != null && !userId.equals(currentUserId)) {
                                userList.add(username);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (userList.isEmpty() && !query.isEmpty()) {
                            noResultsText.setVisibility(View.VISIBLE);
                        } else {
                            noResultsText.setVisibility(View.GONE);
                        }
                    } else {
                        noResultsText.setVisibility(View.VISIBLE);
                    }
                });
    }
}
