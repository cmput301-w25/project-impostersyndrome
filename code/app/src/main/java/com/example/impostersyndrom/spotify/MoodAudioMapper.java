package com.example.impostersyndrom.spotify;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps mood emojis to corresponding Spotify audio features (genre, valence, energy).
 * This class provides a way to translate emotional states into music recommendations.
 *
 * @author Roshan
 */
public class MoodAudioMapper {

    /**
     * Inner class representing audio features for a specific mood.
     */
    private static class MoodAudioFeatures {
        String genre;
        float valence;
        float energy;

        /**
         * Constructs audio features for a mood.
         * @param genre The music genre associated with the mood
         * @param valence The valence value (0-1) indicating musical positivity
         * @param energy The energy value (0-1) indicating track intensity
         */
        MoodAudioFeatures(String genre, float valence, float energy) {
            this.genre = genre;
            this.valence = valence;
            this.energy = energy;
        }
    }

    private final Map<String, MoodAudioFeatures> moodToAudioFeatures = new HashMap<>();

    /**
     * Constructs the mapper and initializes default mood-to-feature mappings.
     */
    public MoodAudioMapper() {
        // Initialize mood-to-audio feature mappings
        moodToAudioFeatures.put("emoji_happy", new MoodAudioFeatures("dance-pop", 0.85f, 0.75f));
        moodToAudioFeatures.put("emoji_confused", new MoodAudioFeatures("indie-pop", 0.5f, 0.45f));
        moodToAudioFeatures.put("emoji_disgust", new MoodAudioFeatures("punk-rock", 0.25f, 0.65f));
        moodToAudioFeatures.put("emoji_angry", new MoodAudioFeatures("metal", 0.15f, 0.95f));
        moodToAudioFeatures.put("emoji_sad", new MoodAudioFeatures("acoustic", 0.15f, 0.25f));
        moodToAudioFeatures.put("emoji_fear", new MoodAudioFeatures("dark-ambient", 0.2f, 0.6f));
        moodToAudioFeatures.put("emoji_shame", new MoodAudioFeatures("folk", 0.2f, 0.15f));
        moodToAudioFeatures.put("emoji_surprised", new MoodAudioFeatures("dance-pop", 0.75f, 0.9f));
    }

    /**
     * Gets the music genre associated with a mood emoji.
     * @param emoji The mood emoji identifier
     * @return The associated music genre, or "pop" as default
     */
    public String getGenre(String emoji) {
        MoodAudioFeatures features = moodToAudioFeatures.getOrDefault(emoji, new MoodAudioFeatures("pop", 0.5f, 0.5f));
        return features.genre;
    }

    /**
     * Gets the valence value associated with a mood emoji.
     * @param emoji The mood emoji identifier
     * @return The valence value (0-1), or 0.5 as default
     */
    public float getValence(String emoji) {
        MoodAudioFeatures features = moodToAudioFeatures.getOrDefault(emoji, new MoodAudioFeatures("pop", 0.5f, 0.5f));
        return features.valence;
    }

    /**
     * Gets the energy value associated with a mood emoji.
     * @param emoji The mood emoji identifier
     * @return The energy value (0-1), or 0.5 as default
     */
    public float getEnergy(String emoji) {
        MoodAudioFeatures features = moodToAudioFeatures.getOrDefault(emoji, new MoodAudioFeatures("pop", 0.5f, 0.5f));
        return features.energy;
    }
}