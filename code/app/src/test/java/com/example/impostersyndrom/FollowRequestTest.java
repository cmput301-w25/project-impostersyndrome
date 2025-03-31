package com.example.impostersyndrom;

import static org.junit.Assert.*;

import com.example.impostersyndrom.model.FollowRequest;

import org.junit.Before;
import org.junit.Test;

public class FollowRequestTest {
    private FollowRequest followRequest;

    @Before
    public void setUp() {
        followRequest = new FollowRequest(
                "sender123",
                "receiver456",
                "user1",
                "user2",
                "pending"
        );
    }

    @Test
    public void testGetters() {
        assertEquals("sender123", followRequest.getSenderId());
        assertEquals("receiver456", followRequest.getReceiverId());
        assertEquals("user1", followRequest.getSenderUsername());
        assertEquals("user2", followRequest.getReceiverUsername());
        assertEquals("pending", followRequest.getStatus());
    }

    @Test
    public void testEmptyConstructor() {
        FollowRequest empty = new FollowRequest();
        assertNotNull(empty);
    }
}