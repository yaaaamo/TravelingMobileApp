package com.example.traveling;

public class ModelPost {

    private String postId;
    private String username;
    private String caption;
    private String imageUrl;
    private String timestamp;

    private int likes;
    private int comments;

    private String profilePicture;
    private String location;
    private String country;
    private String tags;
    private int savedBy;

    private String travelType;
    private String groupid ;
    private String userID;

    public ModelPost() {
    }

    public ModelPost(String username, String caption, String imageUrl,
                     String timestamp, int likes, int comments,
                     String profilePicture, String location,
                     String country, String tags,
                     int savedBy, String travelType, String groupid, String userID) {

        this.username = username;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.likes = likes;
        this.comments = comments;
        this.profilePicture = profilePicture;
        this.location = location;
        this.country = country;
        this.tags = tags;
        this.savedBy = savedBy;
        this.travelType = travelType;
        this.groupid = groupid;
        this.userID = userID;
    }
    public String getPostId() {
        return postId;
    }
    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public int getSavedBy() {
        return savedBy;
    }

    public void setSavedBy(int savedBy) {
        this.savedBy = savedBy;
    }

    public String getTravelType() {
        return travelType;
    }

    public void setTravelType(String travelType) {
        this.travelType = travelType;
    }
    public String getGroupid(){return this.groupid; }
    public void setGroupid(String groupid){ this.groupid = groupid; }
    public String getuserID(){return userID;}
    public void setUserID(String userID){this.userID= userID ; }
}