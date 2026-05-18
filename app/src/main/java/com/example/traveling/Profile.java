package com.example.traveling;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Profile extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();

        TextView name = view.findViewById(R.id.nametv);
        TextView email = view.findViewById(R.id.emailtv);
        TextView postsCount = view.findViewById(R.id.text_posts_count);
        TextView journeysCount = view.findViewById(R.id.text_journeys_count);
        TextView likedCount = view.findViewById(R.id.text_liked_count);

        if (user != null) {
            String uid = user.getUid();

            if (user.isAnonymous()) {
                name.setText("Guest 👋");
                email.setText("guest@traveling.com");
            } else {
                db.collection("Users").document(uid)
                        .addSnapshotListener((snapshot, error) -> {
                            if (snapshot == null) return;
                            name.setText(snapshot.getString("fullname"));
                            email.setText(snapshot.getString("email"));
                        });
            }

            // Posts count — TravelShare photos shared by this user
            db.collection("posts")
                    .whereEqualTo("userID", uid)
                    .get()
                    .addOnSuccessListener(snap ->
                            postsCount.setText(String.valueOf(snap.size())));

            // Saved journeys count
            db.collection("Users").document(uid)
                    .collection("savedJourneys")
                    .get()
                    .addOnSuccessListener(snap ->
                            journeysCount.setText(String.valueOf(snap.size())));

            // Liked journeys count
            db.collection("Users").document(uid)
                    .collection("savedJourneys")
                    .whereEqualTo("liked", true)
                    .get()
                    .addOnSuccessListener(snap ->
                            likedCount.setText(String.valueOf(snap.size())));
        }

        Button btnSavedJourneys = view.findViewById(R.id.btn_saved_journeys);
        btnSavedJourneys.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SavedJourneysFragment())
                        .addToBackStack(null)
                        .commit()
        );

        Button btnLikedJourneys = view.findViewById(R.id.btn_liked_journeys);
        btnLikedJourneys.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new LikedJourneysFragment())
                        .addToBackStack(null)
                        .commit()
        );

        Button btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnEditProfile.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new EditProfileFragment())
                        .addToBackStack(null)
                        .commit()
        );

        Button btnLogout = view.findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            firebaseAuth.signOut();
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new Profile())
                    .commit();
            startActivity(new android.content.Intent(
                    requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        ImageView avatarView = view.findViewById(R.id.avatartv);

        if (user != null && !user.isAnonymous()) {
            db.collection("Users").document(user.getUid())
                    .addSnapshotListener((snapshot, error) -> {
                        if (snapshot == null) return;
                        name.setText(snapshot.getString("fullname"));
                        email.setText(snapshot.getString("email"));

                        String photoUrl = snapshot.getString("profilePicture");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .into(avatarView);
                        }
                    });
        }

        return view;
    }
}