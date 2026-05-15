package com.example.traveling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RouteGenerator {

    private boolean isRegenerate = false;

    public void setRegenerate(boolean regenerate) {
        this.isRegenerate = regenerate;
    }

    public List<RouteOption> generateOptions(List<Place> allPlaces, UserPreferences prefs) {

        List<String> requiredNames = parseFavoritePlaces(prefs.favoritePlaces);
        List<Place> requiredPlaces = findRequiredPlaces(allPlaces, requiredNames);

        List<Place> filtered = filterPlaces(allPlaces, prefs);
        filtered.removeAll(requiredPlaces);

        if (isRegenerate) {
            Collections.shuffle(filtered);
        }


        int maxPlaces = calculateMaxPlaces(prefs);
        double budget = prefs.maxBudget;


        double ecoTarget    = budget * 0.33;
        double balancedTarget = budget * 0.66;
        double comfortTarget  = budget;

        List<Place> eco     = buildRoute(filtered, ecoTarget,     "eco",      maxPlaces, requiredPlaces);
        List<Place> balanced = buildRoute(filtered, balancedTarget, "balanced", maxPlaces, requiredPlaces);
        List<Place> comfort  = buildRoute(filtered, comfortTarget,  "comfort",  maxPlaces, requiredPlaces);

        List<RouteOption> options = new ArrayList<>();
        options.add(new RouteOption("Économique", eco,     calculateTotal(eco)));
        options.add(new RouteOption("Équilibré",  balanced, calculateTotal(balanced)));
        options.add(new RouteOption("Confort",    comfort,  calculateTotal(comfort)));

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
            if (place == null) continue;
            if (place.getCategory() == null) continue;
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
        List<Place> selected = new ArrayList<>(requiredPlaces);
        double total = calculateTotal(selected);

        List<Place> candidates = new ArrayList<>(places);

        switch (mode) {
            case "eco":
                candidates.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
                break;
            case "balanced":
                double perPlace = targetBudget / maxPlaces;
                candidates.sort((a, b) -> Double.compare(
                        Math.abs(a.getPrice() - perPlace),
                        Math.abs(b.getPrice() - perPlace)
                ));
                break;
            case "comfort":
                candidates.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
        }

        for (Place place : candidates) {
            if (selected.size() >= maxPlaces) break;
            if (selected.contains(place)) continue;
            if (total + place.getPrice() > targetBudget) continue;
            selected.add(place);
            total += place.getPrice();
        }

        if (selected.isEmpty() && !candidates.isEmpty()) {
            selected.add(candidates.get(0));
        }

        return selected;
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

        if (requiredNames.isEmpty()) return result;

        for (Place place : allPlaces) {
            if (place == null || place.getName() == null) continue; // null guard

            String placeName = place.getName().toLowerCase();

            for (String requiredName : requiredNames) {
                if (requiredName == null) continue; // null guard
                if (placeName.contains(requiredName)) {
                    result.add(place);
                    break;
                }
            }
        }

        return result;
    }

    public List<Place> buildRoutePublic(List<Place> places, double targetBudget,
                                        String mode, int maxPlaces,
                                        List<Place> requiredPlaces) {
        return buildRoute(places, targetBudget, mode, maxPlaces, requiredPlaces);
    }

    public double calculateTotalPublic(List<Place> places) {
        return calculateTotal(places);
    }




}