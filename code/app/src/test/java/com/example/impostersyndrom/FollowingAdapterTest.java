package com.example.impostersyndrom;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.FollowingAdapter;
import com.example.impostersyndrom.model.UserData;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FollowingAdapterTest {

    @Mock Context mockContext;
    @Mock View mockView;
    @Mock TextView mockUsernameText;
    @Mock Button mockUnfollowButton;
    @Mock FirebaseFirestore mockFirestore;

    private FollowingAdapter adapter;
    private List<UserData> testUsers;

    @Before
    public void setUp() {
        // Setup test data
        testUsers = new ArrayList<>();
        testUsers.add(new UserData("testUser1", "http://test.com/img1.jpg"));
        testUsers.add(new UserData("testUser2", "http://test.com/img2.jpg"));

        // Mock view components
        when(mockView.findViewById(R.id.usernameTextView)).thenReturn(mockUsernameText);
        when(mockView.findViewById(R.id.unfollowButton)).thenReturn(mockUnfollowButton);

        // Create adapter
        adapter = new FollowingAdapter(mockContext, testUsers, "currentUser123");
        adapter.db = mockFirestore; // Inject mock Firestore
    }

    @Test
    public void testGetCount() {
        assertEquals(2, adapter.getCount());
    }

    @Test
    public void testGetItem() {
        assertEquals("testUser1", adapter.getItem(0).username);
        assertEquals("http://test.com/img1.jpg", adapter.getItem(0).profileImageUrl);
    }

    @Test
    public void testViewBinding() {
        View result = adapter.getView(0, mockView, null);

        verify(mockUsernameText).setText("testUser1");
        verify(mockUnfollowButton).setOnClickListener(any(View.OnClickListener.class));
        assertNotNull(result);
    }

    @Test
    public void testEmptyState() {
        FollowingAdapter emptyAdapter = new FollowingAdapter(mockContext, new ArrayList<>(), "user123");
        TextView mockEmptyView = mock(TextView.class);
        emptyAdapter.setEmptyMessageView(mockEmptyView);

        assertEquals(0, emptyAdapter.getCount());
        verify(mockEmptyView).setText("You're not following anyone yet");
    }
}