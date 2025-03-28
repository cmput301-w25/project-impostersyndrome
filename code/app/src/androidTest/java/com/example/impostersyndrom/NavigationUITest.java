package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.util.Log;
import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.example.impostersyndrom.view.EmojiSelectionActivity;
import com.example.impostersyndrom.view.FollowingActivity;
import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;
import com.example.impostersyndrom.view.ProfileActivity;
import com.example.impostersyndrom.view.SearchActivity;
import com.example.impostersyndrom.view.UserProfileActivity;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class NavigationUITest {

    @Rule
    public IntentsTestRule<LoginActivity> intentsRule =
            new IntentsTestRule<>(LoginActivity.class);

    private UiDevice uiDevice;

    @Before
    public void setUp() throws TimeoutException {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Login to reach MainActivity
        onView(withId(R.id.login_email)).perform(typeText("banisett@ualberta.ca"));
        closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText("111222"));
        closeSoftKeyboard();
        onView(withId(R.id.loginBtn)).perform(click());

        // Wait for MainActivity to load
        waitForView(withId(R.id.viewPager), 15000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(hasComponent(MainActivity.class.getName()));
        waitForView(withId(R.id.viewPager), 15000);
    }

    @Test
    public void testNavigateToHome() throws Exception {
        // 1. Start on Home (MainActivity) - already there after login
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));

        // 2. Click Home button (should stay on MainActivity)
        onView(withId(R.id.homeButton)).perform(click());

        // 3. Verify still on MainActivity
        waitForView(withId(R.id.viewPager), 5000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(hasComponent(MainActivity.class.getName()));
    }

    @Test
    public void testNavigateToSearch() throws Exception {
        // 1. Click Search button
        onView(withId(R.id.searchButton)).perform(click());

        // 2. Wait for SearchActivity to load
        waitForView(withId(R.id.searchInput), 5000);

        // 3. Verify Search screen is displayed
        onView(withId(R.id.searchInput)).check(matches(isDisplayed()));
        intended(hasComponent(SearchActivity.class.getName()));
    }

    @Test
    public void testNavigateToAddMood() throws Exception {
        // 1. Click Add Mood button
        onView(withId(R.id.addMoodButton)).perform(click());

        // 2. Wait for EmojiSelectionActivity to load
        waitForView(withId(R.id.emoji5), 5000);

        // 3. Verify Add Mood screen is displayed
        onView(withId(R.id.emoji5)).check(matches(isDisplayed()));
        intended(hasComponent(EmojiSelectionActivity.class.getName()));
    }

    @Test
    public void testNavigateToFollowing() throws Exception {
        // 1. Click Following (Heart) button
        onView(withId(R.id.heartButton)).perform(click());

        // 2. Wait for FollowingActivity to load
        waitForView(withId(R.id.tabLayout), 5000);

        // 3. Verify Following screen is displayed
        onView(withId(R.id.tabLayout)).check(matches(isDisplayed()));
        intended(hasComponent(FollowingActivity.class.getName()));
    }

    @Test
    public void testNavigateToProfile() throws Exception {
        // 1. Click Profile button
        onView(withId(R.id.profileButton)).perform(click());

        // 2. Wait for UserProfileActivity to load
        waitForView(withId(R.id.usernameText), 5000);

        intended(hasComponent(ProfileActivity.class.getName()));
    }

    // Custom wait action
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

    // Wait for view helper with debug logging
    private void waitForView(Matcher<View> viewMatcher, long timeoutMillis) throws TimeoutException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                onView(viewMatcher).check(matches(isDisplayed()));
                Log.d("TestDebug", "View found: " + viewMatcher.toString());
                return;
            } catch (Exception e) {
                Log.d("TestDebug", "View not found yet: " + viewMatcher.toString() + ", elapsed: " + (System.currentTimeMillis() - startTime) + "ms");
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