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
    private LinearLayout timelineContainer;
    private LayoutInflater savedInflater;

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
        savedInflater = inflater;

        int planIndex = getArguments() != null
                ? getArguments().getInt(ARG_PLAN_INDEX, 0) : 0;

        TravelPathViewModel viewModel =
                new ViewModelProvider(requireActivity())
                        .get(TravelPathViewModel.class);

        List<RouteOption> options = viewModel.getRouteOptions().getValue();
        if (options == null || planIndex >= options.size()) return view;

        timelineContainer = view.findViewById(R.id.timeline_container);

        TextView statPrice = view.findViewById(R.id.text_stat_price);
        TextView statEffort = view.findViewById(R.id.text_stat_effort);
        TextView statKm = view.findViewById(R.id.text_stat_km);
        TextView title = view.findViewById(R.id.text_journey_title);
        TextView description = view.findViewById(R.id.text_journey_description);

        RouteOption selectedOption = options.get(planIndex);
        setupMiniMap(selectedOption);
        renderRoute(view, selectedOption, title, description, statPrice, statEffort, statKm);
        setupWeather(view, selectedOption);

        Button btnRegenerate = view.findViewById(R.id.btn_regenerate);

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnRegenerate.setEnabled(!isLoading);
            btnRegenerate.setText(isLoading ? "Loading..." : "🔄 Regenerate");
        });

        viewModel.getRouteOptions().observe(getViewLifecycleOwner(), updatedOptions -> {
            if (updatedOptions == null || planIndex >= updatedOptions.size()) return;
            RouteOption updatedOption = updatedOptions.get(planIndex);
            renderRoute(view, updatedOption, title, description, statPrice, statEffort, statKm);
        });

        btnRegenerate.setOnClickListener(v ->
                viewModel.regenerateSingleRoute(planIndex)
        );

        return view;
    }

    private void renderRoute(View view, RouteOption option,
                             TextView title, TextView description,
                             TextView statPrice, TextView statEffort, TextView statKm) {

        String[] TIME_LABELS = {"MORNING", "LUNCH", "AFTERNOON", "EVENING", "NIGHT"};
        String[] TIMES = {"09:00 AM", "12:30 PM", "03:00 PM", "06:00 PM", "08:30 PM"};
        String API_KEY = "AIzaSyDxCfUdpFdYXDoVqk91QhWDeRqf2XTOP8c";

        title.setText("Your " + option.getTitle() + " Escape");
        description.setText("A curated journey through " + option.getPlaces().size()
                + " unique places, balancing culture, taste, and discovery.");

        timelineContainer.removeAllViews();
        List<Place> places = option.getPlaces();
        for (int i = 0; i < places.size(); i++) {
            Place place = places.get(i);
            View itemView = savedInflater.inflate(R.layout.item_timeline_place,
                    timelineContainer, false);

            int labelIndex = Math.min(i, TIME_LABELS.length - 1);
            ((TextView) itemView.findViewById(R.id.text_time_label)).setText(TIME_LABELS[labelIndex]);
            ((TextView) itemView.findViewById(R.id.text_time)).setText(TIMES[labelIndex]);
            ((TextView) itemView.findViewById(R.id.text_place_name)).setText(place.getName());
            ((TextView) itemView.findViewById(R.id.text_place_description))
                    .setText(getGenericDescription(place));

            if (place.getPhotoReference() != null && !place.getPhotoReference().isEmpty()) {
                String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                        + "?maxwidth=800"
                        + "&photo_reference=" + place.getPhotoReference()
                        + "&key=" + API_KEY;
                Glide.with(requireContext())
                        .load(photoUrl)
                        .centerCrop()
                        .into((ImageView) itemView.findViewById(R.id.image_place));
            }

            timelineContainer.addView(itemView);
        }

        statPrice.setText(String.format("%.0f €", option.getTotalEstimatedCost()));

        if (!places.isEmpty()) {
            double avg = 0;
            for (Place p : places) avg += p.getEffortLevel();
            avg = avg / places.size();
            statEffort.setText(String.format("%.1f / 3", avg));
        }

        RouteDetails details = option.getRouteDetails();
        statKm.setText(details != null ? details.getFormattedDistance() : "—");

        updateMap(option);
    }

    private com.google.android.gms.maps.GoogleMap savedMap;

    private void setupMiniMap(RouteOption selectedOption) {
        FragmentManager fragmentManager = getChildFragmentManager();
        SupportMapFragment mapFragment =
                (SupportMapFragment) fragmentManager.findFragmentById(R.id.mini_map);

        if (mapFragment == null) return;

        mapFragment.getMapAsync(googleMap -> {
            savedMap = googleMap;
            updateMap(selectedOption);
        });
    }

    private void updateMap(RouteOption option) {
        if (savedMap == null) return;

        savedMap.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidLocation = false;

        for (Place place : option.getPlaces()) {
            if (place.getLocation() != null) {
                LatLng latLng = new LatLng(
                        place.getLocation().getLatitude(),
                        place.getLocation().getLongitude());

                savedMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(place.getName()));

                boundsBuilder.include(latLng);
                hasValidLocation = true;
            }
        }

        RouteDetails details = option.getRouteDetails();
        if (details != null && details.getPolylinePoints() != null) {
            savedMap.addPolyline(new PolylineOptions()
                    .addAll(details.getPolylinePoints())
                    .color(Color.BLUE)
                    .width(8));
        }

        if (hasValidLocation) {
            savedMap.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                            boundsBuilder.build(), 100));
        }
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

        if (count == 0) return;

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