package com.example.impostersyndrom.network;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SpotifyRecommendationResponse {
    @SerializedName("tracks")
    public List<Track> tracks;

    public static class Track {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;

        @SerializedName("artists")
        public List<Artist> artists;

        @SerializedName("uri")
        public String uri;

        @SerializedName("album") // Add the album field
        public Album album;
    }

    public static class Artist {
        @SerializedName("name")
        public String name;
    }

    public static class Album {
        @SerializedName("images")
        public List<Image> images;
    }

    public static class Image {
        @SerializedName("url")
        public String url;

        @SerializedName("height")
        public int height;

        @SerializedName("width")
        public int width;
    }
}