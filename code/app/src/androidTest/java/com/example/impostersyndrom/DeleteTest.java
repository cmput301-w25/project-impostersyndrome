package com.example.impostersyndrom;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.model.MoodDataManager;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DeleteTest {

    private MoodDataManager moodDataManager;
    private Mood testMood;

    @Before
    public void setUp() {
        moodDataManager = new MoodDataManager();

        testMood = new Mood(
                "confident",
                "🔥",
                new Date(),
                0xFFA500,
                "Testing delete functionality"
        );
        testMood.setUserId("instrumentedTestUser");
    }

    @Test
    public void testAddThenDeleteMood_andVerifyGone() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        moodDataManager.addMood(testMood, new MoodDataManager.OnMoodAddedListener() {
            @Override
            public void onMoodAdded() {
                System.out.println("Mood added with ID: " + testMood.getId());
                moodDataManager.deleteMood(testMood.getId(), new MoodDataManager.OnMoodDeletedListener() {
                    @Override
                    public void onMoodDeleted() {
                        System.out.println("Mood deleted. Verifying...");
                        FirebaseFirestore.getInstance()
                                .collection("moods")
                                .document(testMood.getId())
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    assertFalse("Mood should be deleted", documentSnapshot.exists());
                                    latch.countDown();
                                })
                                .addOnFailureListener(e -> {
                                    fail("Failed to verify deletion: " + e.getMessage());
                                    latch.countDown();
                                });
                    }
                    @Override
                    public void onError(String errorMessage) {
                        fail("Failed to delete mood: " + errorMessage);
                        latch.countDown();
                    }
                });
            }
            @Override
            public void onError(String errorMessage) {
                fail("Failed to add mood: " + errorMessage);
                latch.countDown();
            }
        });
        boolean completed = latch.await(15, TimeUnit.SECONDS);
        assertTrue("Firebase operation timed out", completed);
    }
}
