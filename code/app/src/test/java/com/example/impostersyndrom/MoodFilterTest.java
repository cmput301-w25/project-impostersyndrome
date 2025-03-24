package com.example.impostersyndrom;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class MoodFilterTest {
    private List<Map<String, Object>> moodDocs;

    @Before
    public void setUp() {
        moodDocs = new ArrayList<>();

        // Mood within 7 days
        Map<String, Object> mood1 = new HashMap<>();
        mood1.put("timestamp", new Timestamp(new Date()));
        mood1.put("emotionalState", "happy");
        mood1.put("reason", "great day");

        // Mood older than 7 days
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -10);
        Map<String, Object> mood2 = new HashMap<>();
        mood2.put("timestamp", new Timestamp(cal.getTime()));
        mood2.put("emotionalState", "sad");
        mood2.put("reason", "lost phone");

        moodDocs.add(mood1);
        moodDocs.add(mood2);
    }

    @Test
    public void testFilterByRecentWeek() {
        long oneWeekMillis = 7L * 24 * 60 * 60 * 1000;
        long now = new Date().getTime();

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> mood : moodDocs) {
            Timestamp ts = (Timestamp) mood.get("timestamp");
            if (ts != null && now - ts.toDate().getTime() <= oneWeekMillis) {
                filtered.add(mood);
            }
        }

        assertEquals(1, filtered.size());
        assertEquals("happy", filtered.get(0).get("emotionalState"));
    }

    @Test
    public void testFilterByEmotionalState() {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> mood : moodDocs) {
            if ("sad".equals(mood.get("emotionalState"))) {
                filtered.add(mood);
            }
        }

        assertEquals(1, filtered.size());
        assertEquals("sad", filtered.get(0).get("emotionalState"));
    }

    @Test
    public void testFilterByReason() {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> mood : moodDocs) {
            String reason = (String) mood.get("reason");
            if (reason != null && reason.contains("phone")) {
                filtered.add(mood);
            }
        }

        assertEquals(1, filtered.size());
        assertTrue(filtered.get(0).get("reason").toString().contains("phone"));
    }

    @Test
    public void testCombinedFilter() {
        long oneWeekMillis = 7L * 24 * 60 * 60 * 1000;
        long now = new Date().getTime();

        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> mood : moodDocs) {
            Timestamp ts = (Timestamp) mood.get("timestamp");
            String moodType = (String) mood.get("emotionalState");
            String reason = (String) mood.get("reason");

            boolean withinWeek = ts != null && now - ts.toDate().getTime() <= oneWeekMillis;
            boolean moodMatch = "happy".equals(moodType);
            boolean reasonMatch = reason != null && reason.contains("great");

            if (withinWeek && moodMatch && reasonMatch) {
                filtered.add(mood);
            }
        }

        assertEquals(1, filtered.size());
        assertEquals("happy", filtered.get(0).get("emotionalState"));
        assertTrue(filtered.get(0).get("reason").toString().contains("great"));
    }
}
