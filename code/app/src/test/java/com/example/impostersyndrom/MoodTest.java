package com.example.impostersyndrom;

import com.example.impostersyndrom.model.Mood;
import org.junit.Test;
import org.junit.Before;

import java.util.Date;

import static org.junit.Assert.*;

public class MoodTest {
    private Mood mood;

    @Before
    public void setUp() {
        mood = new Mood("happy", "Smiling", new Date(), 0xFFFF00, "Had a great lunch");
    }

    @Test
    public void testMoodCreation() {
        assertEquals("happy", mood.getEmotionalState());
        assertEquals("Smiling", mood.getEmojiDescription());
        assertNotNull(mood.getTimestamp());
        assertEquals(0xFFFF00, mood.getColor());
        assertEquals("Had a great lunch", mood.getReason());
        assertEquals("alone", mood.getGroup());  // default group
    }

    @Test
    public void testEditMoodDetails() {
        mood.setReason("Got a job offer");
        mood.setGroup("with one other person");
        mood.setEmotionalState("surprise");

        assertEquals("Got a job offer", mood.getReason());
        assertEquals("with one other person", mood.getGroup());
        assertEquals("surprise", mood.getEmotionalState());
    }

    @Test
    public void testPrivateMoodToggle() {
        mood.setPrivateMood(true);
        assertTrue(mood.isPrivateMood());

        mood.setPrivateMood(false);
        assertFalse(mood.isPrivateMood());
    }

    @Test
    public void testLocationAssignment() {
        mood.setLatitude(53.5461);
        mood.setLongitude(-113.4938);

        assertEquals(53.5461, mood.getLatitude(), 0.0001);
        assertEquals(-113.4938, mood.getLongitude(), 0.0001);
    }

}

