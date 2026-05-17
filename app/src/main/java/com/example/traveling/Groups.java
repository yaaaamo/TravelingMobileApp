package com.example.traveling;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Groups extends Fragment {

    private RecyclerView recyclerView;
    private List<ModelGroup> groupList;
    private AdapterGroups adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_groups, container, false);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.groupsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        groupList = new ArrayList<>();
        adapter   = new AdapterGroups(groupList);
        recyclerView.setAdapter(adapter);

        Button createGroupButton = view.findViewById(R.id.createGroupButton);
        createGroupButton.setOnClickListener(v -> showCreateGroupDialog());

        loadGroups();

        return view;
    }

    private void showCreateGroupDialog() {
        // simple dialog with name + description inputs
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_create_group, null);

        EditText nameInput        = dialogView.findViewById(R.id.groupNameInput);
        EditText descriptionInput = dialogView.findViewById(R.id.groupDescriptionInput);

        new AlertDialog.Builder(getContext())
                .setTitle("Create Group")
                .setView(dialogView)
                .setPositiveButton("Create", (dialog, which) -> {
                    String name        = nameInput.getText().toString().trim();
                    String description = descriptionInput.getText().toString().trim();

                    if (name.isEmpty()) return;

                    if (auth.getCurrentUser() == null) return;

                    String userId    = auth.getCurrentUser().getUid();
                    String username = auth.getCurrentUser().getDisplayName();
                    String timestamp = String.valueOf(System.currentTimeMillis());

                    ModelGroup group = new ModelGroup(name, description, userId, timestamp);

                    // save group to Firestore
                    db.collection("groups")
                            .add(group)
                            .addOnSuccessListener(ref -> {
                                // add creator as first member
                                db.collection("groups")
                                        .document(ref.getId())
                                        .collection("Members")
                                        .document(userId)
                                        .set(new java.util.HashMap<>());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadGroups() {
        db.collection("groups")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    groupList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ModelGroup group = doc.toObject(ModelGroup.class);
                        if (group != null) {
                            group.setGroupId(doc.getId());
                            groupList.add(group);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}