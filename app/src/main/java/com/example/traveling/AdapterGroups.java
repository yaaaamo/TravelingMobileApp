package com.example.traveling;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AdapterGroups extends RecyclerView.Adapter<AdapterGroups.GroupViewHolder> {

    private List<ModelGroup> groupList;

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

        /*// clicking a group opens its detail page
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(),
                    GroupDetailsActivity.class);
            intent.putExtra("groupId", group.getGroupId());
            intent.putExtra("groupName", group.getName());
            intent.putExtra("groupDescription", group.getDescription());
            holder.itemView.getContext().startActivity(intent);
        });*/
    }

    @Override
    public int getItemCount() { return groupList.size(); }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView name, description;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            name        = itemView.findViewById(R.id.groupName);
            description = itemView.findViewById(R.id.groupDescription);
        }
    }
}