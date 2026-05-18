package com.example.traveling;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdapterGroups extends RecyclerView.Adapter<AdapterGroups.GroupViewHolder> {

    private List<ModelGroup> groupList;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static final String[] EMOJIS = {"🏖️", "🏔️", "🌿", "🍜", "🗺️", "🏛️", "🎭", "🚂"};
    private static final String[] BG_COLORS = {
            "#FBEAF0", "#E6F1FB", "#E1F5EE", "#FAEEDA",
            "#EEEDFE", "#F1EFE8", "#FFF0F0", "#E1F5EE"
    };

    public AdapterGroups(List<ModelGroup> groupList) {
        this.groupList = groupList;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_group, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        ModelGroup group = groupList.get(position);
        holder.name.setText(group.getName());
        holder.description.setText(group.getDescription());

        // Emoji avatar cycling by position
        int idx = position % EMOJIS.length;
        holder.emoji.setText(EMOJIS[idx]);
        holder.emoji.setBackgroundColor(
                android.graphics.Color.parseColor(BG_COLORS[idx]));

        // Member count
        if (group.getGroupId() != null) {
            db.collection("groups").document(group.getGroupId())
                    .collection("Members").get()
                    .addOnSuccessListener(snap -> {
                        holder.members.setText(snap.size() + " members");
                    });

            db.collection("posts")
                    .whereEqualTo("groupid", group.getGroupId()).get()
                    .addOnSuccessListener(snap -> {
                        holder.posts.setText(snap.size() + " posts");
                    });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(),
                    GroupDetailsActivity.class);
            intent.putExtra("groupId", group.getGroupId());
            intent.putExtra("groupName", group.getName());
            intent.putExtra("groupDescription", group.getDescription());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return groupList.size(); }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView name, description, emoji, members, posts;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            name        = itemView.findViewById(R.id.groupName);
            description = itemView.findViewById(R.id.groupDescription);
            emoji       = itemView.findViewById(R.id.groupEmoji);
            members     = itemView.findViewById(R.id.groupMemberCount);
            posts       = itemView.findViewById(R.id.groupPostCount);
        }
    }
}