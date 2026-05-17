package com.example.traveling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Maps extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;


    private List<Map<String, Object>> journeyPlaces = null;
    private String journeyTitle = null;
    private String currentTravelMode = "walking";

    private LinearLayout transportModeLayout;

    private final ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        Boolean fineLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_FINE_LOCATION, false);
                        Boolean coarseLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_COARSE_LOCATION, false);

                        if (Boolean.TRUE.equals(fineLocationGranted) ||
                                Boolean.TRUE.equals(coarseLocationGranted)) {
                            enableUserLocation();
                            moveToUserLocation();
                        }
                    });

    public static Maps newInstanceWithJourney(
            List<Map<String, Object>> places, String title) {
        Maps fragment = new Maps();
        fragment.journeyPlaces = places;
        fragment.journeyTitle = title;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        transportModeLayout = view.findViewById(R.id.transport_mode_layout);


        if (transportModeLayout != null && journeyPlaces != null) {
            transportModeLayout.setVisibility(View.VISIBLE);

            Button btnWalk = view.findViewById(R.id.btn_mode_walk);
            Button btnTransit = view.findViewById(R.id.btn_mode_transit);
            Button btnCar = view.findViewById(R.id.btn_mode_car);
            Button btnBike = view.findViewById(R.id.btn_mode_bike);


            highlightButton(btnWalk, true);

            btnWalk.setOnClickListener(v -> {
                currentTravelMode = "walking";
                highlightButton(btnWalk, true);
                highlightButton(btnTransit, false);
                highlightButton(btnCar, false);
                highlightButton(btnBike, false);
                showJourneyOnMap();
            });

            btnTransit.setOnClickListener(v -> {
                currentTravelMode = "transit";
                highlightButton(btnWalk, false);
                highlightButton(btnTransit, true);
                highlightButton(btnCar, false);
                highlightButton(btnBike, false);
                showJourneyOnMap();
            });

            btnCar.setOnClickListener(v -> {
                currentTravelMode = "driving";
                highlightButton(btnWalk, false);
                highlightButton(btnTransit, false);
                highlightButton(btnCar, true);
                highlightButton(btnBike, false);
                showJourneyOnMap();
            });

            btnBike.setOnClickListener(v -> {
                currentTravelMode = "bicycling";
                highlightButton(btnWalk, false);
                highlightButton(btnTransit, false);
                highlightButton(btnCar, false);
                highlightButton(btnBike, true);
                showJourneyOnMap();
            });
        }

        FragmentManager fragmentManager = getChildFragmentManager();
        SupportMapFragment mapFragment =
                (SupportMapFragment) fragmentManager.findFragmentById(R.id.map_container);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void highlightButton(Button btn, boolean selected) {
        if (btn == null) return;
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                        selected
                                ? android.graphics.Color.parseColor("#6200EE")
                                : android.graphics.Color.parseColor("#E0E0E0")));
        btn.setTextColor(selected ? Color.WHITE : Color.BLACK);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        askForLocationPermission();

        if (journeyPlaces != null) {
            showJourneyOnMap();
        }
    }

    private void showJourneyOnMap() {
        if (mMap == null || journeyPlaces == null || journeyPlaces.isEmpty()) return;

        mMap.clear();

        List<LatLng> waypoints = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasLocation = false;

        for (int i = 0; i < journeyPlaces.size(); i++) {
            Map<String, Object> place = journeyPlaces.get(i);

            Object latObj = place.get("lat");
            Object lngObj = place.get("lng");

            if (latObj == null || lngObj == null) continue;

            double lat = ((Number) latObj).doubleValue();
            double lng = ((Number) lngObj).doubleValue();
            LatLng latLng = new LatLng(lat, lng);

            waypoints.add(latLng);
            boundsBuilder.include(latLng);
            hasLocation = true;

            Object nameObj = place.get("name");
            Object creneauObj = place.get("creneau");
            String name = nameObj != null ? nameObj.toString() : "Place " + (i + 1);
            String creneau = creneauObj != null ? creneauObj.toString() : "";

            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(name)
                    .snippet(creneau)
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            i == 0
                                    ? BitmapDescriptorFactory.HUE_GREEN
                                    : i == journeyPlaces.size() - 1
                                    ? BitmapDescriptorFactory.HUE_RED
                                    : BitmapDescriptorFactory.HUE_VIOLET)));
        }

        if (!hasLocation) return;


        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(
                boundsBuilder.build(), 120));


        if (waypoints.size() >= 2) {
            drawRoute(waypoints);
        }
    }

    private void drawRoute(List<LatLng> waypoints) {
        DirectionsService directionsService = new DirectionsService();
        directionsService.getOptimizedRoute(
                waypoints.get(0),
                waypoints.subList(1, waypoints.size()),
                currentTravelMode,
                new DirectionsService.RouteCallback() {
                    @Override
                    public void onSuccess(RouteDetails details) {
                        if (mMap == null) return;
                        mMap.addPolyline(new PolylineOptions()
                                .addAll(details.getPolylinePoints())
                                .color(Color.parseColor("#6200EE"))
                                .width(10));

                        Toast.makeText(requireContext(),
                                "📍 " + details.getFormattedDistance()
                                        + " • " + details.getFormattedDuration(),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Exception e) {

                    }
                });
    }

    private void askForLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableUserLocation();
            moveToUserLocation();
        } else {
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    private void moveToUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }


        if (journeyPlaces != null) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLatLng = new LatLng(
                        location.getLatitude(), location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f));
            }
        });
    }
}