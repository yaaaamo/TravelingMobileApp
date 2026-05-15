package com.example.traveling;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;


import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

public class RouteDetail extends Fragment {

    private static final String ARG_PLAN_INDEX = "plan_index";

    public static RouteDetail newInstance(int planIndex) {
        RouteDetail fragment = new RouteDetail();
        Bundle args = new Bundle();
        args.putInt(ARG_PLAN_INDEX, planIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_detail, container, false);

        int planIndex = getArguments() != null
                ? getArguments().getInt(ARG_PLAN_INDEX, 0) : 0;

        TravelPathViewModel viewModel =
                new ViewModelProvider(requireActivity())
                        .get(TravelPathViewModel.class);

        List<RouteOption> options = viewModel.getRouteOptions().getValue();
        if (options == null || planIndex >= options.size()) {
            return view;
        }

        RouteOption selectedOption = options.get(planIndex);

        android.util.Log.d("RouteDetail", "Total places: " + selectedOption.getPlaces().size());
        for (Place p : selectedOption.getPlaces()) {
            android.util.Log.d("RouteDetail",
                    "Place: " + p.getName() +
                            ", location: " + (p.getLocation() == null ? "NULL" : "OK"));
        }


        TextView title = view.findViewById(R.id.text_journey_title);
        title.setText(generateTitle(selectedOption));

        TextView description = view.findViewById(R.id.text_journey_description);
        description.setText(generateDescription(selectedOption));

        // Timeline RecyclerView
        LinearLayout timelineContainer = view.findViewById(R.id.timeline_container);


        String[] TIME_LABELS = {"MORNING", "LUNCH", "AFTERNOON", "EVENING", "NIGHT"};
        String[] TIMES = {"09:00 AM", "12:30 PM", "03:00 PM", "06:00 PM", "08:30 PM"};
        String API_KEY = "AIzaSyDxCfUdpFdYXDoVqk91QhWDeRqf2XTOP8c";

        List<Place> places = selectedOption.getPlaces();
        for (int i = 0; i < places.size(); i++) {
            Place place = places.get(i);
            View itemView = inflater.inflate(R.layout.item_timeline_place, timelineContainer, false);

            TextView timeLabel = itemView.findViewById(R.id.text_time_label);
            TextView time = itemView.findViewById(R.id.text_time);
            TextView name = itemView.findViewById(R.id.text_place_name);
            TextView placeDescription = itemView.findViewById(R.id.text_place_description);
            placeDescription.setText(getGenericDescription(place));
            ImageView image = itemView.findViewById(R.id.image_place);

            int labelIndex = Math.min(i, TIME_LABELS.length - 1);
            timeLabel.setText(TIME_LABELS[labelIndex]);
            time.setText(TIMES[labelIndex]);
            name.setText(place.getName());
            description.setText(getGenericDescription(place));

            if (place.getPhotoReference() != null && !place.getPhotoReference().isEmpty()) {
                String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                        + "?maxwidth=800"
                        + "&photo_reference=" + place.getPhotoReference()
                        + "&key=" + API_KEY;
                Glide.with(requireContext()).load(photoUrl).centerCrop().into(image);
            }

            timelineContainer.addView(itemView);
        }

        // MINI MAP
        setupMiniMap(selectedOption);
        setupWeather(view, selectedOption);

        TextView statPrice = view.findViewById(R.id.text_stat_price);
        TextView statEffort = view.findViewById(R.id.text_stat_effort);
        TextView statKm = view.findViewById(R.id.text_stat_km);

// price
        statPrice.setText(String.format("%.0f €", selectedOption.getTotalEstimatedCost()));

// average effort
        if (!places.isEmpty()) {
            double avg = 0;
            for (Place p : places) avg += p.getEffortLevel();
            avg = avg / places.size();
            statEffort.setText(String.format("%.1f / 3", avg));
        }

// km
        RouteDetails details = selectedOption.getRouteDetails();
        if (details != null) {
            statKm.setText(details.getFormattedDistance());
        } else {
            statKm.setText("—");
        }

        Button btnRegenerate = view.findViewById(R.id.btn_regenerate);
        btnRegenerate.setOnClickListener(v -> {
            TravelPathViewModel vm = new ViewModelProvider(requireActivity())
                    .get(TravelPathViewModel.class);
            vm.regenerateRoute(planIndex);

            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }


    private void setupMiniMap(RouteOption selectedOption) {
        FragmentManager fragmentManager = getChildFragmentManager();
        SupportMapFragment mapFragment =
                (SupportMapFragment) fragmentManager.findFragmentById(R.id.mini_map);

        if (mapFragment == null) return;

        mapFragment.getMapAsync(googleMap -> {
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boolean hasValidLocation = false;


            for (Place place : selectedOption.getPlaces()) {
                if (place.getLocation() != null) {
                    LatLng latLng = new LatLng(
                            place.getLocation().getLatitude(),
                            place.getLocation().getLongitude());

                    googleMap.addMarker(new MarkerOptions()
                            .position(latLng)
                            .title(place.getName()));

                    boundsBuilder.include(latLng);
                    hasValidLocation = true;
                }
            }


            RouteDetails details = selectedOption.getRouteDetails();
            if (details != null && details.getPolylinePoints() != null) {
                googleMap.addPolyline(new PolylineOptions()
                        .addAll(details.getPolylinePoints())
                        .color(Color.BLUE)
                        .width(8));
            }


            if (hasValidLocation) {
                googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                                boundsBuilder.build(), 100));
            }
        });
    }

    private void setupWeather(View view, RouteOption option) {

        double sumLat = 0, sumLng = 0;
        int count = 0;

        for (Place place : option.getPlaces()) {
            if (place.getLocation() != null) {
                sumLat += place.getLocation().getLatitude();
                sumLng += place.getLocation().getLongitude();
                count++;
            }
        }

        if (count == 0) return; // Konum bilgisi yok

        double centerLat = sumLat / count;
        double centerLng = sumLng / count;


        WeatherService weatherService = new WeatherService();
        weatherService.getTodayForecast(centerLat, centerLng,
                new WeatherService.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherInfo info) {
                        LinearLayout layout = view.findViewById(R.id.layout_weather);
                        TextView text = view.findViewById(R.id.text_weather);

                        text.setText(info.getShortDisplay());
                        layout.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(Exception e) {

                        Log.e("RouteDetail", "Weather fetch failed", e);
                    }
                });
    }

    private String generateTitle(RouteOption option) {
        return "Your " + option.getTitle() + " Escape";
    }

    private String generateDescription(RouteOption option) {
        return "A curated journey through " + option.getPlaces().size()
                + " unique places, balancing culture, taste, and discovery.";
    }

    private String getGenericDescription(Place place) {
        if (place.getCategory() == null) return "A noteworthy stop on your journey.";
        switch (place.getCategory()) {
            case "culture": return "A cultural landmark not to miss during your visit.";
            case "restauration": return "A culinary stop to recharge and enjoy local flavors.";
            case "loisirs": return "A relaxing spot to take a break and enjoy the atmosphere.";
            case "decouvertes": return "An unexpected discovery waiting to be explored.";
            default: return "A noteworthy stop on your journey.";
        }
    }
}