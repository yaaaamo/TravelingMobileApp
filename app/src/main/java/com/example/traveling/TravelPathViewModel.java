package com.example.traveling;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class TravelPathViewModel extends ViewModel {

    private final PlaceRepository repository = new PlaceRepository();
    private final RouteGenerator generator = new RouteGenerator();

    private final MutableLiveData<List<RouteOption>> routeOptions = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<List<RouteOption>> getRouteOptions() {
        return routeOptions;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void generateRoutes(UserPreferences prefs) {
        repository.getAllPlaces(new PlaceRepository.PlacesCallback() {
            @Override
            public void onSuccess(List<Place> places) {
                List<RouteOption> options = generator.generateOptions(places, prefs);
                routeOptions.setValue(options);
            }

            @Override
            public void onFailure(Exception e) {
                errorMessage.setValue(e.getMessage());
            }
        });
    }
}
