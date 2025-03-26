package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasFlags;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.anything;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertTrue;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static org.hamcrest.Matchers.allOf;

import static java.util.concurrent.CompletableFuture.allOf;

import android.content.Intent;
import android.util.Log;
import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiSelector;

import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class MoodPhotoTest {

    @Rule
    public IntentsTestRule<LoginActivity> intentsRule =
            new IntentsTestRule<>(LoginActivity.class);

    private UiDevice uiDevice;

    @Before
    public void setUp() throws TimeoutException {
        // Initialize UiDevice
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        // Login (same as your existing setup)
        onView(withId(R.id.login_email)).perform(typeText("banisett@ualberta.ca"));
        closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText("111222"));
        closeSoftKeyboard();
        onView(withId(R.id.loginBtn)).perform(click());

        // Wait for main activity
        waitForView(withId(R.id.viewPager), 15000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(hasComponent(MainActivity.class.getName()));
    }

    @Test
    public void testAddMoodAndOpenGallery() throws Exception {
        // 1. Start mood creation
        onView(withId(R.id.addMoodButton)).perform(click());
        onView(withId(R.id.emoji5)).perform(click());

        // 2. Click camera menu button and select gallery
        onView(withId(R.id.cameraMenuButton)).perform(click());
        onView(withText("Choose from Gallery")).perform(click());

        // 3. Handle permission dialog with UiAutomator
        UiObject allowButton = uiDevice.findObject(new UiSelector().text("Allow"));
        if (allowButton.exists()) {
            allowButton.click();
        } else {
            // Optional: Log if permission dialog wasn't found
            System.out.println("Permission dialog not found, might already be granted");
        }

        // 5. Press back to return to app
        uiDevice.pressBack();
    }

    @Test
    public void testRemovePhotoFromMood() throws Exception {
        // 1. Wait for moods to load (with timeout)

        onView(isRoot()).perform(waitFor(2000));
        onData(anything())
                .inAdapterView(withId(R.id.moodListView))
                .atPosition(0) // First item
                .onChildView(withId(R.id.emojiView))
                .perform(longClick());

        onView(isRoot()).perform(waitFor(2000));

        // 3. Handle Edit Mood option
        onView(withId(R.id.editMoodOption)).perform(click());

        onView(isRoot()).perform(waitFor(2000));

        // 5. Remove photo flow (with verification)
        try {
            // Check if photo exists before trying to remove
            if (isViewDisplayed(R.id.EditImagePreview)) {
                // Open camera menu
                onView(withId(R.id.EditCameraMenuButton))
                        .perform(click());

                // Wait for menu to appear
                onView(isRoot()).perform(waitFor(500));

                // Remove photo
                onView(withText("Remove Photo"))
                        .perform(click());

                // Verify removal
                onView(withId(R.id.EditImagePreview))
                        .check(matches(not(isDisplayed())));
            }
        } catch (Exception e) {
            Log.d("Test", "Photo removal step failed: " + e.getMessage());
            throw e;
        }

        // 6. Save changes
        onView(withId(R.id.submitButton))
                .perform(click());

        waitForView(withId(R.id.viewPager), 5000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
    }

    @Test
    public void testEditMoodAndOpenGallery() throws Exception {
        // 1. Wait for moods to load
        onView(isRoot()).perform(waitFor(2000));

        // 2. Long press on first mood item
        onData(anything())
                .inAdapterView(withId(R.id.moodListView))
                .atPosition(0) // First item
                .onChildView(withId(R.id.emojiView))
                .perform(longClick());

        onView(isRoot()).perform(waitFor(2000));

        // 3. Handle Edit Mood option
        onView(withId(R.id.editMoodOption)).perform(click());

        onView(isRoot()).perform(waitFor(2000));

        // 4. Open camera menu and select gallery
        onView(withId(R.id.EditCameraMenuButton)).perform(click());
        onView(isRoot()).perform(waitFor(500)); // Wait for menu to appear
        onView(withText("Choose from Gallery")).perform(click());

        // 5. Handle permission dialog with UiAutomator
        UiObject allowButton = uiDevice.findObject(new UiSelector().text("Allow"));
        if (allowButton.exists()) {
            allowButton.click();
        } else {
            System.out.println("Permission dialog not found, might already be granted");
        }

        // 6. Press back to return to EditMoodActivity and verify
        uiDevice.pressBack();
    }

    // Helper method to check view visibility
    private boolean isViewDisplayed(int viewId) {
        try {
            onView(withId(viewId)).check(matches(isDisplayed()));
            return true;
        } catch (NoMatchingViewException e) {
            return false;
        }
    }




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