package com.example.impostersyndrom;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db;
    private ListView moodListView;
    private MoodAdapter moodAdapter;
    private String userId;
    private List<DocumentSnapshot> moodDocs = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        moodListView = findViewById(R.id.moodListView);

        // Get userId
        userId = getIntent().getStringExtra("userId");
        if (userId == null && FirebaseAuth.getInstance().getCurrentUser() != null) {
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get userId from FirebaseAuth
        }
        if (userId == null) {
            Toast.makeText(this, "User ID is missing!", Toast.LENGTH_SHORT).show();
            // Redirect to LoginActivity if userId is missing
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Debug: Log userId
        Log.d("MainActivity", "userId: " + userId);

        // Fetch and display moods
        fetchMoods(userId);

        // Add Mood Button
        Button addButton = findViewById(R.id.addMoodButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EmojiSelectionActivity.class);
            intent.putExtra("userId", userId); // Ensure userId is not null here
            startActivity(intent);
        });
        // Inside onCreate() method, after setting the adapter

        // Logout Button
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // Logs out the user
            showToast("Logged out successfully!");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
            startActivity(intent);
            finish();
        });
    }

    private void fetchMoods(String userId) {
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            List moodDocs = snapshot.getDocuments();
                            moodAdapter = new MoodAdapter(this, moodDocs);
                            moodListView.setAdapter(moodAdapter);
                            Log.d("MainActivity", "Fetched " + moodDocs.size() + " moods");
                            moodListView.setOnItemClickListener((parent, view, position, id) -> {
                                if (moodDocs != null && moodDocs.size() > position) {
                                    DocumentSnapshot moodDoc = (DocumentSnapshot) moodDocs.get(position);
                                    Map<String, Object> data = moodDoc.getData();

                                    if (data != null) {
                                        // Retrieve mood data
                                        String emoji = (String) data.get("emotionalState");
                                        Timestamp timestamp = (Timestamp) data.get("timestamp");
                                        String reason = (String) data.get("reason");
                                        String group = (String) data.get("group");
                                        int color = data.get("color") != null ? ((Long) data.get("color")).intValue() : Color.WHITE;
                                        String imageUrl = (String) data.get("imageUrl");
                                        String emojiDescription = (String) data.get("emojiDescription");


                                        // Pass data to MoodDetailActivity
                                        Intent intent = new Intent(MainActivity.this, MoodDetailActivity.class);
                                        intent.putExtra("emoji", emoji);
                                        intent.putExtra("timestamp", timestamp);
                                        intent.putExtra("reason", reason);
                                        intent.putExtra("group", group);
                                        intent.putExtra("color", color);
                                        intent.putExtra("imageUrl", imageUrl);
                                        intent.putExtra("emojiDescription", emojiDescription);
                                        startActivity(intent);
                                    }}
                            });
                        } else {
                            Toast.makeText(this, "No moods found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch moods!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}