package com.example.traveling;

public class ModelGroupPost {
    private String postId;       // reference to original post
    private String sharedBy;     // username who shared it

    public ModelGroupPost() {}

    public ModelGroupPost(String postId, String sharedBy, String timestamp) {
        this.postId = postId;
        this.sharedBy = sharedBy;
    }

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getSharedBy() { return sharedBy; }
    public void setSharedBy(String sharedBy) { this.sharedBy = sharedBy; }
}