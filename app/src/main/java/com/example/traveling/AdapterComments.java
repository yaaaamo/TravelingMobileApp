package com.example.traveling;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdapterComments extends RecyclerView.Adapter<AdapterComments.CommentViewHolder> {

    private List<ModelComment> commentList;

    public AdapterComments(List<ModelComment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        ModelComment comment = commentList.get(position);
        holder.username.setText(comment.getUsername());
        holder.text.setText(comment.getText());
    }

    @Override
    public int getItemCount() { return commentList.size(); }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView username, text;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.commentUsername);
            text     = itemView.findViewById(R.id.commentText);
        }
    }
}