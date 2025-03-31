package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

import android.view.View;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.impostersyndrom.view.ForgotPasswordActivity;
import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;
import com.example.impostersyndrom.view.RegisterActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
public class AuthenticationFlowTest {

    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Before
    public void setUp() {
        Intents.init();
        FirebaseAuth.getInstance().signOut();
    }

    @After
    public void tearDown() {
        Intents.release();
        FirebaseAuth.getInstance().signOut();
    }

    @Test
    public void testSuccessfulEmailPasswordLogin() throws TimeoutException {
        onView(withId(R.id.login_email)).perform(typeText("banisett@ualberta.ca"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText("111222"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.loginBtn)).perform(click());
        waitForView(withId(R.id.viewPager), 15000);
        onView(withId(R.id.viewPager)).check(matches(isDisplayed()));
        intended(hasComponent(MainActivity.class.getName()));
    }

    @Test
    public void testFailedLoginInvalidCredentials() throws TimeoutException {
        // Enter invalid credentials
        onView(withId(R.id.login_email)).perform(typeText("invalid@example.com"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText("wrongpass"));
        Espresso.closeSoftKeyboard();

        // Click login button
        onView(withId(R.id.loginBtn)).perform(click());

        // Wait for error to appear (if needed)
        try {
            Thread.sleep(2000); // Small delay to ensure UI updates
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check if the error is displayed
        onView(withId(R.id.layoutPassword))
                .check(matches(hasErrorText("Wrong password. Try again or click Forgot password to reset it.")));
    }

    @Test
    public void testNavigateToForgotPassword() {
        onView(withId(R.id.forgotPassword)).perform(click());
        intended(hasComponent(ForgotPasswordActivity.class.getName()));
    }

    @Test
    public void testNavigateToSignUp() {
        onView(withId(R.id.newUserSignUp)).perform(click());
        intended(hasComponent(RegisterActivity.class.getName()));
    }

    @Test
    public void testUsernameAlreadyExists() throws TimeoutException {
        // Navigate to RegisterActivity
        onView(withId(R.id.newUserSignUp)).perform(click());

        // Fill in registration form
        onView(withId(R.id.firstName)).perform(typeText("Ro"));
        onView(withId(R.id.lastName)).perform(typeText("Ba"));
        onView(withId(R.id.username)).perform(typeText("Roxsksk"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.email)).perform(typeText("test@example.com"));
        Espresso.closeSoftKeyboard();
        onView(withId(R.id.password)).perform(typeText("111222"));
        Espresso.closeSoftKeyboard();

        // Click register button
        onView(withId(R.id.registerBtn)).perform(click());

        // Wait for error to appear
        try {
            Thread.sleep(3000); // Wait for Firestore check
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify the error text - USING THE SAME APPROACH AS testFailedLoginInvalidCredentials
        onView(withId(R.id.layoutUsername))
                .check(matches(hasErrorText("Username is already taken")));
    }

    private void waitForView(Matcher<View> viewMatcher, long timeoutMillis) throws TimeoutException {
        waitForView(viewMatcher, timeoutMillis, true);
    }

    private void waitForView(Matcher<View> viewMatcher, long timeoutMillis, boolean shouldBeDisplayed) throws TimeoutException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                onView(viewMatcher).check(matches(shouldBeDisplayed ? isDisplayed() : not(isDisplayed())));
                return;
            } catch (NoMatchingViewException e) {
                if (shouldBeDisplayed) {
                    // View not found yet, keep waiting
                } else {
                    return; // View is gone, which is what we want
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        String state = shouldBeDisplayed ? "displayed" : "not displayed";
        throw new TimeoutException("Timed out waiting for view with matcher " + viewMatcher.toString() + " to be " + state);
    }

    // Custom matcher to check TextInputLayout error text
    public static Matcher<View> hasErrorText(final String expectedError) {
        return new BoundedMatcher<View, TextInputLayout>(TextInputLayout.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("has error text: " + expectedError);
            }

            @Override
            protected boolean matchesSafely(TextInputLayout layout) {
                final CharSequence error = layout.getError();
                return error != null && error.toString().equals(expectedError);
            }
        };
    }

    // Custom matcher for TextInputLayout error text
    private static Matcher<View> hasTextInputLayoutErrorText(final String expectedError) {
        return new BoundedMatcher<View, TextInputLayout>(TextInputLayout.class) {
            @Override
            public void describeTo(Description description) {
                description.appendText("with error text: " + expectedError);
            }

            @Override
            protected boolean matchesSafely(TextInputLayout item) {
                return expectedError.equals(item.getError());
            }
        };
    }

    // Waits for error to appear in TextInputLayout
    private void waitForErrorToAppear(int viewId, String errorText, long timeoutMillis)
            throws TimeoutException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            try {
                onView(withId(viewId))
                        .check(matches(hasTextInputLayoutErrorText(errorText)));
                return; // Success!
            } catch (Exception e) {
                // Error not yet appeared, keep waiting
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        throw new TimeoutException("Timed out waiting for error text: " + errorText);
    }
}

