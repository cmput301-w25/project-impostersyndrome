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

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;

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
            userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        if (userId == null) {
            Toast.makeText(this, "User ID is missing!", Toast.LENGTH_SHORT).show();
            // Redirect to LoginActivity if userId is missing
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Check if data was preloaded
        boolean dataPreloaded = getIntent().getBooleanExtra("dataPreloaded", false);

        if (dataPreloaded && MoodDataCache.getInstance().getMoodDocs() != null) {
            // Use pre-fetched data
            setupMoodAdapter(MoodDataCache.getInstance().getMoodDocs());
            // Clear the cache after using it
            MoodDataCache.getInstance().clearCache();
        } else {
            // Fetch data if not pre-loaded
            fetchMoods(userId);
        }

        // Add Mood Button
        Button addButton = findViewById(R.id.addMoodButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EmojiSelectionActivity.class);
            intent.putExtra("userId", userId);
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

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh moods when returning to this activity (e.g., after adding a new mood)
        if (userId != null) {
            fetchMoods(userId);
        }
    }

    private void setupMoodAdapter(List moodDocs) {
        if (moodDocs != null && !moodDocs.isEmpty()) {
            moodAdapter = new MoodAdapter(this, moodDocs);
            moodListView.setAdapter(moodAdapter);
        } else {
            Toast.makeText(this, "No moods found!", Toast.LENGTH_SHORT).show();
        }
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
                            setupMoodAdapter(moodDocs);
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

                            moodListView.setOnItemLongClickListener((parent, view, position, id) -> {
                                if (moodDocs != null && moodDocs.size() > position) {
                                    DocumentSnapshot moodDoc = (DocumentSnapshot) moodDocs.get(position);
                                    showBottomSheetDialog(moodDoc);
                                }
                                return true; // Return true to indicate that the long press was handled
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

    private void showBottomSheetDialog(DocumentSnapshot moodDoc) {
        // Create bottom sheet dialog
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_mood_options, null);

        // Find views inside the bottom sheet
        TextView editMood = bottomSheetView.findViewById(R.id.editMoodOption);
        TextView deleteMood = bottomSheetView.findViewById(R.id.deleteMoodOption);

        // Handle Edit option
        editMood.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditMoodActivity.class);
            intent.putExtra("moodId", moodDoc.getId());
            startActivity(intent);
            bottomSheetDialog.dismiss();
        });

        // Handle Delete option
        deleteMood.setOnClickListener(v -> {
            db.collection("moods").document(moodDoc.getId()).delete()
                    .addOnSuccessListener(aVoid -> {
                        showToast("Mood deleted!");
                        fetchMoods(userId); // Refresh list after deletion
                    })
                    .addOnFailureListener(e -> showToast("Failed to delete mood"));
            bottomSheetDialog.dismiss();
        });

        // Set view and show dialog
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

}

