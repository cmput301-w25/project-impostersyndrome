package com.example.impostersyndrom.model;

import java.util.List;

/**
 * Singleton cache for storing and managing mood document data in a thread-safe manner.
 *
 * @author Roshan Banisetti
 */
public class MoodDataCache {
    private static MoodDataCache instance;
    private List moodDocs;

    /**
     * Private constructor to enforce the Singleton pattern.
     */
    private MoodDataCache() {
        // Private constructor to prevent instantiation
    }

    /**
     * Retrieves the singleton instance of MoodDataCache, creating it if necessary.
     *
     * @return The singleton instance of MoodDataCache
     */
    public static synchronized MoodDataCache getInstance() {
        if (instance == null) {
            instance = new MoodDataCache();
        }
        return instance;
    }

    /**
     * Sets the mood documents in the cache.
     *
     * @param moodDocs List of mood documents to be cached
     */
    public void setMoodDocs(List moodDocs) {
        this.moodDocs = moodDocs;
    }

    /**
     * Retrieves the cached mood documents.
     *
     * @return List of cached mood documents, or null if not set
     */
    public List getMoodDocs() {
        return moodDocs;
    }

    /**
     * Clears all stored mood documents from the cache.
     */
    public void clearCache() {
        moodDocs = null;
    }
}