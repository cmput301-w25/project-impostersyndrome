package com.example.impostersyndrom;

import java.util.List;

/**
 * Singleton cache for storing and managing mood document data.
 *
 * Provides a thread-safe mechanism to:
 * - Store mood documents
 * - Retrieve cached mood documents
 * - Clear the cache
 *
 * @author Roshan Banisetti
 */
public class MoodDataCache {
    /**
     * Singleton instance of the MoodDataCache.
     */
    private static MoodDataCache instance;

    /**
     * List to store mood documents.
     */
    private List moodDocs;

    /**
     * Private constructor to prevent direct instantiation.
     *
     * Enforces the Singleton pattern by making the constructor private.
     */
    private MoodDataCache() {
        // Private constructor to prevent instantiation
    }

    /**
     * Provides global access to the singleton instance.
     *
     * Creates the instance if it doesn't exist, ensuring thread-safety
     * through synchronized access.
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
     * @return List of cached mood documents
     */
    public List getMoodDocs() {
        return moodDocs;
    }

    /**
     * Clears the current cache.
     *
     * Removes all stored mood documents.
     */
    public void clearCache() {
        moodDocs = null;
    }
}