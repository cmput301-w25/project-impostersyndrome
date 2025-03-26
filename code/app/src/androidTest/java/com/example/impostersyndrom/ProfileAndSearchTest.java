package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.core.AllOf.allOf;

import android.util.Log;
import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class ProfileAndSearchTest {

    @Rule
    public IntentsTestRule<LoginActivity> intentsRule =
            new IntentsTestRule<>(LoginActivity.class);

    private UiDevice uiDevice;
    private static final String TEST_USERNAME = "roxsksk"; // Logged-in user's username
    private static final String SEARCH_USERNAME = "bhuvan"; // Known user in Firestore for testing

    @Before
    public void setUp() throws TimeoutException {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Login
        onView(withId(R.id.login_email)).perform(typeText("banisett@ualberta.ca"));
        closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText("111222"));
        closeSoftKeyboard();
        onView(withId(R.id.loginBtn)).perform(click());

        // Wait for MainActivity
        waitForView(withId(R.id.viewPager), 15000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(hasComponent(MainActivity.class.getName()));
    }

    @Test
    public void testViewOwnProfile() throws Exception {
        // 1. Navigate to profile screen
        onView(withId(R.id.profileButton)).perform(click());

        // 2. Wait for profile screen to load
        waitForView(withId(R.id.usernameText), 5000);

        // 3. Confirm username is displayed
        onView(withId(R.id.usernameText))
                .check(matches(withText(TEST_USERNAME)));
    }

    @Test
    public void testSearchForAnotherUser() throws Exception {
        // 1. Go to search screen
        onView(withId(R.id.searchButton)).perform(click());

        // 2. Wait for search screen to load
        waitForView(withId(R.id.searchInput), 5000);

        // 3. Enter a known username (search triggers automatically via TextWatcher)
        onView(withId(R.id.searchInput)).perform(typeText(SEARCH_USERNAME));
        closeSoftKeyboard();
        onView(isRoot()).perform(waitFor(2000)); // Wait 2s for ListView to update

        // 4. Confirm username is displayed in the ListView
        Log.d("TestDebug", "Checking for username: " + SEARCH_USERNAME);
        onView(allOf(withId(R.id.usernameTextView), withText(SEARCH_USERNAME)))
                .check(matches(isDisplayed()));
    }

    // Custom wait action
    public static ViewAction waitFor(long delay) {
        return new ViewAction() {
            @Override
            public org.hamcrest.Matcher<View> getConstraints() {
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

    // Wait for view helper
    private void waitForView(org.hamcrest.Matcher<View> viewMatcher, long timeoutMillis) throws TimeoutException {
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
