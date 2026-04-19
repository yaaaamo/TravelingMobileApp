package com.example.traveling;


import java.util.List;

public class RouteOption {
    private String title;
    private List<Place> places;
    private double totalEstimatedCost;

    public RouteOption(String title, List<Place> places, double totalEstimatedCost) {
        this.title = title;
        this.places = places;
        this.totalEstimatedCost = totalEstimatedCost;
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
}
