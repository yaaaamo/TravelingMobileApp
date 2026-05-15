package com.example.traveling;

public class ModelComment {
    private String commentId;
    private String text;
    private String username;
    private String timestamp;

    public ModelComment() {}

    public ModelComment(String text, String username, String timestamp) {
        this.text = text;
        this.username = username;
        this.timestamp = timestamp;
    }

    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}