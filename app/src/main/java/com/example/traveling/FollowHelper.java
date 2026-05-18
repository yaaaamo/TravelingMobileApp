package com.example.traveling;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FollowHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Follow a user.
     * - Adds currentUserId to targetUserId's "followers" subcollection
     * - Adds targetUserId to currentUser's "following" subcollection
     * - Sends a follow notification to the target user
     *
     * @param currentUserId  the UID of the person clicking Follow
     * @param currentUsername display name / email of the current user
     * @param targetUserId   the UID of the profile being followed
     */
    public static void followUser(String currentUserId,
                                  String currentUsername,
                                  String targetUserId) {
        if (currentUserId == null || targetUserId == null) return;
        if (currentUserId.equals(targetUserId)) return; // can't follow yourself

        Map<String, Object> data = new HashMap<>();
        data.put("username", currentUsername);
        data.put("timestamp", String.valueOf(System.currentTimeMillis()));

        // add currentUser to target's followers
        db.collection("Users").document(targetUserId)
                .collection("followers")
                .document(currentUserId)
                .set(data);

        // add target to currentUser's following
        db.collection("Users").document(currentUserId)
                .collection("following")
                .document(targetUserId)
                .set(data);

        // notify the target user
        NotificationHelper.sendFollowNotification(targetUserId, currentUsername);
    }

    /**
     * Unfollow a user.
     * Removes the follow documents from both sides.
     */
    public static void unfollowUser(String currentUserId, String targetUserId) {
        if (currentUserId == null || targetUserId == null) return;

        db.collection("Users").document(targetUserId)
                .collection("followers")
                .document(currentUserId)
                .delete();

        db.collection("Users").document(currentUserId)
                .collection("following")
                .document(targetUserId)
                .delete();
    }

    /**
     * Check if currentUser already follows targetUser, then invoke the callback.
     * Use this to set the initial state of a Follow/Unfollow button.
     */
    public interface FollowCheckCallback {
        void onResult(boolean isFollowing);
    }

    public static void isFollowing(String currentUserId,
                                   String targetUserId,
                                   FollowCheckCallback callback) {
        if (currentUserId == null || targetUserId == null) {
            callback.onResult(false);
            return;
        }
        db.collection("Users").document(targetUserId)
                .collection("followers")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(doc -> callback.onResult(doc.exists()));
    }

    /**
     * When a followed user creates a new post, call this to fan-out
     * a "new post" notification to all their followers.
     *
     * @param authorId       UID of the user who just posted
     * @param authorUsername display name of the author
     * @param postId         the new post's document ID
     */
    public static void notifyFollowersOfNewPost(String authorId,
                                                String authorUsername,
                                                String postId) {
        if (authorId == null) return;

        db.collection("Users").document(authorId)
                .collection("followers")
                .get()
                .addOnSuccessListener(followers -> {
                    for (var doc : followers.getDocuments()) {
                        String followerId = doc.getId();
                        NotificationHelper.sendNewPostNotification(
                                followerId, authorUsername, postId);
                    }
                });
    }
}