package com.example.traveling;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TravelPathViewModel extends ViewModel {

    private final PlaceRepository repository = new PlaceRepository();
    private final RouteGenerator generator = new RouteGenerator();
    private final DirectionsService directionsService = new DirectionsService();

    private final MutableLiveData<List<RouteOption>> routeOptions = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private UserPreferences savedPrefs;
    private LatLng savedLocation;
    private String savedTravelMode;

    public LiveData<List<RouteOption>> getRouteOptions() { return routeOptions; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getLoading() { return loading; }

    public void generateRoutes(UserPreferences prefs, LatLng userLocation, String travelMode) {
        this.savedPrefs = prefs;
        this.savedLocation = userLocation;
        this.savedTravelMode = travelMode;
        fetchAndGenerate();
    }

    private void fetchAndGenerate() {
        loading.setValue(true);

        repository.getAllPlaces(new PlaceRepository.PlacesCallback() {
            @Override
            public void onSuccess(List<Place> places) {
                List<RouteOption> options = generator.generateOptions(places, savedPrefs);
                applySelectedDurationToOptions(options);
                enrichWithDirections(options, savedLocation, savedTravelMode);
            }

            @Override
            public void onFailure(Exception e) {
                loading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }

    public void regenerateSingleRoute(int planIndex) {
        if (savedPrefs == null || savedLocation == null) return;

        List<RouteOption> current = routeOptions.getValue();
        if (current == null) return;

        loading.setValue(true);

        repository.getAllPlaces(new PlaceRepository.PlacesCallback() {
            @Override
            public void onSuccess(List<Place> allPlaces) {

                List<String> requiredNames = generator.parseFavoritePublic(savedPrefs.favoritePlaces);
                List<Place> requiredPlaces = generator.findRequiredPublic(allPlaces, requiredNames);


                Set<String> currentPlaceIds = new HashSet<>();
                for (Place p : current.get(planIndex).getPlaces()) {
                    if (p.getId() != null) {

                        boolean isRequired = false;
                        for (Place req : requiredPlaces) {
                            if (p.getId().equals(req.getId())) {
                                isRequired = true;
                                break;
                            }
                        }
                        if (!isRequired) currentPlaceIds.add(p.getId());
                    }
                }

                List<Place> filtered = new ArrayList<>();
                for (Place p : allPlaces) {
                    if (p == null || p.getName() == null) continue;
                    if (!currentPlaceIds.contains(p.getId()) && !requiredPlaces.contains(p)) {
                        filtered.add(p);
                    }
                }

                double budget = savedPrefs.maxBudget;
                double targetBudget;
                switch (planIndex) {
                    case 0: targetBudget = budget * 0.33; break;
                    case 1: targetBudget = budget * 0.66; break;
                    default: targetBudget = budget; break;
                }

                String mode;
                switch (planIndex) {
                    case 0: mode = "eco"; break;
                    case 1: mode = "balanced"; break;
                    default: mode = "comfort"; break;
                }

                int maxPlaces = current.get(planIndex).getPlaces().size();

                Collections.shuffle(filtered);
                // required places'i geç
                List<Place> newPlaces = generator.buildRoutePublic(
                        filtered, targetBudget, mode, maxPlaces, requiredPlaces
                );

                if (newPlaces.isEmpty()) {
                    List<Place> all = new ArrayList<>(allPlaces);
                    Collections.shuffle(all);
                    newPlaces = generator.buildRoutePublic(
                            all, targetBudget, mode, maxPlaces, requiredPlaces
                    );
                }

                RouteOption newOption = new RouteOption(
                        current.get(planIndex).getTitle(),
                        newPlaces,
                        generator.calculateTotalPublic(newPlaces)
                );

                newOption.setSelectedDurationMinutes(getSelectedDurationMinutesFromPrefs());

                List<LatLng> waypoints = newOption.getPlacesAsLatLng();
                if (waypoints.isEmpty()) {
                    List<RouteOption> updated = new ArrayList<>(current);
                    updated.set(planIndex, newOption);
                    loading.postValue(false);
                    routeOptions.postValue(updated);
                    return;
                }

                directionsService.getOptimizedRoute(
                        savedLocation, waypoints, savedTravelMode,
                        new DirectionsService.RouteCallback() {
                            @Override
                            public void onSuccess(RouteDetails details) {
                                newOption.setRouteDetails(details);
                                newOption.reorderPlacesByOptimization();
                                List<RouteOption> updated = new ArrayList<>(current);
                                updated.set(planIndex, newOption);
                                loading.postValue(false);
                                routeOptions.postValue(updated);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                List<RouteOption> updated = new ArrayList<>(current);
                                updated.set(planIndex, newOption);
                                loading.postValue(false);
                                routeOptions.postValue(updated);
                            }
                        }
                );
            }

            @Override
            public void onFailure(Exception e) {
                loading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }


    private void enrichWithDirections(List<RouteOption> options,
                                      LatLng userLocation, String travelMode) {
        int[] completed = {0};
        int total = options.size();

        for (RouteOption option : options) {
            List<LatLng> waypoints = option.getPlacesAsLatLng();

            if (waypoints.isEmpty()) {
                checkAllDone(completed, total, options);
                continue;
            }

            directionsService.getOptimizedRoute(
                    userLocation, waypoints, travelMode,
                    new DirectionsService.RouteCallback() {
                        @Override
                        public void onSuccess(RouteDetails details) {
                            option.setRouteDetails(details);
                            option.reorderPlacesByOptimization();
                            checkAllDone(completed, total, options);
                        }

                        @Override
                        public void onFailure(Exception e) {
                            checkAllDone(completed, total, options);
                        }
                    }
            );
        }
    }

    private void checkAllDone(int[] completed, int total, List<RouteOption> options) {
        completed[0]++;
        if (completed[0] == total) {
            loading.postValue(false);
            routeOptions.postValue(options);
        }
    }

    private void applySelectedDurationToOptions(List<RouteOption> options) {
        int selectedDurationMinutes = getSelectedDurationMinutesFromPrefs();

        for (RouteOption option : options) {
            option.setSelectedDurationMinutes(selectedDurationMinutes);
        }
    }

    private int getSelectedDurationMinutesFromPrefs() {
        if (savedPrefs == null || savedPrefs.duration == null) {
            return 180;
        }

        try {
            int durationHours = Integer.parseInt(savedPrefs.duration.trim());
            return durationHours * 60;
        } catch (Exception e) {
            return 180;
        }
    }

    public void regenerateSingleRouteWithPrefs(int planIndex, UserPreferences adjustedPrefs) {
        if (savedLocation == null) return;

        List<RouteOption> current = routeOptions.getValue();
        if (current == null) return;

        loading.setValue(true);

        UserPreferences originalPrefs = savedPrefs;

        repository.getAllPlaces(new PlaceRepository.PlacesCallback() {
            @Override
            public void onSuccess(List<Place> allPlaces) {
                List<String> requiredNames =
                        generator.parseFavoritePublic(
                                originalPrefs != null ? originalPrefs.favoritePlaces : "");
                List<Place> requiredPlaces =
                        generator.findRequiredPublic(allPlaces, requiredNames);

                List<Place> filtered = new ArrayList<>();
                List<String> allowedCategories = new ArrayList<>();
                if (adjustedPrefs.culture) allowedCategories.add("culture");
                if (adjustedPrefs.restauration) allowedCategories.add("restauration");
                if (adjustedPrefs.loisirs) allowedCategories.add("loisirs");
                if (adjustedPrefs.decouvertes) allowedCategories.add("decouvertes");

                for (Place p : allPlaces) {
                    if (p == null || p.getName() == null) continue;
                    if (!allowedCategories.isEmpty()
                            && !allowedCategories.contains(p.getCategory())) continue;
                    filtered.add(p);
                }

                Collections.shuffle(filtered);

                double targetBudget;
                String mode;
                switch (planIndex) {
                    case 0: targetBudget = adjustedPrefs.maxBudget * 0.33;
                        mode = "eco"; break;
                    case 1: targetBudget = adjustedPrefs.maxBudget * 0.66;
                        mode = "balanced"; break;
                    default: targetBudget = adjustedPrefs.maxBudget;
                        mode = "comfort"; break;
                }

                int maxPlaces = current.get(planIndex).getPlaces().size();

                List<Place> newPlaces = generator.buildRoutePublic(
                        filtered, targetBudget, mode, maxPlaces, requiredPlaces);

                if (newPlaces.isEmpty()) {
                    Collections.shuffle(allPlaces);
                    newPlaces = generator.buildRoutePublic(
                            allPlaces, targetBudget, mode, maxPlaces, requiredPlaces);
                }

                RouteOption newOption = new RouteOption(
                        current.get(planIndex).getTitle(),
                        newPlaces,
                        generator.calculateTotalPublic(newPlaces)
                );

                newOption.setSelectedDurationMinutes(getSelectedDurationMinutesFromPrefs());

                List<LatLng> waypoints = newOption.getPlacesAsLatLng();
                if (waypoints.isEmpty()) {
                    List<RouteOption> updated = new ArrayList<>(current);
                    updated.set(planIndex, newOption);
                    loading.postValue(false);
                    routeOptions.postValue(updated);
                    return;
                }

                directionsService.getOptimizedRoute(
                        savedLocation, waypoints, savedTravelMode,
                        new DirectionsService.RouteCallback() {
                            @Override
                            public void onSuccess(RouteDetails details) {
                                newOption.setRouteDetails(details);
                                newOption.reorderPlacesByOptimization();
                                List<RouteOption> updated = new ArrayList<>(current);
                                updated.set(planIndex, newOption);
                                loading.postValue(false);
                                routeOptions.postValue(updated);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                List<RouteOption> updated = new ArrayList<>(current);
                                updated.set(planIndex, newOption);
                                loading.postValue(false);
                                routeOptions.postValue(updated);
                            }
                        }
                );
            }

            @Override
            public void onFailure(Exception e) {
                loading.setValue(false);
                errorMessage.setValue(e.getMessage());
            }
        });
    }
}