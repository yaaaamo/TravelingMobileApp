package com.example.traveling;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GroupDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<ModelPost> postList;
    private AdapterPosts adapter;
    private String groupId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // get group info from intent
        groupId                 = getIntent().getStringExtra("groupId");
        String groupName        = getIntent().getStringExtra("groupName");
        String groupDescription = getIntent().getStringExtra("groupDescription");

        // bind views
        TextView nameView        = findViewById(R.id.groupName);
        TextView descriptionView = findViewById(R.id.groupDescription);
        Button backButton        = findViewById(R.id.backButton);
        Button addPostButton     = findViewById(R.id.addPostToGroupButton);
        Button manageMembersButton = findViewById(R.id.manageMembersButton);
        RecyclerView recyclerView  = findViewById(R.id.groupPostsRecyclerView);

        nameView.setText(groupName);
        descriptionView.setText(groupDescription);

        // back button
        backButton.setOnClickListener(v -> finish());

        // set up posts recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        adapter  = new AdapterPosts(postList);
        recyclerView.setAdapter(adapter);

        // add post to group button — navigates to AddPhoto passing groupId
        addPostButton.setOnClickListener(v -> {
            AddPhoto addPhotoFragment = new AddPhoto();
            Bundle bundle = new Bundle();
            bundle.putString("groupId", groupId);
            addPhotoFragment.setArguments(bundle);

            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, addPhotoFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // manage members button
        manageMembersButton.setOnClickListener(v -> showManageMembersDialog());

        // load posts
        loadGroupPosts();
    }

    private void loadGroupPosts() {
        if (groupId == null) return;

        db.collection("posts")
                .whereEqualTo("groupId", groupId)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    postList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ModelPost post = doc.toObject(ModelPost.class);
                        if (post != null) {
                            post.setPostId(doc.getId());
                            postList.add(post);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showManageMembersDialog() {
        if (groupId == null) return;

        // fetch current members first
        db.collection("groups").document(groupId)
                .collection("Members")
                .get()
                .addOnSuccessListener(snapshot -> {

                    // read username field from each member document
                    List<String> usernames = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String username = doc.getString("username");
                        if (username != null) usernames.add(username);
                    }

                    // build dialog
                    View dialogView = LayoutInflater.from(this)
                            .inflate(R.layout.dialog_manage_members, null);

                    TextView membersListView = dialogView.findViewById(R.id.membersList);
                    EditText addMemberInput  = dialogView.findViewById(R.id.addMemberInput);
                    Button addMemberButton   = dialogView.findViewById(R.id.addMemberButton);

                    // show current members as a simple text list
                    if (usernames.isEmpty()) {
                        membersListView.setText("No members yet.");
                    } else {
                        membersListView.setText(String.join("\n", usernames));
                    }

                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle("Manage Members")
                            .setView(dialogView)
                            .setNegativeButton("Close", null)
                            .create();

                    // add member by userId
                    addMemberButton.setOnClickListener(v -> {
                        String newMemberInput = addMemberInput.getText().toString().trim();
                        if (newMemberInput.isEmpty()) return;

                        // look up user by username in Firestore
                        db.collection("users")
                                .whereEqualTo("username", newMemberInput)
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    if (userSnapshot.isEmpty()) {
                                        Toast.makeText(this,
                                                "User not found", Toast.LENGTH_SHORT).show();
                                        return;
                                    }

                                    String newMemberId = userSnapshot
                                            .getDocuments().get(0).getId();

                                    HashMap<String, Object> memberData = new HashMap<>();
                                    memberData.put("username", newMemberInput); // ← store username
                                    //after adding user notify them !!! TO MODIFY LATER
//                                    NotificationHelper.sendGroupNotification(
//                                            newMemberId, currentUsername, groupId, groupName);


                                    db.collection("groups")
                                            .document(groupId)
                                            .collection("Members")
                                            .document(newMemberId)
                                            .set(memberData)
                                            .addOnSuccessListener(unused -> {
                                                Toast.makeText(this,
                                                        newMemberInput + " added!",
                                                        Toast.LENGTH_SHORT).show();
                                                addMemberInput.setText("");
                                                dialog.dismiss();
                                                // reopen to refresh list
                                                showManageMembersDialog();
                                            });
                                });
                    });

                    dialog.show();
                });
    }
}