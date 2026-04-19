package com.example.traveling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RouteGenerator {

    public List<RouteOption> generateOptions(List<Place> allPlaces, UserPreferences prefs) {
        List<Place> filtered = filterPlaces(allPlaces, prefs);

        double ecoBudget = prefs.maxBudget * 0.4;
        double balancedBudget = prefs.maxBudget * 0.65;
        double comfortBudget = prefs.maxBudget * 0.9;

        List<Place> eco = buildRoute(filtered, ecoBudget, "eco");
        List<Place> balanced = buildRoute(filtered, balancedBudget, "balanced");
        List<Place> comfort = buildRoute(filtered, comfortBudget, "comfort");

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

        List<Place> result = new ArrayList<>();

        for (Place place : allPlaces) {
            if (!allowedCategories.isEmpty() && !allowedCategories.contains(place.getCategory())) {
                continue;
            }

            if (prefs.cold && !place.isGoodForCold()) continue;
            if (prefs.heat && !place.isGoodForHeat()) continue;
            if (prefs.rain && !place.isGoodForRain()) continue;

            result.add(place);
        }

        return result;
    }

    private List<Place> buildRoute(List<Place> places, double targetBudget, String mode) {
        List<Place> candidates = new ArrayList<>(places);

        candidates.sort((a, b) -> Double.compare(score(b, mode), score(a, mode)));

        List<Place> selected = new ArrayList<>();
        Set<String> usedCategories = new HashSet<>();
        double total = 0;

        for (Place place : candidates) {
            if (selected.size() >= 4) break;

            if (total + place.getPrice() > targetBudget) {
                continue;
            }

            // Prefer category variety if possible
            if (usedCategories.contains(place.getCategory()) && selected.size() < 3) {
                continue;
            }

            selected.add(place);
            usedCategories.add(place.getCategory());
            total += place.getPrice();
        }

        // If we still have too few places, fill remaining slots
        for (Place place : candidates) {
            if (selected.size() >= 4) break;
            if (selected.contains(place)) continue;
            if (total + place.getPrice() > targetBudget) continue;

            selected.add(place);
            total += place.getPrice();
        }

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
}