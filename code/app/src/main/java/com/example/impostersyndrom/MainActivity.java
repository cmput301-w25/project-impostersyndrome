package com.example.impostersyndrom;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ImageButton;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * MainActivity is the main screen of the application where users can view their mood entries.
 * It displays a list of moods, allows users to add new moods, and provides options to edit or delete existing moods.
 *
 * @author ImposterSyndrome
 */
public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db; // Firestore database instance
    private ListView moodListView; // ListView to display mood entries
    private MoodAdapter moodAdapter; // Adapter for the mood list
    private String userId; // ID of the current user
    private List<DocumentSnapshot> moodDocs = new ArrayList<>(); // List of mood documents from Firestore
    private boolean filterByRecentWeek = false; // Flag to track if the filter is active

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

        // Get userId from intent or FirebaseAuth
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
        ImageButton addMoodButton = findViewById(R.id.addMoodButton);
        addMoodButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EmojiSelectionActivity.class);
            intent.putExtra("userId", userId);
            startActivity(intent);
        });

        // Logout Button
        ImageButton logoutButton = findViewById(R.id.profileButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // Logs out the user
            showToast("Logged out successfully!");
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clears backstack
            startActivity(intent);
            finish();
        });

        // Filter Button
        ImageButton filterButton = findViewById(R.id.filterButton);
        filterButton.setOnClickListener(v -> showFilterDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh moods when returning to this activity (e.g., after adding a new mood)
        if (userId != null) {
            fetchMoods(userId);
        }
    }

    /**
     * Sets up the MoodAdapter with the provided list of mood documents.
     *
     * @param moodDocs The list of mood documents to display.
     */
    private void setupMoodAdapter(List<DocumentSnapshot> moodDocs) {
        if (moodDocs != null && !moodDocs.isEmpty()) {
            moodAdapter = new MoodAdapter(this, moodDocs);
            moodListView.setAdapter(moodAdapter);

            // Set click listener for mood items
            moodListView.setOnItemClickListener((parent, view, position, id) -> {
                DocumentSnapshot moodDoc = moodDocs.get(position);
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
                }
            });

            // Set long-click listener for mood items
            moodListView.setOnItemLongClickListener((parent, view, position, id) -> {
                DocumentSnapshot moodDoc = moodDocs.get(position);
                showBottomSheetDialog(moodDoc);
                return true; // Return true to indicate that the long press was handled
            });
        } else {
            Toast.makeText(this, "No moods found!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Fetches mood entries from Firestore for the current user.
     *
     * @param userId The ID of the current user.
     */
    private void fetchMoods(String userId) {
        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            moodDocs = snapshot.getDocuments();
                            applyFilter(); // Apply filter after fetching moods
                        } else {
                            Toast.makeText(this, "No moods found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch moods!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Applies the filter to the mood list based on the current filter settings.
     */
    private void applyFilter() {
        List<DocumentSnapshot> filteredMoods = new ArrayList<>();

        if (filterByRecentWeek) {
            // Calculate the timestamp for 7 days ago
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -7);
            long oneWeekAgo = calendar.getTimeInMillis();

            // Filter moods from the last 7 days
            for (DocumentSnapshot moodDoc : moodDocs) {
                Timestamp timestamp = moodDoc.getTimestamp("timestamp");
                if (timestamp != null && timestamp.toDate().getTime() >= oneWeekAgo) {
                    filteredMoods.add(moodDoc);
                }
            }
        } else {
            // Show all moods
            filteredMoods.addAll(moodDocs);
        }

        // Update the adapter with the filtered list
        setupMoodAdapter(filteredMoods);
    }

    /**
     * Shows the filter dialog with options to filter moods.
     */
    private void showFilterDialog() {
        // Create the dialog
        Dialog filterDialog = new Dialog(this);
        filterDialog.setContentView(R.layout.filter_mood_dialog);

        // Get views from the dialog
        CheckBox checkboxRecentWeek = filterDialog.findViewById(R.id.checkboxRecentWeek);
        ImageButton tickButton = filterDialog.findViewById(R.id.tickButton);

        // Set the current filter state
        checkboxRecentWeek.setChecked(filterByRecentWeek);

        // Handle Tick Button click
        tickButton.setOnClickListener(v -> {
            filterByRecentWeek = checkboxRecentWeek.isChecked();
            applyFilter(); // Apply the filter
            filterDialog.dismiss(); // Close the dialog
        });

        // Show the dialog
        filterDialog.show();
    }

    /**
     * Displays a toast message.
     *
     * @param message The message to display.
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Displays a bottom sheet dialog with options to edit or delete a mood.
     *
     * @param moodDoc The Firestore document representing the mood.
     */
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

            // Pass mood details to EditMoodActivity
            intent.putExtra("moodId", moodDoc.getId());
            intent.putExtra("emoji", (String) moodDoc.get("emotionalState"));
            intent.putExtra("timestamp", moodDoc.getTimestamp("timestamp"));
            intent.putExtra("reason", (String) moodDoc.get("reason"));
            intent.putExtra("imageUrl", (String) moodDoc.get("imageUrl"));
            intent.putExtra("color", ((Long) moodDoc.get("color")).intValue());
            intent.putExtra("group", (String) moodDoc.get("group"));

            startActivity(intent);
            bottomSheetDialog.dismiss();
        });

        // Handle Delete option
        deleteMood.setOnClickListener(v -> {
            String moodId = moodDoc.getId();
            deleteMoodAndImage(moodId);
            showToast("Mood deleted!");
            fetchMoods(userId); // Refresh list after deletion
            bottomSheetDialog.dismiss();
        });

        // Set view and show dialog
        bottomSheetDialog.setContentView(bottomSheetView);
        bottomSheetDialog.show();
    }

    /**
     * Deletes a mood and its associated image (if any) from Firestore and Firebase Storage.
     *
     * @param moodId The ID of the mood to delete.
     */
    private void deleteMoodAndImage(String moodId) {
        db.collection("moods").document(moodId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("imageUrl");

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            StorageReference imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                            imageRef.delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firebase Storage", "Image deleted successfully");
                                        db.collection("moods").document(moodId)
                                                .delete()
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Log.d("Firestore", "Mood deleted successfully");
                                                    fetchMoods(userId); // Refresh list after deletion
                                                })
                                                .addOnFailureListener(e -> Log.e("Firestore", "Failed to delete mood", e));
                                    })
                                    .addOnFailureListener(e -> Log.e("Firebase Storage", "Failed to delete image", e));
                        } else {
                            db.collection("moods").document(moodId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Firestore", "Mood deleted successfully");
                                        fetchMoods(userId); // Refresh list after deletion
                                    })
                                    .addOnFailureListener(e -> Log.e("Firestore", "Failed to delete mood", e));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to fetch mood details", e));
    }
}
