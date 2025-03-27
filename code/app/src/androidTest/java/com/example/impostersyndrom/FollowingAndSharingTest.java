package com.example.impostersyndrom;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withChild;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import com.example.impostersyndrom.view.LoginActivity;
import com.example.impostersyndrom.view.MainActivity;
import com.example.impostersyndrom.view.SearchActivity;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.concurrent.TimeoutException;

@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING) // Order tests alphabetically by method name
public class FollowingAndSharingTest {

    @Rule
    public IntentsTestRule<LoginActivity> intentsRule =
            new IntentsTestRule<>(LoginActivity.class);

    private UiDevice uiDevice;
    private static final String USER1_USERNAME = "roxsksk";
    private static final String USER1_EMAIL = "banisett@ualberta.ca";
    private static final String USER1_PASSWORD = "111222";
    private static final String USER2_USERNAME = "NotRoxsksk";
    private static final String USER2_EMAIL = "banisettirosh@gmail.com";
    private static final String USER2_PASSWORD = "123456";

    @Before
    public void setUp() throws TimeoutException {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    }

    @Test
    public void test1_SendFollowRequest() throws Exception {
        // Login as User 1 (roxsksk)
        login(USER1_EMAIL, USER1_PASSWORD);

        // 1. Go to search screen
        onView(withId(R.id.searchButton)).perform(click());
        waitForView(withId(R.id.searchInput), 5000);

        // 2. Search for User 2 (NotRoxsksk)
        onView(withId(R.id.searchInput)).perform(typeText(USER2_USERNAME));
        closeSoftKeyboard();
        onView(isRoot()).perform(waitFor(2000));

        // 3. Send follow request
        onView(allOf(withId(R.id.followButton), isDisplayed())).perform(click());

        // 5. Go back to MainActivity (Home)
        onView(withId(R.id.homeButton)).perform(click());
        waitForView(withId(R.id.viewPager), 5000);

        // 6. Open side navigation drawer
        onView(withId(R.id.menuButton)).perform(click());
        waitForView(withId(R.id.logoutContainer), 5000);

        // 7. Logout User 1
        onView(withId(R.id.logoutContainer)).perform(click());
    }

    @Test
    public void test2_SendFollowRequestRevised() throws Exception {
        login(USER2_EMAIL, USER2_PASSWORD);

        // 2. Go to following screen to check pending requests
        onView(withId(R.id.heartButton)).perform(click());
        waitForView(withId(R.id.listView), 10000); // Increased timeout for Firestore load
        onView(isRoot()).perform(waitFor(2000));   // Additional delay for adapter update

        // 3. Verify follow request from User 1 appears in the ListView
        onView(allOf(withId(R.id.usernameTextView), withText(USER1_USERNAME)))
                .check(matches(isDisplayed()));
    }


    // Helper method to login
    private void login(String email, String password) throws TimeoutException {
        onView(withId(R.id.login_email)).perform(typeText(email));
        closeSoftKeyboard();
        onView(withId(R.id.login_password)).perform(typeText(password));
        closeSoftKeyboard();
        onView(withId(R.id.loginBtn)).perform(click());
        waitForView(withId(R.id.viewPager), 15000);
        intended(hasComponent(MainActivity.class.getName()));
    }

    // Custom matcher to find a view that is a descendant of a parent with a specific child
    private static Matcher<View> isDescendantOf(Matcher<View> parentMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                while (parent != null) {
                    if (parent instanceof View && parentMatcher.matches(parent)) {
                        return true;
                    }
                    parent = parent.getParent();
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is descendant of a view matching: ");
                parentMatcher.describeTo(description);
            }
        };
    }

    // Custom matcher to find a view with a specific child
    private static Matcher<View> withChild(Matcher<View> childMatcher) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                if (!(view instanceof ViewGroup)) return false;
                ViewGroup group = (ViewGroup) view;
                for (int i = 0; i < group.getChildCount(); i++) {
                    if (childMatcher.matches(group.getChildAt(i))) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with child matching: ");
                childMatcher.describeTo(description);
            }
        };
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

    // Custom action to select a tab at a specific position
    private ViewAction selectTabAtPosition(final int position) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isDisplayed();
            }

            @Override
            public String getDescription() {
                return "Select tab at position " + position;
            }

            @Override
            public void perform(UiController uiController, View view) {
                if (view instanceof com.google.android.material.tabs.TabLayout) {
                    com.google.android.material.tabs.TabLayout tabLayout = (com.google.android.material.tabs.TabLayout) view;
                    tabLayout.getTabAt(position).select();
                }
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

    // Custom matcher to check child count of ListView
    private Matcher<View> hasChildCount(final int count) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View view) {
                return view instanceof android.widget.ListView && ((android.widget.ListView) view).getChildCount() == count;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("ListView with child count: " + count);
            }
        };
    }
}