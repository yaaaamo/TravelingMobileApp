package com.example.traveling;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class TravelPath extends Fragment {

    private TravelPathViewModel viewModel;
    private FusedLocationProviderClient fusedLocationClient;

    private TextView textEco, textBalanced, textComfort;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_travel_path, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        CheckBox checkRestauration = view.findViewById(R.id.check_restauration);
        CheckBox checkLoisirs = view.findViewById(R.id.check_loisirs);
        CheckBox checkDecouvertes = view.findViewById(R.id.check_decouvertes);
        CheckBox checkCulture = view.findViewById(R.id.check_culture);

        EditText editPlaces = view.findViewById(R.id.edit_places);
        EditText editBudget = view.findViewById(R.id.edit_budget);
        EditText editDuration = view.findViewById(R.id.edit_duration);

        Spinner spinnerEffort = view.findViewById(R.id.spinner_effort);

        CheckBox checkCold = view.findViewById(R.id.check_cold);
        CheckBox checkHeat = view.findViewById(R.id.check_heat);
        CheckBox checkRain = view.findViewById(R.id.check_rain);

        Button btnSave = view.findViewById(R.id.btn_save_preferences);

        textEco = view.findViewById(R.id.text_option_eco);
        textBalanced = view.findViewById(R.id.text_option_balanced);
        textComfort = view.findViewById(R.id.text_option_comfort);
        progressBar = view.findViewById(R.id.progress_bar);

        viewModel = new ViewModelProvider(requireActivity()).get(TravelPathViewModel.class);

        String[] efforts = {"Faible", "Moyen", "Élevé"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item, efforts);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEffort.setAdapter(adapter);

        viewModel.getRouteOptions().observe(getViewLifecycleOwner(), options -> {
            if (options == null || options.isEmpty()) {
                Toast.makeText(requireContext(),
                        "Aucune option trouvée.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Fragment current = requireActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.fragment_container);
            if (current instanceof TravelPath) {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new RouteResults())
                        .addToBackStack(null)
                        .commit();
            }

        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        btnSave.setOnClickListener(v -> {
            String budgetText = editBudget.getText().toString().trim();
            String durationText = editDuration.getText().toString().trim();

            if (TextUtils.isEmpty(budgetText) || TextUtils.isEmpty(durationText)) {
                Toast.makeText(requireContext(),
                        "Veuillez remplir le budget et la durée.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            UserPreferences prefs = new UserPreferences();
            prefs.restauration = checkRestauration.isChecked();
            prefs.loisirs = checkLoisirs.isChecked();
            prefs.decouvertes = checkDecouvertes.isChecked();
            prefs.culture = checkCulture.isChecked();
            prefs.favoritePlaces = editPlaces.getText().toString().trim();
            prefs.maxBudget = Double.parseDouble(budgetText);
            prefs.duration = durationText;
            prefs.effort = spinnerEffort.getSelectedItem().toString();
            prefs.cold = checkCold.isChecked();
            prefs.heat = checkHeat.isChecked();
            prefs.rain = checkRain.isChecked();

            fetchLocationAndGenerate(prefs);
        });

        return view;
    }

    private void fetchLocationAndGenerate(UserPreferences prefs) {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(),
                    "Permission de localisation requise. Ouvrez la carte d'abord.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(requireContext(),
                        "Position introuvable. Activez la localisation.",
                        Toast.LENGTH_LONG).show();
                return;
            }
            LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
            viewModel.generateRoutes(prefs, userLocation, "walking");
        });
    }

    private String formatOption(RouteOption option) {
        StringBuilder builder = new StringBuilder();
        builder.append(option.getTitle())
                .append(" — ")
                .append(option.getTotalEstimatedCost())
                .append(" €");

        RouteDetails details = option.getRouteDetails();
        if (details != null) {
            builder.append(" | ")
                    .append(details.getFormattedDistance())
                    .append(" | ")
                    .append(details.getFormattedDuration());
        }

        builder.append("\n");

        List<Place> places = option.getPlaces();
        for (int i = 0; i < places.size(); i++) {
            builder.append(i + 1).append(". ")
                    .append(places.get(i).getName())
                    .append("\n");
        }

        return builder.toString();
    }
}