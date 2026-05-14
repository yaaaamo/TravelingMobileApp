package com.example.traveling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RouteGenerator {

    public List<RouteOption> generateOptions(List<Place> allPlaces, UserPreferences prefs) {

        List<String> requiredNames = parseFavoritePlaces(prefs.favoritePlaces);
        List<Place> requiredPlaces = findRequiredPlaces(allPlaces, requiredNames);

        List<Place> filtered = filterPlaces(allPlaces, prefs);
        filtered.removeAll(requiredPlaces);

        double ecoBudget = prefs.maxBudget * 0.4;
        double balancedBudget = prefs.maxBudget * 0.65;
        double comfortBudget = prefs.maxBudget * 0.9;

        int maxPlaces = calculateMaxPlaces(prefs);

        List<Place> eco = buildRoute(filtered, ecoBudget, "eco", maxPlaces, requiredPlaces);
        List<Place> balanced = buildRoute(filtered, balancedBudget, "balanced", maxPlaces, requiredPlaces);
        List<Place> comfort = buildRoute(filtered, comfortBudget, "comfort", maxPlaces, requiredPlaces);


        List<RouteOption> options = new ArrayList<>();
        options.add(new RouteOption("Économique", eco, calculateTotal(eco)));
        options.add(new RouteOption("Équilibré", balanced, calculateTotal(balanced)));
        options.add(new RouteOption("Confort", comfort, calculateTotal(comfort)));

        return options;
    }

    private List<Place> filterPlaces(List<Place> allPlaces, UserPreferences prefs) {
        List<String> allowedCategories = new ArrayList<>();

        if (prefs.restauration) allowedCategories.add("restauration");
        if (prefs.loisirs) allowedCategories.add("loisirs");
        if (prefs.decouvertes) allowedCategories.add("decouvertes");
        if (prefs.culture) allowedCategories.add("culture");

        int maxAllowedEffort = mapEffortToMax(prefs.effort);

        List<Place> result = new ArrayList<>();

        for (Place place : allPlaces) {
            if (!allowedCategories.isEmpty() && !allowedCategories.contains(place.getCategory())) {
                continue;
            }

            if (prefs.cold && !place.isGoodForCold()) continue;
            if (prefs.heat && !place.isGoodForHeat()) continue;
            if (prefs.rain && !place.isGoodForRain()) continue;

            if (place.getEffortLevel() > maxAllowedEffort) continue;

            result.add(place);
        }

        return result;
    }

    private int mapEffortToMax(String userEffort) {
        if (userEffort == null) return 3;
        switch (userEffort) {
            case "Faible": return 1;
            case "Moyen":  return 2;
            case "Élevé":  return 3;
            default:       return 3;
        }
    }
    private int calculateMaxPlaces(UserPreferences prefs) {
        if (prefs.duration == null || prefs.duration.trim().isEmpty()) {
            return 4;
        }
        int hours;
        try {
            hours = Integer.parseInt(prefs.duration.trim());
        } catch (NumberFormatException e) {
            return 4;
        }
        if (hours <= 0) return 4;

        return mapHoursToPlaces(hours);
    }

    private int mapHoursToPlaces(int hours) {
        if (hours <= 2) return 2;
        if (hours <= 4) return 3;
        if (hours <= 6) return 4;
        return 5;
    }

    private int mapDaysToPlaces(int days) {
        int total = days * 4;
        return Math.min(total, 20);
    }

    private List<Place> buildRoute(List<Place> places, double targetBudget, String mode, int maxPlaces, List<Place> requiredPlaces) {
        android.util.Log.d("RouteGenerator",
                "Mode: " + mode + ", maxPlaces: " + maxPlaces +
                        ", candidates: " + places.size() + ", budget: " + targetBudget);

        List<Place> selected = new ArrayList<>();
        Set<String> usedCategories = new HashSet<>();
        double total = 0;

        for (Place required : requiredPlaces) {
            if (selected.size() >= maxPlaces) break;
            selected.add(required);
            usedCategories.add(required.getCategory());
            total += required.getPrice();
        }

        List<Place> candidates = new ArrayList<>(places);
        candidates.sort((a, b) -> Double.compare(score(b, mode), score(a, mode)));

        for (Place place : candidates) {
            if (selected.size() >= maxPlaces) break;

            if (total + place.getPrice() > targetBudget) continue;

            if (usedCategories.contains(place.getCategory())
                    && selected.size() < (maxPlaces - 1)) {
                continue;
            }

            selected.add(place);
            usedCategories.add(place.getCategory());
            total += place.getPrice();
        }

        for (Place place : candidates) {
            if (selected.size() >= maxPlaces) break;
            if (selected.contains(place)) continue;
            if (total + place.getPrice() > targetBudget) continue;

            selected.add(place);
            total += place.getPrice();
        }
        android.util.Log.d("RouteGenerator",
                "Mode: " + mode + " → selected: " + selected.size() +
                        ", totalCost: " + total);

        return selected;

    }

    private double score(Place place, String mode) {
        double price = place.getPrice();
        int effort = place.getEffortLevel();

        switch (mode) {
            case "eco":
                // Cheap and easy
                return (100 - price) + (20 - effort * 5);

            case "balanced":
                // Medium price is better
                double balancedPriceScore = 50 - Math.abs(35 - price);
                return balancedPriceScore + (15 - effort * 3);

            case "comfort":
                // More premium, but easy effort
                return price + (20 - effort * 5);

            default:
                return 0;
        }
    }

    private double calculateTotal(List<Place> places) {
        double total = 0;
        for (Place place : places) {
            total += place.getPrice();
        }
        return total;
    }

    private List<String> parseFavoritePlaces(String input) {
        if (input == null || input.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        String[] parts = input.split(",");
        for (String part : parts) {
            String trimmed = part.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private List<Place> findRequiredPlaces(List<Place> allPlaces, List<String> requiredNames) {
        List<Place> result = new ArrayList<>();

        for (Place place : allPlaces) {
            String placeName = place.getName().toLowerCase();

            for (String requiredName : requiredNames) {
                if (placeName.contains(requiredName)) {
                    result.add(place);
                    break;
                }
            }
        }

        return result;
    }


}