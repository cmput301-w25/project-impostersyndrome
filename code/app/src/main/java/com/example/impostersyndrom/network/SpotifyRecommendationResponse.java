package com.example.impostersyndrom.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SpotifyRecommendationResponse {
    @SerializedName("tracks")
    public List<Track> tracks;

    public static class Track {
        @SerializedName("name")
        public String name;

        @SerializedName("artists")
        public List<Artist> artists;

        @SerializedName("uri")
        public String uri;
    }

    public static class Artist {
        @SerializedName("name")
        public String name;
    }
}