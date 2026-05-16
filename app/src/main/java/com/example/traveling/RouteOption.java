package com.example.traveling;

import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;

public class RouteOption {
    private final String title;
    private List<Place> places;
    private final double totalEstimatedCost;
    private RouteDetails routeDetails;


    private int selectedDurationMinutes;

    public RouteOption(String title, List<Place> places, double totalEstimatedCost) {
        this.title = title;
        this.places = places;
        this.totalEstimatedCost = totalEstimatedCost;
        this.selectedDurationMinutes = 0;
    }

    public RouteOption(String title, List<Place> places, double totalEstimatedCost, int selectedDurationMinutes) {
        this.title = title;
        this.places = places;
        this.totalEstimatedCost = totalEstimatedCost;
        this.selectedDurationMinutes = selectedDurationMinutes;
    }

    public String getTitle() {
        return title;
    }

    public List<Place> getPlaces() {
        return places;
    }

    public double getTotalEstimatedCost() {
        return totalEstimatedCost;
    }

    public RouteDetails getRouteDetails() {
        return routeDetails;
    }

    public void setRouteDetails(RouteDetails routeDetails) {
        this.routeDetails = routeDetails;
    }

    public int getSelectedDurationMinutes() {
        return selectedDurationMinutes;
    }

    public void setSelectedDurationMinutes(int selectedDurationMinutes) {
        this.selectedDurationMinutes = selectedDurationMinutes;
    }

    public String getFormattedSelectedDuration() {
        if (selectedDurationMinutes <= 0) {
            return "—";
        }

        int hours = selectedDurationMinutes / 60;
        int minutes = selectedDurationMinutes % 60;

        if (hours > 0 && minutes > 0) {
            return hours + "h " + minutes + "min";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return minutes + "min";
        }
    }

    public List<LatLng> getPlacesAsLatLng() {
        List<LatLng> result = new ArrayList<>();
        for (Place place : places) {
            if (place.getLocation() != null) {
                result.add(new LatLng(
                        place.getLocation().getLatitude(),
                        place.getLocation().getLongitude()
                ));
            }
        }
        return result;
    }

    public void reorderPlacesByOptimization() {
        if (routeDetails == null || routeDetails.getOptimizedOrder().isEmpty()) return;

        List<Integer> order = routeDetails.getOptimizedOrder();
        List<Place> reordered = new ArrayList<>();

        for (Integer index : order) {
            reordered.add(places.get(index));
        }

        reordered.add(places.get(places.size() - 1));

        this.places = reordered;
    }
}