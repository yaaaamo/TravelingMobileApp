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
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                                String newGroupId = ref.getId();

                                db.collection("groups")
                                        .document(newGroupId)
                                        .collection("Members")
                                        .document(userId)
                                        .set(new java.util.HashMap<>());


                                Map<String, Object> data = new HashMap<>();
                                data.put("groupIds", FieldValue.arrayUnion(newGroupId));
                                db.collection("Users").document(userId)
                                        .set(data, SetOptions.merge());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadGroups() {
        if (auth.getCurrentUser() == null) return;
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("Users").document(currentUserId)
                .addSnapshotListener((userDoc, error) -> {
                    if (error != null) return;
                    if (userDoc == null || !userDoc.exists()) return;

                    List<String> groupIds = (List<String>) userDoc.get("groupIds");
                    if (groupIds == null || groupIds.isEmpty()) {
                        groupList.clear();
                        adapter.notifyDataSetChanged();
                        return;
                    }


                    List<String> safeGroupIds = groupIds.size() > 10
                            ? groupIds.subList(0, 10)
                            : groupIds;

                    db.collection("groups")
                            .whereIn(FieldPath.documentId(), safeGroupIds)
                            .get()
                            .addOnSuccessListener(value -> {
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
                            })
                            .addOnFailureListener(e -> {
                                android.util.Log.e("Groups", "loadGroups error: " + e.getMessage());
                            });
                });
    }
}