package com.example.traveling;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class Notifications extends Fragment {

    private RecyclerView recyclerView;
    private List<ModelNotification> notificationList;
    private AdapterNotifications adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        notificationList = new ArrayList<>();
        String userId = auth.getCurrentUser().getUid();
        Log.d("notifications", "the notifications we are loading are for user: "+ userId);
        adapter = new AdapterNotifications(notificationList, userId);
        recyclerView.setAdapter(adapter);

        loadNotifications(userId);

        return view;
    }

    private void loadNotifications(String userId) {
        db.collection("Users")
                .document(userId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;

                    notificationList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        ModelNotification notification =
                                doc.toObject(ModelNotification.class);
                        if (notification != null) {
                            notification.setNotificationId(doc.getId());
                            notificationList.add(notification);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}