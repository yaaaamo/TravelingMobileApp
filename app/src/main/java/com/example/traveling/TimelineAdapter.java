package com.example.traveling;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class TimelineAdapter
        extends RecyclerView.Adapter<TimelineAdapter.TimelineViewHolder> {

    private static final String API_KEY = "AIzaSyDxCfUdpFdYXDoVqk91QhWDeRqf2XTOP8c";
    private static final String[] TIME_LABELS = {"MORNING", "LUNCH", "AFTERNOON", "EVENING", "NIGHT"};
    private static final String[] TIMES = {"09:00 AM", "12:30 PM", "03:00 PM", "06:00 PM", "08:30 PM"};

    private final List<Place> places;

    public TimelineAdapter(List<Place> places) {
        this.places = places;
    }

    @NonNull
    @Override
    public TimelineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline_place, parent, false);
        return new TimelineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimelineViewHolder holder, int position) {
        Place place = places.get(position);

        // Time label & time
        int labelIndex = Math.min(position, TIME_LABELS.length - 1);
        holder.timeLabel.setText(TIME_LABELS[labelIndex]);
        holder.time.setText(TIMES[labelIndex]);

        // Place info
        holder.name.setText(place.getName());
        holder.description.setText(getGenericDescription(place));

        // Photo
        if (place.getPhotoReference() != null && !place.getPhotoReference().isEmpty()) {
            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                    + "?maxwidth=800"
                    + "&photo_reference=" + place.getPhotoReference()
                    + "&key=" + API_KEY;

            Glide.with(holder.itemView.getContext())
                    .load(photoUrl)
                    .centerCrop()
                    .into(holder.image);
        } else {
            holder.image.setImageResource(android.R.color.darker_gray);
        }
    }

    @Override
    public int getItemCount() {
        return places.size();
    }

    private String getGenericDescription(Place place) {
        if (place.getCategory() == null) {
            return "A noteworthy stop on your journey.";
        }
        switch (place.getCategory()) {
            case "culture":
                return "A cultural landmark not to miss during your visit.";
            case "restauration":
                return "A culinary stop to recharge and enjoy local flavors.";
            case "loisirs":
                return "A relaxing spot to take a break and enjoy the atmosphere.";
            case "decouvertes":
                return "An unexpected discovery waiting to be explored.";
            default:
                return "A noteworthy stop on your journey.";
        }
    }

    static class TimelineViewHolder extends RecyclerView.ViewHolder {
        final TextView timeLabel;
        final TextView time;
        final TextView name;
        final TextView description;
        final ImageView image;

        TimelineViewHolder(@NonNull View itemView) {
            super(itemView);
            timeLabel = itemView.findViewById(R.id.text_time_label);
            time = itemView.findViewById(R.id.text_time);
            name = itemView.findViewById(R.id.text_place_name);
            description = itemView.findViewById(R.id.text_place_description);
            image = itemView.findViewById(R.id.image_place);
        }
    }
}