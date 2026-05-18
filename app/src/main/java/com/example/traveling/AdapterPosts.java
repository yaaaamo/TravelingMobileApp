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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.PostViewHolder> {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<ModelPost> postList;
    private List<ModelPost> postListFull;

    // Group context — set these to enable the delete-from-group button
    private String groupId      = null;
    private String currentUserId = null;

    /** Normal constructor — used in Home feed */
    public AdapterPosts(List<ModelPost> postList) {
        this.postList     = postList;
        this.postListFull = new ArrayList<>(postList);
        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    /** Group constructor — shows delete button for own posts */
    public AdapterPosts(List<ModelPost> postList, String groupId, String currentUserId) {
        this(postList);
        this.groupId       = groupId;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.example_home, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        ModelPost post = postList.get(position);

        holder.username.setText(post.getUsername());
        holder.caption.setText(post.getCaption());
        holder.location.setText(post.getLocation() + ", " + post.getCountry());
        holder.likes.setText(post.getLikes() + " Likes");
        holder.comments.setText(post.getComments() + " Comments");
        holder.tags.setText(post.getTags());
        holder.travelType.setText(post.getTravelType());

        Glide.with(holder.itemView.getContext())
                .load(post.getImageUrl())
                .into(holder.image);

            Intent intent = new Intent(holder.itemView.getContext(),
                    PostDetailsActivity.class);

            intent.putExtra("username", post.getUsername());
            intent.putExtra("caption", post.getCaption());
            intent.putExtra("imageUrl", post.getImageUrl());

            intent.putExtra("profilePicture", post.getProfilePicture());

            intent.putExtra("location", post.getLocation());
            intent.putExtra("country", post.getCountry());

            intent.putExtra("tags", post.getTags());

            intent.putExtra("travelType", post.getTravelType());

            intent.putExtra("likes", post.getLikes());

            intent.putExtra("comments", post.getComments());

            intent.putExtra("timestamp", post.getTimestamp());
            intent.putExtra("postId", post.getPostId());
            // AdapterPosts.java — add to intent
            intent.putExtra("postOwnerId", post.getuserID());
            intent.putExtra("lat", post.getLat());
            intent.putExtra("lng", post.getLng());
            intent.putExtra("googlePlaceId", post.getGooglePlaceId());
            intent.putExtra("groupId", post.getGroupid());


            holder.itemView.getContext().startActivity(intent);        
        Glide.with(holder.itemView.getContext())
                .load(post.getProfilePicture())
                .into(holder.profileImage);

        // Delete from group button
        boolean isGroupContext = groupId != null;
        boolean isOwner = currentUserId != null
                && currentUserId.equals(post.getuserID());

        if (isGroupContext && isOwner) {
            holder.btnDeleteFromGroup.setVisibility(View.VISIBLE);
            holder.btnDeleteFromGroup.setOnClickListener(v -> {
                // Show confirmation dialog
                new android.app.AlertDialog.Builder(holder.itemView.getContext())
                        .setTitle("Remove from group")
                        .setMessage("Remove this post from the group? It will still be visible on your profile.")
                        .setPositiveButton("Remove", (dialog, which) -> {
                            if (post.getPostId() == null) return;
                            // Set groupid to null → post leaves the group, stays public
                            db.collection("posts")
                                    .document(post.getPostId())
                                    .update("groupid", null)
                                    .addOnSuccessListener(unused -> {
                                        int pos = holder.getAdapterPosition();
                                        if (pos != RecyclerView.NO_ID) {
                                            postList.remove(pos);
                                            postListFull.remove(post);
                                            notifyItemRemoved(pos);
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            android.widget.Toast.makeText(
                                                    holder.itemView.getContext(),
                                                    "Failed: " + e.getMessage(),
                                                    android.widget.Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        } else {
            holder.btnDeleteFromGroup.setVisibility(View.GONE);
        }

        // Open post details on click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(),
                    PostDetailsActivity.class);
            intent.putExtra("username",     post.getUsername());
            intent.putExtra("caption",      post.getCaption());
            intent.putExtra("imageUrl",     post.getImageUrl());
            intent.putExtra("profilePicture", post.getProfilePicture());
            intent.putExtra("location",     post.getLocation());
            intent.putExtra("country",      post.getCountry());
            intent.putExtra("tags",         post.getTags());
            intent.putExtra("travelType",   post.getTravelType());
            intent.putExtra("likes",        post.getLikes());
            intent.putExtra("comments",     post.getComments());
            intent.putExtra("timestamp",    post.getTimestamp());
            intent.putExtra("postId",       post.getPostId());
            intent.putExtra("postOwnerId",  post.getuserID());
            intent.putExtra("lat",          post.getLat());
            intent.putExtra("lng",          post.getLng());
            intent.putExtra("googlePlaceId", post.getGooglePlaceId());
            holder.itemView.getContext().startActivity(intent);
        });

        // Live like count
        if (post.getPostId() != null) {
            db.collection("posts")
                    .document(post.getPostId())
                    .collection("Likes")
                    .addSnapshotListener((value, error) -> {
                        if (value != null) {
                            holder.likes.setText(value.size() + " Likes");
                        }
                    });
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // ── Search filter ─────────────────────────────────────────────────────────

    public void filter(String query) {
        postList.clear();
        if (query.isEmpty()) {
            postList.addAll(postListFull);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (ModelPost post : postListFull) {
                if ((post.getLocation()   != null && post.getLocation().toLowerCase().contains(lowerQuery)) ||
                        (post.getTags()       != null && post.getTags().toLowerCase().contains(lowerQuery))     ||
                        (post.getTravelType() != null && post.getTravelType().toLowerCase().contains(lowerQuery)) ||
                        (post.getCountry()    != null && post.getCountry().toLowerCase().contains(lowerQuery))) {
                    postList.add(post);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void updateFullList(List<ModelPost> newList) {
        postListFull.clear();
        postListFull.addAll(newList);
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView   username, caption, location, likes, comments, tags, travelType;
        ImageView  image, profileImage;
        Button     likeButton;
        ImageButton btnDeleteFromGroup;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            username            = itemView.findViewById(R.id.postUsername);
            caption             = itemView.findViewById(R.id.postCaption);
            image               = itemView.findViewById(R.id.postImage);
            profileImage        = itemView.findViewById(R.id.profileImage);
            location            = itemView.findViewById(R.id.postLocation);
            likes               = itemView.findViewById(R.id.postLikes);
            comments            = itemView.findViewById(R.id.postComments);
            tags                = itemView.findViewById(R.id.postTags);
            travelType          = itemView.findViewById(R.id.postTravelType);
            likeButton          = itemView.findViewById(R.id.likeButton);
            btnDeleteFromGroup  = itemView.findViewById(R.id.btnDeleteFromGroup);
        }
    }
}