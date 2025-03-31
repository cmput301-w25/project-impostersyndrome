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
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;

import android.view.View;

import androidx.test.espresso.ViewAssertion;
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
public class MoodHistorySortingTest {

    @Rule
    public IntentsTestRule<LoginActivity> intentsRule =
            new IntentsTestRule<>(LoginActivity.class);

    private static final String USER_EMAIL = "emo2@ualberta.ca";
    private static final String USER_PASSWORD = "123456";

    @Before
    public void setUp() throws Exception {
        // Log in once.
        login(USER_EMAIL, USER_PASSWORD);
        // (Assume the app persists login state and MainActivity is displayed.)
    }

    @Test
    public void testMoodHistorySorting() throws Exception {
        addMoodUnique("Mood1");
        Thread.sleep(1000);
        addMoodUnique("Mood2");
        Thread.sleep(1000);
        addMoodUnique("Mood3");

        Thread.sleep(2000);
        onData(anything())
                .inAdapterView(withId(R.id.moodListView))
                .atPosition(0)
                .onChildView(withId(R.id.reasonView))
                .check(matches(withText("Mood3")));

        onData(anything())
                .inAdapterView(withId(R.id.moodListView))
                .atPosition(1)
                .onChildView(withId(R.id.reasonView))
                .check(matches(withText("Mood2")));

        onData(anything())
                .inAdapterView(withId(R.id.moodListView))
                .atPosition(2)
                .onChildView(withId(R.id.reasonView))
                .check(matches(withText("Mood1")));

        onView(withId(R.id.moodListView)).check(matches(isDisplayed()));
        tearDown();
    }

    // Helper method to perform login.
    private void login(String email, String password) throws TimeoutException {
        onView(withId(R.id.login_email)).perform(typeText(email));
        closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText(password));
        closeSoftKeyboard();
        onView(withId(R.id.loginBtn)).perform(click());
        waitForView(withId(R.id.viewPager), 15000);
        intended(hasComponent(MainActivity.class.getName()));
    }

    private void addMoodUnique(String reason) throws Exception {
        onView(withId(R.id.addMoodButton)).perform(click());
        waitForView(withId(R.id.emoji1), 5000);
        onView(withId(R.id.emoji1)).perform(click());
        waitForView(withId(R.id.addReasonEdit), 5000);
        onView(withId(R.id.addReasonEdit))
                .perform(clearText(), typeText(reason), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.submitButton)).perform(click());
        waitForView(withId(R.id.moodListView), 10000);
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

    public void tearDown() {
        // The moods we added have reasons "Mood1", "Mood2", "Mood3".
        String[] moodReasons = {"Mood1", "Mood2", "Mood3"};
        for (String reason : moodReasons) {
            try {
                // If the mood is displayed, delete it.
                onView(withText(reason)).perform(longClick());
                waitForView(withText("Delete Mood"), 5000);
                onView(withId(R.id.deleteMoodOption)).perform(click());
                Thread.sleep(2000); // Allow deletion to process.
            } catch (Exception e) {
                // Ignore if not found.
            }
        }
    }

    private ViewAssertion isBelowOf(final Matcher<View> upperViewMatcher) {
        return (lowerView, noViewException) -> {
            if (noViewException != null) throw noViewException;

            final int[] upperLocation = new int[2];
            final int[] lowerLocation = new int[2];

            View upperView = getViewFromMatcher(upperViewMatcher);
            if (upperView == null) throw new AssertionError("Upper view not found.");

            upperView.getLocationOnScreen(upperLocation);
            lowerView.getLocationOnScreen(lowerLocation);

            if (lowerLocation[1] <= upperLocation[1]) {
                throw new AssertionError("Expected lower view to be below upper view, but it wasn't.");
            }
        };
    }

    private View getViewFromMatcher(final Matcher<View> matcher) {
        final View[] foundView = new View[1];
        try {
            onView(matcher).check((view, e) -> foundView[0] = view);
        } catch (Exception e) {
            return null;
        }
        return foundView[0];
    }
}
