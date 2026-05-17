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
import com.google.firebase.firestore.FieldValue;
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
        if (groupId == null || isFinishing() || isDestroyed()) return;

        db.collection("groups").document(groupId)
                .collection("Members")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    List<String> emails = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String email = doc.getString("email");
                        if (email != null && !email.trim().isEmpty()) {
                            emails.add(email);
                        }
                    }

                    View dialogView = LayoutInflater.from(this)
                            .inflate(R.layout.dialog_manage_members, null);

                    TextView membersListView = dialogView.findViewById(R.id.membersList);
                    EditText addMemberInput = dialogView.findViewById(R.id.addMemberInput);
                    Button addMemberButton = dialogView.findViewById(R.id.addMemberButton);

                    updateMembersList(membersListView, emails);

                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle("Manage Members")
                            .setView(dialogView)
                            .setNegativeButton("Close", null)
                            .create();

                    addMemberButton.setOnClickListener(v -> {
                        String emailInput = addMemberInput.getText().toString().trim().toLowerCase();

                        if (emailInput.isEmpty()) {
                            Toast.makeText(this, "Enter an email", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        addMemberButton.setEnabled(false);

                        db.collection("Users")
                                .whereEqualTo("email", emailInput)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    if (isFinishing() || isDestroyed()) return;

                                    if (userSnapshot.isEmpty()) {
                                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                                        addMemberButton.setEnabled(true);
                                        return;
                                    }

                                    DocumentSnapshot userDoc = userSnapshot.getDocuments().get(0);
                                    String newMemberId = userDoc.getId();
                                    String fetchedEmail = userDoc.getString("email");

                                    final String userEmail;
                                    if (fetchedEmail == null || fetchedEmail.trim().isEmpty()) {
                                        userEmail = emailInput;
                                    } else {
                                        userEmail = fetchedEmail;
                                    }

                                    HashMap<String, Object> memberData = new HashMap<>();
                                    memberData.put("email", userEmail);
                                    memberData.put("uid", newMemberId);

                                    db.collection("groups")
                                            .document(groupId)
                                            .collection("Members")
                                            .document(newMemberId)
                                            .set(memberData)
                                            .addOnSuccessListener(unused -> {
                                                db.collection("Users").document(newMemberId)
                                                        .update("groupIds", FieldValue.arrayUnion(groupId));

                                                if (isFinishing() || isDestroyed()) return;

                                                Toast.makeText(this, userEmail + " added!", Toast.LENGTH_SHORT).show();

                                                addMemberInput.setText("");

                                                if (!emails.contains(userEmail)) {
                                                    emails.add(userEmail);
                                                    updateMembersList(membersListView, emails);
                                                }

                                                addMemberButton.setEnabled(true);
                                            })
                                            .addOnFailureListener(e -> {
                                                if (isFinishing() || isDestroyed()) return;

                                                Toast.makeText(this, "Failed to add member", Toast.LENGTH_SHORT).show();
                                                addMemberButton.setEnabled(true);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    if (isFinishing() || isDestroyed()) return;

                                    Toast.makeText(this, "Failed to search user", Toast.LENGTH_SHORT).show();
                                    addMemberButton.setEnabled(true);
                                });
                    });

                    dialog.show();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Failed to load members", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateMembersList(TextView membersListView, List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            membersListView.setText("No members yet.");
        } else {
            membersListView.setText(String.join("\n", emails));
        }
    }
}