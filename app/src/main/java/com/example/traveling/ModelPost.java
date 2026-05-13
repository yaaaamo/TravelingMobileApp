package com.example.traveling;

public class ModelPost {

    private String username;
    private String caption;
    private String imageUrl;
    private String timestamp;

    public ModelPost() {
        // Needed for Firebase
    }

    public ModelPost(String username, String caption, String imageUrl, String timestamp) {
        this.username = username;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public String getCaption() {
        return caption;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}