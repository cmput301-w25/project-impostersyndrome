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
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

import android.graphics.Color;
import android.view.View;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
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
public class CommentFilterTest {

    @Rule
    public IntentsTestRule<LoginActivity> intentsRule =
            new IntentsTestRule<>(LoginActivity.class);

    private static final String USER_EMAIL = "emo2@ualberta.ca";
    private static final String USER_PASSWORD = "123456";
    private static final String USER1_EMAIL = "hipersn@gmail.com";
    private static final String USER1_PASSWORD = "123456";

    @Before
    public void setUp() throws Exception {
        login(USER_EMAIL, USER_PASSWORD);
    }

    @Test
    public void testCommentFilter() throws Exception {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference moodsRef = db.collection("moods");

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault());
        Date oldDate = sdf.parse("01-01-2025 00:00");
        Date currentDate = new Date();

        Mood oldMood = new Mood("emoji_happy", "Happy", oldDate, Color.parseColor("#FFCC00"), "Old Mood");
        oldMood.setId("oldMood123");
        oldMood.setUserId("EPqalueqLW4iktHYvV6kzGSeBe2");

        Mood newMood = new Mood("emoji_angry", "Angry", currentDate, Color.parseColor("#FF4D00"), "New Mood");
        newMood.setId("newMood123");
        newMood.setUserId("EPqalueqLW4iktHYvV6kzGSeBe2");

        moodsRef.document(oldMood.getId()).set(oldMood);
        moodsRef.document(newMood.getId()).set(newMood);
        Thread.sleep(2000);

        onView(withText("Following")).perform(click());

        onView(withId(R.id.filterButton)).perform(click());
        waitForView(withText("Filter Mood"), 5000);
        onView(withId(R.id.checkboxRecentWeek)).perform(click());
        onView(withId(R.id.tickButton)).perform(click());
        Thread.sleep(2000);

        onView(withText("Old Mood")).check(doesNotExist());


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

}
