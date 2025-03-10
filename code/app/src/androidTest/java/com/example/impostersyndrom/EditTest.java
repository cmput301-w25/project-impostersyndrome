package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.graphics.Color;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

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
public class EditTest {
    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new
            ActivityScenarioRule<MainActivity>(MainActivity.class);
    @Before
    public void seedDatabase() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference moodsRef = db.collection("moods");
        Mood[] moods = {
                new Mood("emoji_happy", "Happy", new Date(), Color.parseColor("#FFCC00"), "test1 reason").setId("test1").setUserId("CXoVdFoN5yXbjveG4ocy8hRIS3s1"),
                new Mood("emoji_angry", "Angry", new Date(), Color.parseColor("#FF4D00"), "test2 reason").setId("test2").setUserId("CXoVdFoN5yXbjveG4ocy8hRIS3s1")
        };
        for (Mood mood : moods) {
            DocumentReference docRef = moodsRef.document(mood.getId());
            docRef.set(mood);
        }

    }

    @Test
    public void testEditMovie(){
        onView(withId(R.id.login_email)).perform(ViewActions.typeText("gtse1@ualberta.ca"));
        onView(withId(R.id.login_password)).perform(ViewActions.typeText("gtse1pw"));
        onView(withId(R.id.loginBtn)).perform(click());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withText("test1 reason")).perform(ViewActions.longClick());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withId(R.id.loginBtn)).perform(click());



    }
}

