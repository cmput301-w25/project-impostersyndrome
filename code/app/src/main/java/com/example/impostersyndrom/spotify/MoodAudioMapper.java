package com.example.impostersyndrom.spotify;

import java.util.HashMap;
import java.util.Map;

public class MoodAudioMapper {

    private static class MoodAudioFeatures {
        String genre;
        float valence;
        float energy;

        MoodAudioFeatures(String genre, float valence, float energy) {
            this.genre = genre;
            this.valence = valence;
            this.energy = energy;
        }
    }

    private final Map<String, MoodAudioFeatures> moodToAudioFeatures = new HashMap<>();

    public MoodAudioMapper() {
        // Initialize mood-to-audio feature mappings
        moodToAudioFeatures.put("emoji_happy", new MoodAudioFeatures("dance-pop", 0.85f, 0.75f));
        moodToAudioFeatures.put("emoji_confused", new MoodAudioFeatures("indie-pop", 0.5f, 0.45f));
        moodToAudioFeatures.put("emoji_disgust", new MoodAudioFeatures("punk-rock", 0.25f, 0.65f));
        moodToAudioFeatures.put("emoji_angry", new MoodAudioFeatures("metal", 0.15f, 0.95f));
        moodToAudioFeatures.put("emoji_sad", new MoodAudioFeatures("sad-pop", 0.2f, 0.35f));
        moodToAudioFeatures.put("emoji_fear", new MoodAudioFeatures("dark-ambient", 0.2f, 0.6f));
        moodToAudioFeatures.put("emoji_shame", new MoodAudioFeatures("acoustic", 0.25f, 0.15f));
        moodToAudioFeatures.put("emoji_surprised", new MoodAudioFeatures("dance-pop", 0.75f, 0.9f));
    }

    public String getGenre(String emoji) {
        MoodAudioFeatures features = moodToAudioFeatures.getOrDefault(emoji, new MoodAudioFeatures("pop", 0.5f, 0.5f));
        return features.genre;
    }

    public float getValence(String emoji) {
        MoodAudioFeatures features = moodToAudioFeatures.getOrDefault(emoji, new MoodAudioFeatures("pop", 0.5f, 0.5f));
        return features.valence;
    }

    public float getEnergy(String emoji) {
        MoodAudioFeatures features = moodToAudioFeatures.getOrDefault(emoji, new MoodAudioFeatures("pop", 0.5f, 0.5f));
        return features.energy;
    }
}