package com.example.traveling;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;


public class RouteDetails {
    private final double totalDistanceKm;
    private final int totalDurationMin;
    private final List<LatLng> polylinePoints;
    private final List<Integer> optimizedOrder;

    public RouteDetails(double totalDistanceKm, int totalDurationMin,
                        List<LatLng> polylinePoints, List<Integer> optimizedOrder) {
        this.totalDistanceKm = totalDistanceKm;
        this.totalDurationMin = totalDurationMin;
        this.polylinePoints = polylinePoints;
        this.optimizedOrder = optimizedOrder;
    }

    public double getTotalDistanceKm() { return totalDistanceKm; }
    public int getTotalDurationMin() { return totalDurationMin; }
    public List<LatLng> getPolylinePoints() { return polylinePoints; }
    public List<Integer> getOptimizedOrder() { return optimizedOrder; }


    public String getFormattedDuration() {
        int hours = totalDurationMin / 60;
        int mins = totalDurationMin % 60;
        if (hours > 0) {
            return hours + "h " + mins + "min";
        }
        return mins + "min";
    }


    public String getFormattedDistance() {
        return String.format("%.1f km", totalDistanceKm);
    }
}