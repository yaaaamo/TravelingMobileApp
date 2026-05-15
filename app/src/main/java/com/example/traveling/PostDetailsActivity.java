package com.example.traveling;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class PostDetailsActivity extends AppCompatActivity {

    ImageView imageView;
    ImageView profileImage;
    TextView captionView, username, location, country,
            tags, travelType, likes, comments, date;
    Button likeButton;

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

        // --- get data from intent ---
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
        postId                 = getIntent().getStringExtra("postId"); // ← read postId from intent

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
                } else {
                    likeRef.set(new HashMap<>());
                    db.collection("posts").document(postId)
                            .update("likes", FieldValue.increment(1));
                    Log.d("LIKES", "added a like!");
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
                                likeButton.setText(liked ? "Liked" : "Like");
                            }
                        }
                    });
        }
    }
}