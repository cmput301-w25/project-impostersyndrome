package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasFlags;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.content.Intent;
import android.view.View;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.BoundedMatcher;

import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class MoodEventFlowTest {
    @Rule
    public IntentsTestRule<LoginActivity> intentsRule =
            new IntentsTestRule<>(LoginActivity.class);

    @Before
    public void setUp() throws TimeoutException {
        // Handle login
        onView(withId(R.id.login_email)).perform(typeText("banisett@ualberta.ca"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText("111222"));
        Espresso.closeSoftKeyboard();

        onView(withId(R.id.loginBtn)).perform(click());

        // Verify we're in MainActivity
        waitForView(withId(R.id.viewPager), 15000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(hasComponent(MainActivity.class.getName()));
    }

    @Test
    public void testAddBasicMood() throws TimeoutException {
        // 1. Click the add mood button in MainActivity
        onView(withId(R.id.addMoodButton)).perform(click());

        // 2. Verify we're in EmojiSelectionActivity and select Sad emoji
        onView(withId(R.id.emoji5)).check(matches(isDisplayed())); // Verify emoji selection screen
        onView(withId(R.id.emoji5)).perform(click()); // Click emoji5 to go to AddMoodActivity

        // 3. Verify we're in AddMoodActivity and check the emoji display
        onView(withId(R.id.emojiView)).check(matches(isDisplayed())); // Now emojiView should be present
        onView(withId(R.id.emojiDescription))
                .check(matches(withText(containsString("Sad"))));

        // 5. Submit the mood
        onView(withId(R.id.submitButton)).perform(click());

        // 6. Wait 6 seconds for the mood to save and navigate back
        onView(isRoot()).perform(waitFor(6000));

        // 7. Verify we're back on MainActivity
        waitForView(withId(R.id.viewPager), 15000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(allOf(
                hasComponent(MainActivity.class.getName()),
                hasFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
        ));
    }


    // Test 2: Add Mood Event with All Fields and verifies them
    @Test
    public void testAddMoodEventWithAllFields() throws TimeoutException {
        // 1. Click the add mood button in MainActivity
        onView(withId(R.id.addMoodButton)).perform(click());

        // 2. Verify we're in EmojiSelectionActivity and select Sad emoji (emoji5)
        onView(withId(R.id.emoji5)).check(matches(isDisplayed()));
        onView(withId(R.id.emoji5)).perform(click());

        // 3. Verify we're in AddMoodActivity and fill in details
        onView(withId(R.id.emojiView)).check(matches(isDisplayed()));
        onView(withId(R.id.addReasonEdit)).perform(typeText("TESTING RHHEHHH"), closeSoftKeyboard());
        onView(withId(R.id.groupButton)).perform(click());
        onView(withText("With another person")).perform(click());
        onView(withId(R.id.privacySwitch)).perform(click()); // Set to private

        // 4. Submit the mood
        onView(withId(R.id.submitButton)).perform(click());

        // 5. Wait 6 seconds for the mood to save and navigate back to MainActivity
        onView(isRoot()).perform(waitFor(6000));

        // 6. Verify we're back in MainActivity and MyMoodsFragment is updated
        waitForView(withId(R.id.viewPager), 5000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(allOf(
                hasComponent(MainActivity.class.getName()),
                hasFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK)
        ));

        // 7. Tap on the mood in MyMoodsFragment's moodListView to view details
        waitForView(withId(R.id.moodListView), 5000); // Wait for list to update
        onView(withId(R.id.moodListView)).check(matches(isDisplayed()));
        onData(anything())
                .inAdapterView(withId(R.id.moodListView))
                .atPosition(0)
                .perform(click());

        // 8. Verify mood details in MoodDetailActivity
        waitForView(withId(R.id.rootLayout), 5000); // Wait for MoodDetailActivity to load
        onView(withId(R.id.reasonView)).check(matches(withText("TESTING RHHEHHH")));
        onView(withId(R.id.groupView)).check(matches(withText("With another person")));
        onView(withId(R.id.emojiDescription)).check(matches(withText("Sad")));
    }

    public static Matcher<View> atPosition(final int position, final Matcher<View> itemMatcher) {
        return new BoundedMatcher<View, RecyclerView>(RecyclerView.class) {
            @Override public void describeTo(Description description) {
                description.appendText("has item at position " + position + ": ");
                itemMatcher.describeTo(description);
            }
            @Override protected boolean matchesSafely(final RecyclerView view) {
                RecyclerView.ViewHolder viewHolder = view.findViewHolderForAdapterPosition(position);
                return viewHolder != null && itemMatcher.matches(viewHolder.itemView);
            }
        };
    }

    public static ViewAction waitFor(long delay) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for " + delay + " milliseconds.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(delay);
            }
        };
    }

    private void waitForView(Matcher<View> viewMatcher, long timeoutMillis) throws TimeoutException {
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
}