package com.example.traveling;


import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.PostViewHolder> {

    private FirebaseFirestore db;

    private FirebaseAuth auth;
    private List<ModelPost> postList;       // filtered list (what's displayed)
    private List<ModelPost> postListFull;   // full original list

    public AdapterPosts(List<ModelPost> postList) {
        this.postList = postList;
        db = FirebaseFirestore.getInstance();

        auth = FirebaseAuth.getInstance();
        //for search bar

        this.postList = postList;
        this.postListFull = new ArrayList<>(postList); // keep a copy
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

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
        holder.itemView.setOnClickListener(v -> {

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


            holder.itemView.getContext().startActivity(intent);        });
        Glide.with(holder.itemView.getContext())
                .load(post.getProfilePicture())
                .into(holder.profileImage);

//            holder.likeButton.setOnClickListener(v -> {
//                if (auth.getCurrentUser() == null) {Log.d("NULLVALUE", "oops currentUser is null"); return;}
//
//                String currentUserId = auth.getCurrentUser().getUid();
//                String postId = post.getPostId();
//
//                if (postId == null) {Log.d("NULLVALUE", "oops postID is null") ; return;}
//
//                DocumentReference likeRef = db
//                        .collection("posts")
//                        .document(postId)
//                        .collection("Likes")
//                        .document(currentUserId);
//
//                likeRef.get().addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        likeRef.delete();
//                        db.collection("posts").document(postId)
//                                .update("likes", FieldValue.increment(-1));  // unlike
//                        Log.d("LIKES", "removed a like!");
//                    } else {
//                        likeRef.set(new HashMap<>());
//                        // increment the likes count on the post
//                        db.collection("posts").document(postId)
//                                .update("likes", FieldValue.increment(1));
//                        Log.d("LIKES", "added a like!");
//
//                    }
//                });
//            });

        if (post.getPostId() != null) {
            db.collection("posts")               // ← match exact case you use when writing
                    .document(post.getPostId())      // ← use actual postId, not .document()
                    .collection("Likes")
                    .addSnapshotListener((value, error) -> {
                        if (value != null) {
                            holder.likes.setText(value.size() + " Likes");
                        }
                    });
        }
        //idk if i should have both of them???
//        String currentUserId = FirebaseAuth.getInstance()
//                .getCurrentUser()
//                .getUid();
//        db.collection("Posts")
//                .document(post.getPostId())
//                .collection("Likes")
//                .document(currentUserId)
//                .addSnapshotListener((value, error) -> {
//
//                    if(value != null && value.exists()) {
//
//                        holder.likeButton.setText("Liked");
//
//                    } else {
//
//                        holder.likeButton.setText("Like");
//                    }
//                });

    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView username, caption;
        ImageView image;
        TextView location, likes, comments, tags, travelType;
        ImageView profileImage;
        Button likeButton;


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

        }
    }
    public void filter(String query) {
        postList.clear();

        if (query.isEmpty()) {
            postList.addAll(postListFull); // show everything if search is empty
        } else {
            String lowerQuery = query.toLowerCase().trim();

            for (ModelPost post : postListFull) {
                // search by location, tags, or travel type
                if ((post.getLocation() != null && post.getLocation().toLowerCase().contains(lowerQuery)) ||
                        (post.getTags() != null && post.getTags().toLowerCase().contains(lowerQuery)) ||
                        (post.getTravelType() != null && post.getTravelType().toLowerCase().contains(lowerQuery)) ||
                        (post.getCountry() != null && post.getCountry().toLowerCase().contains(lowerQuery))) {

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

}