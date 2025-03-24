package com.example.impostersyndrom;

import com.example.impostersyndrom.model.User;
import com.example.impostersyndrom.model.UserData;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class UserSearchTest {
    private User user;
    private UserData userData;

    @Before
    public void setUp() {
        user = User.getInstance();
        userData = new UserData("alice123", "https://example.com/profile.jpg");
    }

    @Test
    public void testSetAndGetUserId() {
        user.setUserId("user_001");
        assertEquals("user_001", user.getUserId());
    }

    @Test
    public void testUserDataUsername() {
        assertEquals("alice123", userData.username);
    }

    @Test
    public void testUserDataProfileImageUrl() {
        assertEquals("https://example.com/profile.jpg", userData.profileImageUrl);
    }

    @Test
    public void testSingletonUniqueness() {
        User anotherInstance = User.getInstance();
        assertSame(user, anotherInstance);
    }

    @Test
    public void testUniqueUsernames() {
        Set<String> usernames = new HashSet<>();
        usernames.add("alice123");
        usernames.add("bob456");
        usernames.add("charlie789");

        String newUsername = "bob456";

        boolean isUnique = !usernames.contains(newUsername);
        assertFalse(isUnique);

        newUsername = "daisy007";
        isUnique = !usernames.contains(newUsername);
        assertTrue(isUnique);
    }
}
