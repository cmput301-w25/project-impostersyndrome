package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import androidx.test.espresso.action.ViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new
            ActivityScenarioRule<MainActivity>(MainActivity.class);

    @Test
    public void testBackButton() {
        onView(withId(R.id.backButton)).check(matches(isDisplayed()));
        onView(withId(R.id.backButton)).perform(click());
    }

    @Test
    public void testSubmitButton() {
        onView(withId(R.id.submitButton)).check(matches(isDisplayed()));
        onView(withId(R.id.submitButton)).perform(click());
    }

    @Test
    public void testCameraMenuButton() {
        onView(withId(R.id.cameraMenuButton)).check(matches(isDisplayed()));
        onView(withId(R.id.cameraMenuButton)).perform(click());
    }

    @Test
    public void testGroupButton() {
        onView(withId(R.id.groupButton)).check(matches(isDisplayed()));
        onView(withId(R.id.groupButton)).perform(click());
    }

    @Test
    public void testAddReasonEditText() {
        onView(withId(R.id.addReasonEdit)).check(matches(isDisplayed()));
        onView(withId(R.id.addReasonEdit)).perform(typeText("Feeling great today!"));
        onView(withId(R.id.addReasonEdit)).check(matches(withText("Feeling great today!")));
    }

    @Test
    public void testReasonCharCount() {
        onView(withId(R.id.addReasonEdit)).perform(typeText("Happy"));
        onView(withId(R.id.reasonCharCount)).check(matches(withText("5/20")));
    }

    @Test
    public void testEmojiView() {
        onView(withId(R.id.emojiView)).check(matches(isDisplayed()));
    }

    @Test
    public void testEmojiDescription() {
        onView(withId(R.id.emojiDescription)).check(matches(isDisplayed()));
    }

    @Test
    public void testDateTimeView() {
        onView(withId(R.id.dateTimeView)).check(matches(isDisplayed()));
    }

    @Test
    public void testImagePreview() {
        onView(withId(R.id.imagePreview)).check(matches(isDisplayed()));
    }
}