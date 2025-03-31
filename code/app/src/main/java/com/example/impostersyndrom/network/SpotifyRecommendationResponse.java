package com.example.impostersyndrom.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents the response structure for Spotify track recommendations.
 * This class models the JSON response returned by the Spotify API's recommendation endpoint.
 * @author Roshan
 */
public class SpotifyRecommendationResponse {
    @SerializedName("tracks")
    public List<Track> tracks;

    /**
     * Represents a track in the Spotify recommendation response.
     */
    public static class Track {
        @SerializedName("id")
        public String id;

        @SerializedName("name")
        public String name;

        @SerializedName("artists")
        public List<Artist> artists;

        @SerializedName("uri")
        public String uri;

        @SerializedName("album")
        public Album album;
    }

    /**
     * Represents an artist associated with a track.
     */
    public static class Artist {
        @SerializedName("name")
        public String name;
    }

    /**
     * Represents an album associated with a track.
     */
    public static class Album {
        @SerializedName("images")
        public List<Image> images;
    }

    /**
     * Represents an image associated with an album.
     */
    public static class Image {
        @SerializedName("url")
        public String url;

        @SerializedName("height")
        public int height;

        @SerializedName("width")
        public int width;
    }
}