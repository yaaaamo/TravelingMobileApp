package com.example.traveling;

import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // call this when someone likes a post
    public static void sendLikeNotification(String postOwnerId,
                                            String fromUsername,
                                            String postId) {
        if (postOwnerId == null || fromUsername == null) return;

        String message = fromUsername + " liked your post";
        ModelNotification notification = new ModelNotification(
                "like", message, postId, null,
                String.valueOf(System.currentTimeMillis()));

        db.collection("Users")
                .document(postOwnerId)
                .collection("notifications")
                .add(notification);
    }

    // call this when someone comments on a post
    public static void sendCommentNotification(String postOwnerId,
                                               String fromUsername,
                                               String postId) {
        if (postOwnerId == null || fromUsername == null) return;

        String message = fromUsername + " commented on your post";
        ModelNotification notification = new ModelNotification(
                "comment", message, postId, null,
                String.valueOf(System.currentTimeMillis()));

        db.collection("Users")
                .document(postOwnerId)
                .collection("notifications")
                .add(notification);
    }

    // call this when someone adds a user to a group
    public static void sendGroupNotification(String addedUserId,
                                             String fromUsername,
                                             String groupId,
                                             String groupName) {
        if (addedUserId == null || fromUsername == null) return;

        String message = fromUsername + " added you to " + groupName;
        ModelNotification notification = new ModelNotification(
                "group", message, null, groupId,
                String.valueOf(System.currentTimeMillis()));

        db.collection("Users")
                .document(addedUserId)
                .collection("notifications")
                .add(notification);
    }
    public static void sendNewPostNotification(String followerId,
                                               String fromUsername,
                                               String postId) {
        if (followerId == null || fromUsername == null) return;

        String message = fromUsername + " shared a new post";
        ModelNotification notification = new ModelNotification(
                "new_post", message, postId, null,
                String.valueOf(System.currentTimeMillis()));

        db.collection("Users")
                .document(followerId)
                .collection("notifications")
                .add(notification);
    }
    public static void sendFollowNotification(String targetUserId,
                                              String fromUsername) {
        if (targetUserId == null || fromUsername == null) return;

        String message = fromUsername + " started following you";
        ModelNotification notification = new ModelNotification(
                "follow", message, null, null,
                String.valueOf(System.currentTimeMillis()));

        db.collection("Users")
                .document(targetUserId)
                .collection("notifications")
                .add(notification);
    }


}