package com.example.impostersyndrom;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    private EditText searchInput;
    private ListView userListView;
    private ImageButton addMoodButton, homeButton, logoutButton, clearSearch;
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
        logoutButton = findViewById(R.id.profileButton);
        clearSearch = findViewById(R.id.clearSearch);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance(); // Initialize Firebase Auth

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

        // Log Out User when Logout Button is Pressed
        logoutButton.setOnClickListener(v -> {
            auth.signOut(); // Firebase logout
            Toast.makeText(SearchActivity.this, "Logged out successfully!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SearchActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear backstack
            startActivity(intent);
            finish();
        });
    }

    private void searchUsers(String query) {
        TextView noResultsText = findViewById(R.id.noResultsText); // Get reference

        if (query.isEmpty()) {
            // 🔹 Hide everything if the search bar is empty
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
                            if (username != null) userList.add(username);
                        }
                        adapter.notifyDataSetChanged();

                        // 🔹 Show "No results found" only if input is typed AND no users are found
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
