package com.example.traveling;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PostDetailsActivity extends AppCompatActivity {

    ImageView imageView;
    ImageView profileImage;
    TextView captionView, username, location, country,
            tags, travelType, likes, comments, date;
    ImageButton likeButton;
    LinearLayout groupLayout;
    TextView groupName;

    // ← these now live here, not in an adapter
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String postId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // --- bind views ---
        imageView    = findViewById(R.id.imageView);
        captionView  = findViewById(R.id.caption);
        profileImage = findViewById(R.id.profileImage);
        username     = findViewById(R.id.username);
        location     = findViewById(R.id.location);
        country      = findViewById(R.id.country);
        tags         = findViewById(R.id.tags);
        travelType   = findViewById(R.id.travelType);
        likes        = findViewById(R.id.likes);
        comments     = findViewById(R.id.comments);
        date         = findViewById(R.id.date);
        likeButton   = findViewById(R.id.likeButton);
        groupLayout = findViewById(R.id.groupLayout);
        groupName = findViewById(R.id.groupName);
        EditText commentInput       = findViewById(R.id.commentInput);
        Button submitCommentButton  = findViewById(R.id.submitCommentButton);
        RecyclerView commentsRV     = findViewById(R.id.commentsRecyclerView);
        List<ModelComment> commentList = new ArrayList<>();
        AdapterComments commentAdapter = new AdapterComments(commentList);
        commentsRV.setLayoutManager(new LinearLayoutManager(this));
        commentsRV.setAdapter(commentAdapter);
        submitCommentButton.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null || postId == null) return;

            String text = commentInput.getText().toString().trim();
            if (text.isEmpty()) return;

            // get the current user's username from Firestore (or use email as fallback)
            String username = auth.getCurrentUser().getEmail();

            ModelComment comment = new ModelComment(text, username,
                    String.valueOf(System.currentTimeMillis()));
//TO ADD A NEW COMMENT
            db.collection("posts")
                    .document(postId)
                    .collection("Comments")
                    .add(comment)
                    .addOnSuccessListener(ref -> {
                        // increments the comment count on the post
                        db.collection("posts").document(postId)
                                .update("comments", FieldValue.increment(1));
                        commentInput.setText(""); // clear input
                    });
            //notify the user about the new comment
            String postOwnerId = getIntent().getStringExtra("postOwnerId");
            NotificationHelper.sendCommentNotification(postOwnerId, username, postId);
        });
        String imageUrl        = getIntent().getStringExtra("imageUrl");
        String captionText     = getIntent().getStringExtra("caption");
        String usernameText    = getIntent().getStringExtra("username");
        String profilePicUrl   = getIntent().getStringExtra("profilePicture");
        String locationText    = getIntent().getStringExtra("location");
        String countryText     = getIntent().getStringExtra("country");
        String tagsText        = getIntent().getStringExtra("tags");
        String travelTypeText  = getIntent().getStringExtra("travelType");
        String timestamp       = getIntent().getStringExtra("timestamp");
        int likesCount         = getIntent().getIntExtra("likes", 0);
        int commentsCount      = getIntent().getIntExtra("comments", 0);
        postId                 = getIntent().getStringExtra("postId");
        //google location
        // read coordinates
        double lat            = getIntent().getDoubleExtra("lat", 0);
        double lng            = getIntent().getDoubleExtra("lng", 0);
        String googlePlaceId  = getIntent().getStringExtra("googlePlaceId");
        String groupId = getIntent().getStringExtra("groupId");
        if (groupId != null && !groupId.isEmpty()) {

            db.collection("groups")
                    .document(groupId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {

                        if (documentSnapshot.exists()) {

                            String groupTitle = documentSnapshot.getString("name");

                            groupLayout.setVisibility(View.VISIBLE);
                            groupName.setText("Posted in " + groupTitle);
                        }
                    });
        }

// bind and wire directions button
        Button directionsButton = findViewById(R.id.coordinates);

        if (lat == 0 && lng == 0) {
            // if no coordinates available :
            directionsButton.setText("No coordinates available");
        } else {
            directionsButton.setVisibility(View.VISIBLE);
            directionsButton.setText(" " + locationText + "\n" + String.format("%.4f, %.4f", lat, lng));
            directionsButton.setOnClickListener(v -> {
                // open Google Maps with coordinates
                Uri gmmIntentUri = Uri.parse(
                        "google.navigation:q=" + lat + "," + lng);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                // fallback to browser if Google Maps not installed
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Uri browserUri = Uri.parse(
                            "https://www.google.com/maps/dir/?api=1&destination="
                                    + lat + "," + lng);
                    startActivity(new Intent(Intent.ACTION_VIEW, browserUri));
                }
            });
        }

        //TO SHOW EXISTENT COMMENTS!
        db.collection("posts")
                .document(postId)
                .collection("Comments")
                .addSnapshotListener((value, error) -> {
                    if (error != null) { Log.e("COMMENTS", error.getMessage()); return; }
                    if (value == null) { Log.d("COMMENTS", "value is null"); return; }

                    Log.d("COMMENTS", "got " + value.size() + " comments"); // ← add this

                    commentList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ModelComment comment = doc.toObject(ModelComment.class);
                        if (comment != null) {
                            comment.setCommentId(doc.getId());
                            commentList.add(comment);
                        }
                    }
                    commentAdapter.notifyDataSetChanged();
                });        // --- get data from intent ---

        // --- populate views ---
        captionView.setText(captionText);
        username.setText(usernameText);
        location.setText(locationText);
        country.setText(countryText);
        tags.setText(tagsText);
        travelType.setText("Travel Type: " + travelTypeText);
        likes.setText(likesCount + " Likes");
        comments.setText(commentsCount + " Comments");
        date.setText(timestamp);

        Glide.with(this).load(imageUrl).into(imageView);
        Glide.with(this).load(profilePicUrl).into(profileImage);

        likeButton.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Log.d("NULLVALUE", "currentUser is null");
                return;
            }
            if (postId == null) {
                Log.d("NULLVALUE", "postId is null");
                return;
            }

            String currentUserId = auth.getCurrentUser().getUid();

            DocumentReference likeRef = db
                    .collection("posts")
                    .document(postId)
                    .collection("Likes")
                    .document(currentUserId);

            likeRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    likeRef.delete();
                    db.collection("posts").document(postId)
                            .update("likes", FieldValue.increment(-1));

                    Log.d("LIKES", "removed a like!");
                    likeButton.setImageResource(
                            R.drawable.likebutton
                    );

                } else {
                    likeRef.set(new HashMap<>());
                    // after likeRef.set(new HashMap<>()) — when liking:
                    String postOwnerId = getIntent().getStringExtra("postOwnerId");
                    String currentUsername = auth.getCurrentUser().getEmail(); //!! TO CHANGE WITH USERNAME LATER!
                    NotificationHelper.sendLikeNotification(postOwnerId, currentUsername, postId);
                    Log.d("notifications", "sent notif to user: "+ postOwnerId + " " + currentUsername);
                    db.collection("posts").document(postId)
                            .update("likes", FieldValue.increment(1));
                    Log.d("LIKES", "added a like!");
                    likeButton.setImageResource(
                            R.drawable.likedd_button
                    );
                }
            });
        });

        if (postId != null) {
            db.collection("posts")
                    .document(postId)
                    .collection("Likes")
                    .addSnapshotListener((value, error) -> {
                        if (value != null) {
                            likes.setText(value.size() + " Likes");
                            Log.d("LIKES", "like count"+value.size());

                            // update button text based on whether current user liked
                            if (auth.getCurrentUser() != null) {
                                String uid = auth.getCurrentUser().getUid();
                                boolean liked = value.getDocuments()
                                        .stream()
                                        .anyMatch(d -> d.getId().equals(uid));
                            }
                        }
                    });

        }
        Button backarrow = findViewById(R.id.backarrow);
        backarrow.setOnClickListener(l -> onBackPressed());

    }
}