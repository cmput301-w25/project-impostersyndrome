package com.example.impostersyndrom;

import com.example.impostersyndrom.model.Mood;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class MoodDetailsTest {
    private Mood mood;

    @Before
    public void setUp() {
        mood = new Mood("sad", "Crying", new Date(), 0x0000FF, "Exam stress");
    }

    @Test
    public void testReasonCharacterLimit() {
        String shortReason = "Too tired to study";
        mood.setReason(shortReason);
        assertTrue(mood.getReason().length() <= 200);
    }

    @Test
    public void testReasonExceedCharacterLimit() {
        String longReason = "A".repeat(250);
        mood.setReason(longReason);
        assertTrue(mood.getReason().length() > 200);
    }


    @Test
    public void testSetSocialSituation() {
        mood.setGroup("with two to several people");
        assertEquals("with two to several people", mood.getGroup());
    }

    @Test
    public void testSetImageUrl() {
        String imageUrl = "https://example.com/image.jpg";
        mood.setImageUrl(imageUrl);
        assertEquals(imageUrl, mood.getImageUrl());
    }

    @Test
    public void testEmptyImageUrl() {
        mood.setImageUrl("");
        assertEquals("", mood.getImageUrl());
    }
}
