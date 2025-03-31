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
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.impostersyndrom.MoodEventFlowTest.waitFor;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.not;
import android.graphics.Color;

import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.impostersyndrom.model.Mood;
import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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
        Thread.sleep(1000);
        
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

        //onView(isRoot()).perform(waitForIdle());
    }

    @Test
    public void testDateFilter() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference moodsRef = db.collection("moods");

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        Date oldDate = sdf.parse("01-01-2025 00:00");
        Date currentDate = new Date();

        Mood oldMood = new Mood("emoji_happy", "Happy", oldDate, Color.parseColor("#FFCC00"), "Old Mood");
        oldMood.setId("oldMood123");
        oldMood.setUserId("hLuJvhlYpqRPBXrHJlhg2n6QduJ3");

        Mood newMood = new Mood("emoji_angry", "Angry", currentDate, Color.parseColor("#FF4D00"), "New Mood");
        newMood.setId("newMood123");
        newMood.setUserId("hLuJvhlYpqRPBXrHJlhg2n6QduJ3");

        moodsRef.document(oldMood.getId()).set(oldMood);
        moodsRef.document(newMood.getId()).set(newMood);

        Thread.sleep(2000);
        onView(withId(R.id.swipeRefreshLayout)).perform(ViewActions.swipeDown());
        Thread.sleep(3000);

        onView(withId(R.id.filterButton)).perform(click());
        waitForView(withText("Filter Mood"), 5000);
        onView(withId(R.id.checkboxRecentWeek)).perform(click());
        onView(withId(R.id.tickButton)).perform(click());
        waitForView(withId(R.id.moodListView), 10000);

        onView(withText("Old Mood")).check(doesNotExist());

        onView(withId(R.id.filterButton)).perform(click());
        waitForView(withText("Filter Mood"), 5000);
        onView(withId(R.id.checkboxRecentWeek)).perform(click());
        onView(withId(R.id.tickButton)).perform(click());
    }

    @Test
    public void testEmotionFilter() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference moodsRef = db.collection("moods");

        Date now = new Date();

        Mood happyMood = new Mood("emoji_happy", "Happy", now, Color.parseColor("#FFCC00"), "Feeling joyful!");
        happyMood.setId("happyMood123");
        happyMood.setUserId("hLuJvhlYpqRPBXrHJlhg2n6QduJ3");

        Mood sadMood = new Mood("emoji_sad", "Sad", now, Color.parseColor("#0099CC"), "Feeling blue.");
        sadMood.setId("sadMood123");
        sadMood.setUserId("hLuJvhlYpqRPBXrHJlhg2n6QduJ3");

        moodsRef.document(happyMood.getId()).set(happyMood);
        moodsRef.document(sadMood.getId()).set(sadMood);

        // Refresh mood list
        Thread.sleep(2000);
        onView(withId(R.id.swipeRefreshLayout)).perform(ViewActions.swipeDown());
        Thread.sleep(3000);

        onView(withId(R.id.filterButton)).perform(click());
        waitForView(withText("Filter Mood"), 5000);

        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        Thread.sleep(500);
        onData(anything())
                .inRoot(isPlatformPopup())
                .atPosition(1)
                .perform(click());
        Thread.sleep(500);
        onView(withId(R.id.tickButton)).perform(click());
        waitForView(withId(R.id.moodListView), 10000);

        onView(withText("Feeling blue.")).check(doesNotExist());
        Thread.sleep(3000);

        onView(withId(R.id.filterButton)).perform(click());
        waitForView(withText("Filter Mood"), 5000);
        onView(withId(R.id.emotionalStateSpinner)).perform(click());
        Thread.sleep(500);
        onData(anything())
                .inRoot(isPlatformPopup())
                .atPosition(0)
                .perform(click());
        Thread.sleep(500);
        onView(withId(R.id.tickButton)).perform(click());
        Thread.sleep(2000);
    }

    public static ViewAction waitForIdle() {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isRoot();
            }

            @Override
            public String getDescription() {
                return "Wait for UI thread to be idle";
            }

            @Override
            public void perform(UiController uiController, View view) {
                uiController.loopMainThreadUntilIdle();
            }
        };
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
    public void tearDown() throws Exception {
        while (true) {
            try {
                // Try long-clicking the first item in the mood list (position 0).
                onData(anything())
                        .inAdapterView(withId(R.id.moodListView))
                        .atPosition(0)
                        .perform(longClick());

                // Wait for the "Delete Mood" option to appear and perform the delete action.
                waitForView(withText("Delete Mood"), 5000);
                onView(withId(R.id.deleteMoodOption)).perform(click());

                // Pause briefly to allow deletion to complete.
                Thread.sleep(2000);
            } catch (Exception e) {
                // If no more moods exist (adapter is empty), exit the loop.
                break;
            }
        }
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
