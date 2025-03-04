package com.example.impostersyndrom;

import java.util.List;

public class MoodDataCache {
    private static MoodDataCache instance;
    private List moodDocs;

    private MoodDataCache() {
        // Private constructor to prevent instantiation
    }

    public static synchronized MoodDataCache getInstance() {
        if (instance == null) {
            instance = new MoodDataCache();
        }
        return instance;
    }

    public void setMoodDocs(List moodDocs) {
        this.moodDocs = moodDocs;
    }

    public List getMoodDocs() {
        return moodDocs;
    }

    public void clearCache() {
        moodDocs = null;
    }
}