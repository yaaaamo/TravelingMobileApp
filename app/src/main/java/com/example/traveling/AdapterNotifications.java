package com.example.traveling;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdapterNotifications extends RecyclerView.Adapter<AdapterNotifications.NotificationViewHolder> {

    private List<ModelNotification> notificationList;
    private String userId;

    public AdapterNotifications(List<ModelNotification> notificationList, String userId) {
        this.notificationList = notificationList;
        this.userId = userId;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        ModelNotification notification = notificationList.get(position);
        holder.message.setText(notification.getMessage());
        holder.timestamp.setText(notification.getTimestamp());
    }

    @Override
    public int getItemCount() { return notificationList.size(); }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView message, timestamp;
        View unreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            message   = itemView.findViewById(R.id.notificationMessage);
            timestamp = itemView.findViewById(R.id.notificationTimestamp);
            unreadDot = itemView.findViewById(R.id.unreadDot);
        }
    }
}