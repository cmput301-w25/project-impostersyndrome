package com.example.impostersyndrom.network;

import java.util.List;

public class RecommendationResponse {
    public List<Track> tracks;

    public static class Track {
        public String name;
        public List<Artist> artists;
    }

    public static class Artist {
        public String name;
    }
}