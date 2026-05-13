package com.example.traveling;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.PostViewHolder> {

    private List<ModelPost> postList;

    public AdapterPosts(List<ModelPost> postList) {
        this.postList = postList;
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

        Glide.with(holder.itemView.getContext())
                .load(post.getImageUrl())
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView username, caption;
        ImageView image;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.postUsername);
            caption = itemView.findViewById(R.id.postCaption);
            image = itemView.findViewById(R.id.postImage);
        }
    }
}