package com.example.traveling;

public class ModelNotification {
    private String notificationId;
    private String type;
    private String message;
    private String postId;
    private String groupId;
    private String timestamp;

    public ModelNotification() {}

    public ModelNotification(String type, String message, String postId,
                             String groupId, String timestamp) {
        this.type = type;
        this.message = message;
        this.postId = postId;
        this.groupId = groupId;
        this.timestamp = timestamp;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String id) { this.notificationId = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}