package com.example.traveling;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.PostViewHolder> {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final List<ModelPost> postList;
    private final List<ModelPost> postListFull;

    private String groupId;
    private String currentUserId;

    public AdapterPosts(List<ModelPost> postList) {
        this.postList = postList;
        this.postListFull = new ArrayList<>(postList);
    }

    public AdapterPosts(List<ModelPost> postList, String groupId, String currentUserId) {
        this(postList);
        this.groupId = groupId;
        this.currentUserId = currentUserId;
    }

    // ───────────────────────────── VIEW HOLDER ─────────────────────────────

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.example_home, parent, false);
        return new PostViewHolder(view);
    }

    // ───────────────────────────── BIND ─────────────────────────────

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

        ModelPost post = postList.get(position);

        bindPostData(holder, post);
        bindImages(holder, post);
        bindClicks(holder, post);
        bindDeleteButton(holder, post);
        bindLikesListener(holder, post);
    }

    // ───────────────────────────── CLEAN METHODS ─────────────────────────────

    private void bindPostData(PostViewHolder h, ModelPost p) {
        h.username.setText(p.getUsername());
        h.caption.setText(p.getCaption());
        h.location.setText(p.getLocation() + ", " + p.getCountry());
        h.likes.setText(p.getLikes() + " Likes");
        h.comments.setText(p.getComments() + " Comments");
        h.tags.setText(p.getTags());
        h.travelType.setText(p.getTravelType());
    }

    private void bindImages(PostViewHolder h, ModelPost p) {
        Glide.with(h.itemView.getContext())
                .load(p.getImageUrl())
                .into(h.image);

        Glide.with(h.itemView.getContext())
                .load(p.getProfilePicture())
                .into(h.profileImage);
    }

    private void bindClicks(PostViewHolder h, ModelPost p) {
        h.itemView.setOnClickListener(v -> openPost(h, p));
    }

    private void openPost(PostViewHolder h, ModelPost p) {
        Intent intent = new Intent(h.itemView.getContext(), PostDetailsActivity.class);

        intent.putExtra("username", p.getUsername());
        intent.putExtra("caption", p.getCaption());
        intent.putExtra("imageUrl", p.getImageUrl());
        intent.putExtra("profilePicture", p.getProfilePicture());
        intent.putExtra("location", p.getLocation());
        intent.putExtra("country", p.getCountry());
        intent.putExtra("tags", p.getTags());
        intent.putExtra("travelType", p.getTravelType());
        intent.putExtra("likes", p.getLikes());
        intent.putExtra("comments", p.getComments());
        intent.putExtra("timestamp", p.getTimestamp());
        intent.putExtra("postId", p.getPostId());
        intent.putExtra("postOwnerId", p.getuserID());
        intent.putExtra("lat", p.getLat());
        intent.putExtra("lng", p.getLng());
        intent.putExtra("googlePlaceId", p.getGooglePlaceId());
        intent.putExtra("groupId", p.getGroupid());

        h.itemView.getContext().startActivity(intent);
    }

    private void bindDeleteButton(PostViewHolder h, ModelPost p) {

        boolean isGroupContext = groupId != null;
        boolean isOwner = currentUserId != null
                && currentUserId.equals(p.getuserID());

        if (isGroupContext && isOwner) {
            h.btnDeleteFromGroup.setVisibility(View.VISIBLE);

            h.btnDeleteFromGroup.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(h.itemView.getContext())
                        .setTitle("Remove from group")
                        .setMessage("Remove this post from the group?")
                        .setPositiveButton("Remove", (d, w) -> removeFromGroup(h, p))
                        .setNegativeButton("Cancel", null)
                        .show();
            });

        } else {
            h.btnDeleteFromGroup.setVisibility(View.GONE);
        }
    }

    private void removeFromGroup(PostViewHolder h, ModelPost p) {
        if (p.getPostId() == null) return;

        db.collection("posts")
                .document(p.getPostId())
                .update("groupid", null)
                .addOnSuccessListener(unused -> {
                    int pos = h.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        postList.remove(pos);
                        postListFull.remove(p);
                        notifyItemRemoved(pos);
                    }
                });
    }

    // ⚠️ NOTE: This is still inefficient (Firestore listener per item)
    private void bindLikesListener(PostViewHolder h, ModelPost p) {
        if (p.getPostId() == null) return;

        db.collection("posts")
                .document(p.getPostId())
                .collection("Likes")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        h.likes.setText(value.size() + " Likes");
                    }
                });
    }

    // ───────────────────────────── BASIC ─────────────────────────────

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public void updateFullList(List<ModelPost> newList) {
        postListFull.clear();
        postListFull.addAll(newList);
    }

    public void filter(String query) {
        postList.clear();

        if (query.isEmpty()) {
            postList.addAll(postListFull);
        } else {
            String q = query.toLowerCase().trim();

            for (ModelPost p : postListFull) {
                if ((p.getLocation() != null && p.getLocation().toLowerCase().contains(q)) ||
                        (p.getTags() != null && p.getTags().toLowerCase().contains(q)) ||
                        (p.getTravelType() != null && p.getTravelType().toLowerCase().contains(q)) ||
                        (p.getCountry() != null && p.getCountry().toLowerCase().contains(q))) {

                    postList.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    // ───────────────────────────── VIEW HOLDER ─────────────────────────────

    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView username, caption, location, likes, comments, tags, travelType;
        ImageView image, profileImage;
        Button likeButton;
        ImageButton btnDeleteFromGroup;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.postUsername);
            caption = itemView.findViewById(R.id.postCaption);
            image = itemView.findViewById(R.id.postImage);
            profileImage = itemView.findViewById(R.id.profileImage);
            location = itemView.findViewById(R.id.postLocation);
            likes = itemView.findViewById(R.id.postLikes);
            comments = itemView.findViewById(R.id.postComments);
            tags = itemView.findViewById(R.id.postTags);
            travelType = itemView.findViewById(R.id.postTravelType);
            likeButton = itemView.findViewById(R.id.likeButton);
            btnDeleteFromGroup = itemView.findViewById(R.id.btnDeleteFromGroup);
        }
    }
}