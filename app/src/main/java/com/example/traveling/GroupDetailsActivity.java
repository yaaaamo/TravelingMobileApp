package com.example.traveling;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GroupDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<ModelPost> postList;
    private AdapterPosts adapter;
    private String groupId;
    private String currentUserId;

    // Tab views
    private Button tabPhotos, tabPaths;
    private RecyclerView groupPostsRecyclerView;
    private ScrollView pathsScrollView;
    private LinearLayout pathsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_details);

        db            = FirebaseFirestore.getInstance();
        auth          = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        groupId                 = getIntent().getStringExtra("groupId");
        String groupName        = getIntent().getStringExtra("groupName");
        String groupDescription = getIntent().getStringExtra("groupDescription");

        TextView nameView        = findViewById(R.id.groupName);
        TextView descView        = findViewById(R.id.groupDescription);
        TextView memberChip      = findViewById(R.id.memberCountChip);
        TextView postChip        = findViewById(R.id.postCountChip);
        TextView routeChip       = findViewById(R.id.routeCountChip);
        View backButton          = findViewById(R.id.backButton);
        View addPostButton       = findViewById(R.id.addPostToGroupButton);
        View manageMembersButton = findViewById(R.id.manageMembersButton);
        View quitGroupButton     = findViewById(R.id.quitGroupButton);

        // Tab views
        tabPhotos             = findViewById(R.id.tabPhotos);
        tabPaths              = findViewById(R.id.tabPaths);
        groupPostsRecyclerView = findViewById(R.id.groupPostsRecyclerView);
        pathsScrollView       = findViewById(R.id.pathsScrollView);
        pathsContainer        = findViewById(R.id.pathsContainer);

        nameView.setText(groupName);
        descView.setText(groupDescription);
        backButton.setOnClickListener(v -> finish());

        // Load counts
        if (groupId != null) {
            db.collection("groups").document(groupId)
                    .collection("Members").get()
                    .addOnSuccessListener(s -> memberChip.setText(s.size() + " members"));

            db.collection("posts").whereEqualTo("groupid", groupId).get()
                    .addOnSuccessListener(s -> postChip.setText(s.size() + " photos"));

            db.collection("groups").document(groupId)
                    .collection("sharedRoutes").get()
                    .addOnSuccessListener(s -> routeChip.setText(s.size() + " paths"));
        }

        // RecyclerView setup
        groupPostsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        postList = new ArrayList<>();
        adapter  = new AdapterPosts(postList);
        groupPostsRecyclerView.setAdapter(adapter);

        // Tab click listeners
        tabPhotos.setOnClickListener(v -> showPhotosTab());
        tabPaths.setOnClickListener(v -> showPathsTab());

        // Default: photos tab
        showPhotosTab();

        addPostButton.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.putExtra("openAddPhoto", true);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });

        if (groupId != null) {
            db.collection("groups").document(groupId).get()
                    .addOnSuccessListener(doc -> {
                        String createdBy = doc.getString("createdBy");
                        boolean isOwner  = currentUserId != null && currentUserId.equals(createdBy);
                        manageMembersButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                    });
        }

        manageMembersButton.setOnClickListener(v -> showManageMembersDialog());
        quitGroupButton.setOnClickListener(v -> showQuitConfirmDialog());

        loadGroupPosts();
        loadGroupPaths();
    }

    // ─── Tab switching ────────────────────────────────────────────────────────

    private void showPhotosTab() {
        groupPostsRecyclerView.setVisibility(View.VISIBLE);
        pathsScrollView.setVisibility(View.GONE);
        tabPhotos.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        getColor(R.color.mainpink)));
        tabPhotos.setTextColor(android.graphics.Color.WHITE);
        tabPaths.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F5E8E8")));
        tabPaths.setTextColor(getColor(R.color.mainpink));
    }

    private void showPathsTab() {
        groupPostsRecyclerView.setVisibility(View.GONE);
        pathsScrollView.setVisibility(View.VISIBLE);
        tabPaths.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        getColor(R.color.mainpink)));
        tabPaths.setTextColor(android.graphics.Color.WHITE);
        tabPhotos.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#F5E8E8")));
        tabPhotos.setTextColor(getColor(R.color.mainpink));
    }

    // ─── Load data ────────────────────────────────────────────────────────────

    private void loadGroupPosts() {
        if (groupId == null) return;

        db.collection("posts")
                .whereEqualTo("groupid", groupId)
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
                    // Pass groupId and currentUserId so the delete button appears
                    adapter = new AdapterPosts(postList, groupId, currentUserId);
                    groupPostsRecyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                });
    }

    private void loadGroupPaths() {
        if (groupId == null) return;

        db.collection("groups").document(groupId)
                .collection("sharedRoutes")
                .orderBy("sharedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    pathsContainer.removeAllViews();

                    if (value.isEmpty()) {
                        TextView empty = new TextView(this);
                        empty.setText("No paths shared yet.\nShare a route from TravelPath!");
                        empty.setTextSize(14f);
                        empty.setGravity(android.view.Gravity.CENTER);
                        empty.setPadding(0, 48, 0, 0);
                        empty.setTextColor(android.graphics.Color.parseColor("#888888"));
                        pathsContainer.addView(empty);
                        return;
                    }

                    for (DocumentSnapshot doc : value.getDocuments()) {
                        addRouteCard(doc);
                    }
                });
    }

    private void addRouteCard(DocumentSnapshot doc) {
        View card = LayoutInflater.from(this)
                .inflate(R.layout.item_group_route, pathsContainer, false);

        TextView title       = card.findViewById(R.id.routeTitle);
        TextView sharedBy    = card.findViewById(R.id.routeSharedBy);
        android.widget.ImageButton btnDeleteRoute = card.findViewById(R.id.btnDeleteRoute);
        TextView cost     = card.findViewById(R.id.routeCost);
        TextView distance = card.findViewById(R.id.routeDistance);
        TextView date     = card.findViewById(R.id.routeDate);
        TextView places   = card.findViewById(R.id.routePlaces);
        Button btnMap     = card.findViewById(R.id.btnViewRouteOnMap);

        String routeTitle = doc.getString("title");
        title.setText(null != routeTitle ? routeTitle + " Journey" : "Journey");

        String by = doc.getString("sharedByName");
        sharedBy.setText("Shared by " + (by != null ? by : "someone"));

        Double c = doc.getDouble("totalCost");
        cost.setText(c != null ? String.format("💰 %.0f €", c) : "");

        String dist = doc.getString("distance");
        distance.setText(dist != null ? "📍 " + dist : "");

        com.google.firebase.Timestamp ts = doc.getTimestamp("sharedAt");
        if (ts != null) {
            String formatted = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                    .format(ts.toDate());
            date.setText("🗓 " + formatted);
        }

        List<Map<String, Object>> placesList = (List<Map<String, Object>>) doc.get("places");
        if (placesList != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < placesList.size(); i++) {
                sb.append(i + 1).append(". ").append(placesList.get(i).get("name"));
                if (i < placesList.size() - 1) sb.append("\n");
            }
            places.setText(sb.toString());
        }

        btnMap.setOnClickListener(v -> {
            if (placesList == null || placesList.isEmpty()) {
                Toast.makeText(this, "No location data available.", Toast.LENGTH_SHORT).show();
                return;
            }
            SharedJourneyHolder.places = placesList;
            SharedJourneyHolder.title  = routeTitle;
            android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
            intent.putExtra("openMapsWithJourney", true);
            intent.putExtra("journeyTitle", routeTitle);
            startActivity(intent);
        });

        // Fix: check sharedByUid, fallback to sharedByName match
        String sharedByUid = doc.getString("sharedByUid");

        boolean isOwner = false;
        if (currentUserId != null) {
            if (sharedByUid != null) {
                isOwner = currentUserId.equals(sharedByUid);
            } else {
                // Fallback for old routes: patch the document with current user's uid if name matches
                db.collection("Users").document(currentUserId).get()
                        .addOnSuccessListener(userDoc -> {
                            String myName = userDoc.getString("fullname");
                            if (myName != null && myName.equals(by)) {
                                // Patch the missing sharedByUid field
                                doc.getReference().update("sharedByUid", currentUserId);
                                btnDeleteRoute.setVisibility(View.VISIBLE);
                                btnDeleteRoute.setOnClickListener(v -> showDeleteRouteDialog(doc, card));
                            }
                        });
            }
        }

        if (isOwner) {
            btnDeleteRoute.setVisibility(View.VISIBLE);
            btnDeleteRoute.setOnClickListener(v -> showDeleteRouteDialog(doc, card));
        }

        pathsContainer.addView(card);
    }

    private void showDeleteRouteDialog(DocumentSnapshot doc, View card) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Remove from group")
                .setMessage("Remove this path from the group?")
                .setPositiveButton("Remove", (dialog, which) ->
                        db.collection("groups")
                                .document(groupId)
                                .collection("sharedRoutes")
                                .document(doc.getId())
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    pathsContainer.removeView(card);
                                    Toast.makeText(this, "Path removed.", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─── Quit group ───────────────────────────────────────────────────────────

    private void showQuitConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quit group")
                .setMessage("Are you sure you want to leave this group?")
                .setPositiveButton("Quit", (dialog, which) -> quitGroup())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void quitGroup() {
        if (currentUserId == null || groupId == null) return;

        db.collection("groups")
                .document(groupId)
                .collection("Members")
                .document(currentUserId)
                .delete()
                .addOnSuccessListener(unused ->
                        db.collection("Users").document(currentUserId)
                                .update("groupIds", FieldValue.arrayRemove(groupId))
                                .addOnSuccessListener(u -> {
                                    Toast.makeText(this, "You left the group.",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to quit: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    // ─── Manage members ───────────────────────────────────────────────────────

    private void showManageMembersDialog() {
        if (groupId == null || isFinishing() || isDestroyed()) return;

        db.collection("groups").document(groupId)
                .collection("Members")
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) return;

                    List<String> emails = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String email = doc.getString("username");
                        if (email != null && !email.trim().isEmpty()) {
                            emails.add(email);
                        }
                    }

                    View dialogView = LayoutInflater.from(this)
                            .inflate(R.layout.dialog_manage_members, null);

                    TextView membersListView = dialogView.findViewById(R.id.membersList);
                    EditText addMemberInput  = dialogView.findViewById(R.id.addMemberInput);
                    Button addMemberButton   = dialogView.findViewById(R.id.addMemberButton);

                    updateMembersList(membersListView, emails);

                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setTitle("Manage Members")
                            .setView(dialogView)
                            .setNegativeButton("Close", null)
                            .create();

                    addMemberButton.setOnClickListener(v -> {
                        String emailInput = addMemberInput.getText().toString().trim().toLowerCase();
                        if (emailInput.isEmpty()) {
                            Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        addMemberButton.setEnabled(false);

                        db.collection("Users")
                                .whereEqualTo("fullname", emailInput)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(userSnapshot -> {
                                    if (isFinishing() || isDestroyed()) return;

                                    if (userSnapshot.isEmpty()) {
                                        Toast.makeText(this, "User not found",
                                                Toast.LENGTH_SHORT).show();
                                        addMemberButton.setEnabled(true);
                                        return;
                                    }

                                    DocumentSnapshot userDoc = userSnapshot.getDocuments().get(0);
                                    String newMemberId = userDoc.getId();
                                    String fetchedEmail = userDoc.getString("fullname");
                                    DocumentSnapshot userDoc  = userSnapshot.getDocuments().get(0);
                                    String newMemberId        = userDoc.getId();
                                    String fetchedEmail       = userDoc.getString("email");
                                    final String userEmail    = (fetchedEmail == null
                                            || fetchedEmail.trim().isEmpty())
                                            ? emailInput : fetchedEmail;

                                    java.util.HashMap<String, Object> memberData = new java.util.HashMap<>();
                                    memberData.put("email", userEmail);
                                    memberData.put("uid", newMemberId);

                                    db.collection("groups")
                                            .document(groupId)
                                            .collection("Members")
                                            .document(newMemberId)
                                            .set(memberData)
                                            .addOnSuccessListener(unused -> {
                                                db.collection("Users").document(newMemberId)
                                                        .update("groupIds",
                                                                FieldValue.arrayUnion(groupId));
                                                if (isFinishing() || isDestroyed()) return;
                                                Toast.makeText(this, userEmail + " added!",
                                                        Toast.LENGTH_SHORT).show();
                                                addMemberInput.setText("");
                                                if (!emails.contains(userEmail)) {
                                                    emails.add(userEmail);
                                                    updateMembersList(membersListView, emails);
                                                }
                                                addMemberButton.setEnabled(true);
                                            })
                                            .addOnFailureListener(e -> {
                                                if (isFinishing() || isDestroyed()) return;
                                                Toast.makeText(this, "Failed to add member",
                                                        Toast.LENGTH_SHORT).show();
                                                addMemberButton.setEnabled(true);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    Toast.makeText(this, "Failed to search user",
                                            Toast.LENGTH_SHORT).show();
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