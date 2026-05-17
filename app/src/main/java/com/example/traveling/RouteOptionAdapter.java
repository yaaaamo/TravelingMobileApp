package com.example.traveling;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class RouteOptionAdapter
        extends RecyclerView.Adapter<RouteOptionAdapter.RouteViewHolder> {

    private static final String API_KEY = "AIzaSyDxCfUdpFdYXDoVqk91QhWDeRqf2XTOP8c";

    public interface OnSelectListener {
        void onSelect(RouteOption option);
    }

    private List<RouteOption> options;
    private final OnSelectListener listener;

    public RouteOptionAdapter(List<RouteOption> options, OnSelectListener listener) {
        this.options = options;
        this.listener = listener;
    }

    public void updateData(List<RouteOption> newOptions) {
        this.options = newOptions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_route_option, parent, false);
        return new RouteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
        RouteOption option = options.get(position);
        holder.bind(option, listener);
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class RouteViewHolder extends RecyclerView.ViewHolder {
        private final TextView textPlanBadge;
        private final TextView textBudget;
        private final TextView textDuration;
        private final TextView textDistance;
        private final TextView textPlacesCount;
        private final TextView textPlacesList;
        private final Button btnSelect;
        private final ImageView imageRoute;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            textPlanBadge = itemView.findViewById(R.id.text_plan_badge);
            textBudget = itemView.findViewById(R.id.text_budget);
            textDuration = itemView.findViewById(R.id.text_duration);
            textDistance = itemView.findViewById(R.id.text_distance);
            textPlacesCount = itemView.findViewById(R.id.text_places_count);
            textPlacesList = itemView.findViewById(R.id.text_places_list);
            btnSelect = itemView.findViewById(R.id.btn_select_plan);
            imageRoute = itemView.findViewById(R.id.image_route);
        }

        void bind(RouteOption option, OnSelectListener listener) {
            // Badge: "Économique" → "ECONOMIC"
            textPlanBadge.setText(option.getTitle().toUpperCase());

            // Budget
            textBudget.setText(String.format("%.0f €", option.getTotalEstimatedCost()));

            // Distance & duration
            RouteDetails details = option.getRouteDetails();
            if (details != null) {
                textDistance.setText(details.getFormattedDistance());
                textDuration.setText(details.getFormattedDuration());
            } else {
                textDistance.setText("—");
                textDuration.setText("—");
            }

            // Places count
            List<Place> places = option.getPlaces();
            textPlacesCount.setText(String.valueOf(places.size()));

            // Places list as numbered string
            StringBuilder placesBuilder = new StringBuilder();
            for (int i = 0; i < places.size(); i++) {
                placesBuilder.append(i + 1).append(". ")
                        .append(places.get(i).getName());
                if (i < places.size() - 1) {
                    placesBuilder.append("\n");
                }
            }
            textPlacesList.setText(placesBuilder.toString());

            // Load first available photo as card image
            String photoRef = null;
            for (Place p : places) {
                if (p.getPhotoReference() != null && !p.getPhotoReference().isEmpty()) {
                    photoRef = p.getPhotoReference();
                    break;
                }
            }

            if (photoRef != null) {
                String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                        + "?maxwidth=800"
                        + "&photo_reference=" + photoRef
                        + "&key=" + API_KEY;
                Glide.with(itemView.getContext())
                        .load(photoUrl)
                        .centerCrop()
                        .into(imageRoute);
            } else {
                // clear in case the view is being recycled with an old image
                imageRoute.setImageDrawable(null);
            }

            // Click listener
            btnSelect.setOnClickListener(v -> listener.onSelect(option));
        }
    }
}