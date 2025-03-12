package com.example.impostersyndrom;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private FirebaseFirestore db; // Firestore database instance
    private ListView moodListView; // ListView to display mood entries
    private MoodAdapter moodAdapter; // Adapter for the mood list
    private String userId; // ID of the current user
    private List<DocumentSnapshot> moodDocs = new ArrayList<>(); // List of mood documents from Firestore
    private boolean filterByRecentWeek = false; // Flag to track if the filter is active
    private MoodFilter moodFilter; // Instance of MoodFilter for filtering logic

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

        // Initialize MoodFilter
        moodFilter = new MoodFilter();

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
                            applyFilter(""); // Apply filter after fetching moods (no emotional state filter initially)
                        } else {
                            Toast.makeText(this, "No moods found!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to fetch moods!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Shows the filter dialog with options to filter moods.
     */
    private void showFilterDialog() {
        // Create the dialog
        Dialog filterDialog = new Dialog(this);
        filterDialog.setContentView(R.layout.filter_mood_dialog);

        // Set dialog window attributes
        Window window = filterDialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawableResource(android.R.color.transparent); // Transparent background
            window.setGravity(Gravity.CENTER); // Center the dialog vertically
        }

        // Get views from the dialog
        CheckBox checkboxRecentWeek = filterDialog.findViewById(R.id.checkboxRecentWeek);
        Spinner emotionalStateSpinner = filterDialog.findViewById(R.id.emotionalStateSpinner);
        ImageButton tickButton = filterDialog.findViewById(R.id.tickButton);

        // Set up the emotional state spinner
        List<String> emotionalStates = new ArrayList<>();
        emotionalStates.add("All Moods"); // First item for no filter
        emotionalStates.addAll(List.of(EmojiUtils.getEmojiDescriptions())); // Add all emoji descriptions

        // List of emoji drawable resource IDs (in the same order as descriptions)
        List<Integer> emojiDrawables = new ArrayList<>();
        emojiDrawables.add(R.drawable.emoji_happy);       // Happy
        emojiDrawables.add(R.drawable.emoji_confused);    // Confused
        emojiDrawables.add(R.drawable.emoji_disgust);     // Disgust
        emojiDrawables.add(R.drawable.emoji_angry);       // Angry
        emojiDrawables.add(R.drawable.emoji_sad);         // Sad
        emojiDrawables.add(R.drawable.emoji_fear);        // Fear
        emojiDrawables.add(R.drawable.emoji_shame);       // Shame
        emojiDrawables.add(R.drawable.emoji_surprised);   // Surprise

        // Create and set the adapter
        EmojiSpinnerAdapter spinnerAdapter = new EmojiSpinnerAdapter(this, emotionalStates, emojiDrawables);
        emotionalStateSpinner.setAdapter(spinnerAdapter);

        // Set the current filter state
        checkboxRecentWeek.setChecked(filterByRecentWeek);

        // Handle Tick Button click
        tickButton.setOnClickListener(v -> {
            filterByRecentWeek = checkboxRecentWeek.isChecked();
            String selectedDescription = (String) emotionalStateSpinner.getSelectedItem(); // Cast to String
            String selectedEmotionalState = "";

            // Map the selected description to the corresponding emoji key
            if (!selectedDescription.equals("All Moods")) {
                selectedEmotionalState = EmojiUtils.getEmojiKey(selectedDescription);
            }

            applyFilter(selectedEmotionalState); // Apply the filter
            filterDialog.dismiss(); // Close the dialog
        });

        // Show the dialog
        filterDialog.show();
    }

    /**
     * Applies the filter to the mood list based on the current filter settings.
     *
     * @param emotionalState The emotional state to filter by (empty string for no filter).
     */
    private void applyFilter(String emotionalState) {
        List<DocumentSnapshot> filteredMoods;

        if (filterByRecentWeek) {
            // Filter by recent week
            filteredMoods = moodFilter.filterByRecentWeek(moodDocs);
        } else {
            // Show all moods
            filteredMoods = new ArrayList<>(moodDocs);
        }

        // Filter by emotional state if one is selected
        if (!emotionalState.isEmpty()) {
            filteredMoods = moodFilter.filterByEmotionalState(filteredMoods, emotionalState);
        }

        // Update the adapter with the filtered list
        setupMoodAdapter(filteredMoods);
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