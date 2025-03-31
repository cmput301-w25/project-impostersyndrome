package com.example.impostersyndrom;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


import com.example.impostersyndrom.model.Mood;

import org.junit.Test;


import org.junit.Before;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;



public class MoodLocationTest {
    private Mood moodWithLocation;
    private Mood moodWithoutLocation;
    private static final double TEST_LAT = 43.6532;
    private static final double TEST_LNG = -79.3832;

    @Before
    public void setUp() {
        moodWithLocation = new Mood("happy", "smile", new Date(), 0xFF00FF00, "At park");
        moodWithLocation.setLocation(TEST_LAT, TEST_LNG);

        moodWithoutLocation = new Mood("sad", "frown", new Date(), 0xFFFF0000, "At home");
    }

    @Test
    public void testHasLocation() {
        assertTrue(moodWithLocation.hasLocation());
        assertFalse(moodWithoutLocation.hasLocation());
    }

    @Test
    public void testGetCoordinates() {
        assertEquals(TEST_LAT, moodWithLocation.getLatitude(), 0.0001);
        assertEquals(TEST_LNG, moodWithLocation.getLongitude(), 0.0001);

        assertNull(moodWithoutLocation.getLatitude());
        assertNull(moodWithoutLocation.getLongitude());
    }

    @Test
    public void testSetLocation() {
        moodWithoutLocation.setLocation(34.0522, -118.2437);
        assertTrue(moodWithoutLocation.hasLocation());
        assertEquals(34.0522, moodWithoutLocation.getLatitude(), 0.0001);
    }

    @Test
    public void testFilterMoodsByLocation() {
        List<Mood> moods = new ArrayList<>();
        moods.add(moodWithLocation);
        moods.add(moodWithoutLocation);

        List<Mood> filtered = new ArrayList<>();
        for (Mood mood : moods) {
            if (mood.hasLocation()) {
                filtered.add(mood);
            }
        }

        assertEquals(1, filtered.size());
        assertTrue(filtered.contains(moodWithLocation));
    }

    @Test
    public void testCalculateDistance() {
        Mood mood2 = new Mood("excited", "grin", new Date(), 0xFFFFFF00, "Nearby");
        mood2.setLocation(43.6550, -79.3800);

        double distance = calculateDistance(
                moodWithLocation.getLatitude(), moodWithLocation.getLongitude(),
                mood2.getLatitude(), mood2.getLongitude()
        );

        assertTrue(distance < 0.5);
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double R = 6371; // Earth radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng/2) * Math.sin(dLng/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // Edge Cases
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidCoordinates() {
        moodWithLocation.setLocation(-91.0, 181.0);
    }

}
