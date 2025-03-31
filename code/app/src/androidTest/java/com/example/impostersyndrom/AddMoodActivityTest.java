package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.content.Intent;
import android.graphics.Color;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.view.AddMoodActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AddMoodActivityTest {

    @Rule
    public ActivityScenarioRule<AddMoodActivity> scenario = new
            ActivityScenarioRule<AddMoodActivity>(AddMoodActivity.class);

    @Before
    public void seedDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference moodsRef = db.collection("moods");
        Mood[] moods = {
                new Mood("emoji_happy", "Happy", new Date(), Color.parseColor("#FFCC00"), "test1 reason"),
                new Mood("emoji_angry", "Angry", new Date(), Color.parseColor("#FF4D00"), "test2 reason")
        };
        for (Mood mood : moods) {
            DocumentReference docRef = moodsRef.document(mood.getId());
            docRef.set(mood);
        }
    }

    @Before
    public void setUp() {
        // Launch AddMoodActivity with a valid userId and mood
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AddMoodActivity.class);
        intent.putExtra("userId", "testUserId");
        intent.putExtra("mood", new Mood("emoji_happy", "Happy", new Date(), Color.parseColor("#FFCC00"), "test1 reason"));
        ActivityScenario.launch(intent);
    }

    @Test
    public void testAddMoodFlow() {
        onView(withId(R.id.emojiView)).check(matches(isDisplayed())); // Check if emoji view is displayed

        onView(withId(R.id.emojiDescription)).check(matches(isDisplayed()));
        onView(withId(R.id.emojiDescription)).check(matches(withText("Happy")));

        onView(withId(R.id.dateTimeView)).check(matches(isDisplayed()));

        onView(withId(R.id.addReasonEdit)).perform(typeText("Feeling great today!"));
        onView(withId(R.id.addReasonEdit)).check(matches(withText("Feeling great today!")));

        onView(withId(R.id.reasonCharCount)).check(matches(withText("20/20")));

        onView(withId(R.id.submitButton)).check(matches(isDisplayed()));
        onView(withId(R.id.submitButton)).perform(click());

    }
}