package com.example.traveling;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class Home extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<ModelPost> postList;
    private AdapterPosts adapter;

    private DatabaseReference postsRef;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.postrecyclerview);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        postList = new ArrayList<>();

        adapter = new AdapterPosts(postList);

        recyclerView.setAdapter(adapter);

        FloatingActionButton add = view.findViewById(R.id.addphoto);

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

        postsRef = FirebaseDatabase.getInstance().getReference("Posts");

        postsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                postList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {

                    ModelPost post = ds.getValue(ModelPost.class);

                    postList.add(post);
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {

            }
        });
    }
}