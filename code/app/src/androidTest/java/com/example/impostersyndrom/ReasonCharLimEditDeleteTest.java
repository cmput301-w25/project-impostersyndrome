package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

import android.view.View;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ReasonCharLimEditDeleteTest {

    @Rule
    public IntentsTestRule<LoginActivity> intentsRule =
            new IntentsTestRule<>(LoginActivity.class);

    private static final String USER_EMAIL = "emo2@ualberta.ca";
    private static final String USER_PASSWORD = "123456";
    // This unique reason is used for seeding a mood to later delete.
    private static final String UNIQUE_MOOD_REASON = "DeleteThisMoodUnique";

    @Before
    public void setUp() throws Exception {
        // Log in using LoginActivity.
        login(USER_EMAIL, USER_PASSWORD);
        // Ensure a mood exists to edit. If not, seed one.
        seedMood();
    }

    @Test
    public void testEditMoodEventDetails() throws Exception {
        onData(anything()).inAdapterView(withId(R.id.moodListView)).atPosition(0).perform(longClick());

        waitForView(withText("Edit Mood"), 5000);
        onView(withId(R.id.editMoodOption)).perform(click());

        waitForView(withId(R.id.EditEmoji), 5000);
        onView(withId(R.id.EditEmoji)).perform(click());

        waitForView(withId(R.id.emojiII), 5000);
        onView(withId(R.id.emojiII)).perform(click());

        waitForView(withId(R.id.EditReason), 5000);
        onView(withId(R.id.EditReason))
                .perform(clearText(), typeText("Feeling confused instead of happy"), ViewActions.closeSoftKeyboard());

        onView(withId(R.id.submitButton)).perform(click());

        waitForView(withId(R.id.moodListView), 10000);
        onView(withId(R.id.moodListView))
                .check(matches(hasDescendant(withText("Feeling confused instead of happy"))));
    }

    @Test
    public void testDeleteMood() throws Exception {
        seedMoodUnique(UNIQUE_MOOD_REASON);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        onView(withText(UNIQUE_MOOD_REASON)).perform(longClick());

        waitForView(withText("Delete Mood"), 5000);
        onView(withId(R.id.deleteMoodOption)).perform(click());

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        onView(withText("DeleteThisMoodUnique")).check(doesNotExist());
    }

    @Test
    public void testReasonCharacterLimit() throws Exception {
        onView(withId(R.id.addMoodButton)).perform(click());
        waitForView(withId(R.id.emoji1), 10000);
        onView(withId(R.id.emoji1)).perform(click());
        waitForView(withId(R.id.addReasonEdit), 5000);

        // Create a long string (e.g., 250 characters).
        String longText = new String(new char[250]).replace("\0", "a");
        onView(withId(R.id.addReasonEdit))
                .perform(clearText(), typeText(longText), ViewActions.closeSoftKeyboard());
        Thread.sleep(2000);

        // Check that the character counter displays "200/200".
        onView(withId(R.id.reasonCharCount)).check(matches(withText("200/200")));
    }

    private void login(String email, String password) throws TimeoutException {
        onView(withId(R.id.login_email)).perform(typeText(email));
        closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText(password));
        closeSoftKeyboard();
        onView(withId(R.id.loginBtn)).perform(click());
        waitForView(withId(R.id.viewPager), 15000);
        intended(hasComponent(MainActivity.class.getName()));
    }

    private void seedMood() throws Exception {
        onView(withId(R.id.addMoodButton)).perform(click());
        waitForView(withId(R.id.emoji1), 5000);
        onView(withId(R.id.emoji1)).perform(click());
        waitForView(withId(R.id.addReasonEdit), 5000);
        onView(withId(R.id.addReasonEdit))
                .perform(typeText("I feel happy"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.submitButton)).perform(click());
        waitForView(withId(R.id.moodListView), 10000);
    }

    // Seed a mood event with a unique reason (useful for deletion tests).
    private void seedMoodUnique(String reason) throws Exception {
        onView(withId(R.id.addMoodButton)).perform(click());
        waitForView(withId(R.id.emoji1), 5000);
        onView(withId(R.id.emoji1)).perform(click());
        waitForView(withId(R.id.addReasonEdit), 5000);
        onView(withId(R.id.addReasonEdit))
                .perform(clearText(), typeText(reason), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.submitButton)).perform(click());
        waitForView(withId(R.id.moodListView), 10000);
    }

    // Helper method to wait for a view to be displayed within a given timeout.
    private void waitForView(final Matcher<View> viewMatcher, long timeoutMillis) throws TimeoutException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                onView(viewMatcher).check(matches(isDisplayed()));
                return;
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new TimeoutException("Timed out waiting for view: " + viewMatcher.toString());
    }

    @After
    public void tearDown() {
        // Clean up any seeded moods.
        // Try to delete moods with known seed texts.
        String[] seedTexts = {"Feeling confused instead of happy", "I feel happy", UNIQUE_MOOD_REASON};
        for (String text : seedTexts) {
            try {
                // Attempt to find and delete the mood.
                onView(withText(text)).perform(longClick());
                waitForView(withText("Delete Mood"), 5000);
                onView(withId(R.id.deleteMoodOption)).perform(click());
                Thread.sleep(5000);
            } catch (Exception e) {
                // If not found or deletion fails, ignore.
            }
        }
        Intents.release();
    }
}
