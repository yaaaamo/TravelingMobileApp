package com.example.traveling;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;

 public class Home extends Fragment {

        private RecyclerView recyclerView;
        private ArrayList<ModelPost> postList;
        private AdapterPosts adapter;

        private FirebaseFirestore db;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View view = inflater.inflate(R.layout.fragment_home, container, false);

            db = FirebaseFirestore.getInstance();

            recyclerView = view.findViewById(R.id.postrecyclerview);
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

            postList = new ArrayList<>();
            adapter = new AdapterPosts(postList);
            recyclerView.setAdapter(adapter);

            Button add = view.findViewById(R.id.addphoto);

            add.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new AddPhoto())
                        .addToBackStack(null)
                        .commit();
            });

            loadPosts();

            return view;
        }

        private void loadPosts() {

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("posts")
                    .addSnapshotListener((value, error) -> {

                        if (error != null) {
                            Log.e("FIREBASE", error.getMessage());
                            return;
                        }

                        if (value == null) return;

                        postList.clear();

                        for (DocumentSnapshot doc : value.getDocuments()) {

                            ModelPost post = doc.toObject(ModelPost.class);

                            if (post != null) {
                                Log.d("Posts", post.getCaption());
                                postList.add(post);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    });
        }
    }