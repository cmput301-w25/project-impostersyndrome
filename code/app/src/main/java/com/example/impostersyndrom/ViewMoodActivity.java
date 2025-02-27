package com.example.impostersyndrom;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class ViewMoodActivity extends AppCompatActivity {
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_mood);
        db = FirebaseFirestore.getInstance();

        Button backButton = findViewById(R.id.backButton);

        // Set click listener for the back button
        backButton.setOnClickListener(v -> {
            // navigate to mainactivity
            Intent intent = new Intent(ViewMoodActivity.this, MainActivity.class);
            intent.putExtra("userId", getIntent().getStringExtra("userId")); // Pass userId back
            startActivity(intent);
            finish();
        });
        // getting userid form intent
        String userId = getIntent().getStringExtra("userId");

        if (userId != null) {
            fetchLatestMood(userId, () -> {

            });
        } else {
            Toast.makeText(this, "User ID is missing!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchLatestMood(String userId, Runnable onComplete) {
        Log.d("ViewMoodActivity", "Fetching moods for userId: " + userId);

        db.collection("moods")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        if (snapshot != null && !snapshot.isEmpty()) {
                            List<DocumentSnapshot> moodDocs = snapshot.getDocuments();
                            displayMoods(moodDocs); // Pass the list of moods to display
                        } else {
                            Log.d("ViewMoodActivity", "No mood entries available for userId: " + userId);
                            Toast.makeText(this, "No mood entries available!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Exception exception = task.getException();
                        if (exception != null) {
                            Log.e("ViewMoodActivity", "Failed to get mood: " + exception.getMessage(), exception);
                        }
                        Toast.makeText(this, "Failed to get mood", Toast.LENGTH_SHORT).show();
                    }
                    onComplete.run();
                });
    }

    private void displayMoods(List<DocumentSnapshot> moodDocs) {
        ListView moodListView = findViewById(R.id.moodListView);
        MoodAdapter adapter = new MoodAdapter(this, moodDocs);
        moodListView.setAdapter(adapter);
    }
}