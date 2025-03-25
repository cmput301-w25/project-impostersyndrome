package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;

import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import android.view.View;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.matcher.BoundedMatcher;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class MoodEventCreationDisplayTest {
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

        // 2. Verify we're in EmojiSelectionActivity and select Happy emoji
        onView(withId(R.id.emoji1)).check(matches(isDisplayed())); // Verify emoji selection screen
        onView(withId(R.id.emoji1)).perform(click()); // Click emoji1 to go to AddMoodActivity

        // 3. Verify we're in AddMoodActivity
        onView(withId(R.id.emojiView)).check(matches(isDisplayed())); // Now emojiView should be present

        // 4. Verify emoji selection
        onView(withId(R.id.emojiDescription))
                .check(matches(withText(containsString("Happy"))));

        // 5. Submit the mood
        onView(withId(R.id.submitButton)).perform(click());

        // 6. Wait 6 seconds for the mood to save and navigate back
        onView(isRoot()).perform(waitFor(6000));

        // 7. Verify we're back on MainActivity
        waitForView(withId(R.id.viewPager), 15000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(hasComponent(MainActivity.class.getName()));
    }


    //    // Test 2: Add Mood Event with All Fields
//    @Test
//    public void testAddMoodEventWithAllFields() {
//        onView(withId(R.id.addMoodButton)).perform(click());
//
//        // Select Sadness (emoji5)
//        onView(withId(R.id.emoji5)).perform(click());
//
//        // In AddMoodActivity, fill in details
//        onView(withId(R.id.addReasonEdit)).perform(typeText("Feeling down today"));
//        onView(withId(R.id.groupButton)).perform(click());
//        onView(withText("Alone")).perform(click());
//        onView(withId(R.id.privacySwitch)).perform(click()); // Set to private
//        onView(withId(R.id.submitButton)).perform(click());
//
//        // Verify in MyMoodsFragment
//        onView(withId(R.id.moodListView))
//                .check(matches(isDisplayed()));
//        onView(withId(R.id.reasonView))
//                .check(matches(withText("Feeling down today")));
//        onView(withId(R.id.socialSituationIcon))
//                .check(matches(withText(containsString("ic_alone"))));
//        onView(withId(R.id.emojiView))
//                .check(matches(withText(containsString("emoji_sad"))));
//    }
//
//    // Test 3: Emotional State Display Consistency
//    @Test
//    public void testEmotionalStateDisplayConsistency() {
//        // Add Anger
//        onView(withId(R.id.addMoodButton)).perform(click());
//        onView(withId(R.id.emoji4)).perform(click()); // emoji_angry
//        onView(withId(R.id.submitButton)).perform(click());
//
//        // Add Happiness
//        onView(withId(R.id.addMoodButton)).perform(click());
//        onView(withId(R.id.emoji1)).perform(click()); // emoji_happy
//        onView(withId(R.id.submitButton)).perform(click());
//
//        // Add Surprise
//        onView(withId(R.id.addMoodButton)).perform(click());
//        onView(withId(R.id.emoji8)).perform(click()); // emoji_surprised
//        onView(withId(R.id.submitButton)).perform(click());
//
//        // Verify all appear in MyMoodsFragment
//        onView(withId(R.id.moodListView))
//                .check(matches(isDisplayed()));
//        onView(withId(R.id.emojiView))
//                .check(matches(withText(containsString("emoji_angry"))));
//        onView(withId(R.id.emojiView))
//                .check(matches(withText(containsString("emoji_happy"))));
//        onView(withId(R.id.emojiView))
//                .check(matches(withText(containsString("emoji_surprised"))));
//        // Note: Color consistency needs manual verification as Espresso can't check drawables directly
//    }
//
//    // Test 4: View Mood Event Details
//    @Test
//    public void testViewMoodEventDetails() {
//        // Add a mood event
//        onView(withId(R.id.addMoodButton)).perform(click());
//        onView(withId(R.id.emoji6)).perform(click()); // emoji_fear
//        onView(withId(R.id.addReasonEdit)).perform(typeText("Scary movie"));
//        onView(withId(R.id.submitButton)).perform(click());
//
//        // Tap the first item in the mood list (MyMoodsFragment)
//        onView(withId(R.id.moodListView)).perform(click());
//
//        // Verify MoodDetailActivity (assuming it’s launched; adjust IDs based on actual layout)
//        onView(withId(R.id.mood_detail_emotional_state)) // Placeholder ID, replace with actual
//                .check(matches(withText("Fear")));
//        onView(withId(R.id.mood_detail_reason)) // Placeholder ID, replace with actual
//                .check(matches(withText("Scary movie")));
//    }

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