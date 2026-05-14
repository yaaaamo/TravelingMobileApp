package com.example.traveling;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class TravelPathViewModel extends ViewModel {

    private final PlaceRepository repository = new PlaceRepository();
    private final RouteGenerator generator = new RouteGenerator();
    private final DirectionsService directionsService = new DirectionsService();

    private final MutableLiveData<List<RouteOption>> routeOptions = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public LiveData<List<RouteOption>> getRouteOptions() { return routeOptions; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getLoading() { return loading; }

    public void generateRoutes(UserPreferences prefs, LatLng userLocation, String travelMode) {
        loading.setValue(true);

        repository.getAllPlaces(new PlaceRepository.PlacesCallback() {
            @Override
            public void onSuccess(List<Place> places) {
                List<RouteOption> options = generator.generateOptions(places, prefs);

                enrichWithDirections(options, userLocation, travelMode);
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
}