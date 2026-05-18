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
import android.widget.SearchView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;

public class Home extends Fragment {

     private RecyclerView recyclerView;
     private ArrayList<ModelPost> postList;
     private AdapterPosts adapter;

     private FirebaseFirestore db;
     private FirebaseAuth auth = FirebaseAuth.getInstance();

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
         SearchView searchView = view.findViewById(R.id.searchView);
         searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

             @Override
             public boolean onQueryTextSubmit(String query) {
                 adapter.filter(query);
                 return false;
             }

             @Override
             public boolean onQueryTextChange(String newText) {
                 adapter.filter(newText); // filters as you type
                 return false;
             }
         });
         Button add = view.findViewById(R.id.addphoto);

         add.setOnClickListener(v -> {
             requireActivity().getSupportFragmentManager()
                     .beginTransaction()
                     .replace(R.id.fragment_container, new Add_fragment())
                     .addToBackStack(null)
                     .commit();
         });
         Button notifs = view.findViewById(R.id.notifs);
         notifs.setOnClickListener(l->{
             requireActivity().getSupportFragmentManager()
                     .beginTransaction()
                     .replace(R.id.fragment_container, new Notifications())
                     .addToBackStack(null)
                     .commit();

         });

         loadPosts();

         return view;
     }

     private void loadPosts() {
         if (auth == null) auth = FirebaseAuth.getInstance();
         if (auth.getCurrentUser() == null) return;

         String currentUserId = auth.getCurrentUser().getUid();

         // step 1 — get user's group memberships
         db.collection("Users").document(currentUserId).get()
                 .addOnSuccessListener(userDoc -> {
                     List<String> memberGroupIds = new ArrayList<>();

                     if (userDoc.exists()) {
                         List<String> ids = (List<String>) userDoc.get("groupIds");
                         if (ids != null) memberGroupIds.addAll(ids);
                     }

                     // step 2 — load public posts
                     db.collection("posts")
                             .whereEqualTo("groupid", null)
                             .addSnapshotListener((value, error) -> {
                                 if (error != null) { Log.e("FIREBASE", error.getMessage()); return; }
                                 if (value == null) return;

                                 postList.clear();

                                 for (DocumentSnapshot doc : value.getDocuments()) {
                                     ModelPost post = doc.toObject(ModelPost.class);
                                     if (post != null) {
                                         post.setPostId(doc.getId());
                                         postList.add(post);
                                     }
                                 }

                                 // step 3 — also load group posts for each group user belongs to
                                 if (memberGroupIds.isEmpty()) {
                                     adapter.updateFullList(postList);
                                     adapter.notifyDataSetChanged();
                                 } else {
                                     loadGroupPostsForMember(memberGroupIds, 0);
                                 }
                             });
                 });
     }

     private void loadGroupPostsForMember(List<String> groupIds, int index) {
         if (index >= groupIds.size()) {
             // all group posts loaded — update adapter
             adapter.updateFullList(postList);
             adapter.notifyDataSetChanged();
             return;
         }

         String groupId = groupIds.get(index);

         db.collection("posts")
                 .whereEqualTo("groupid", groupId)
                 .get()
                 .addOnSuccessListener(snapshot -> {
                     for (DocumentSnapshot doc : snapshot.getDocuments()) {
                         ModelPost post = doc.toObject(ModelPost.class);
                         if (post != null) {
                             post.setPostId(doc.getId());
                             postList.add(post);
                         }
                     }
                     // recurse to next group
                     loadGroupPostsForMember(groupIds, index + 1);
                 });
     } }