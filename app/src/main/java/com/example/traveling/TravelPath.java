package com.example.traveling;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

public class TravelPath extends Fragment {

    private TravelPathViewModel viewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_travel_path, container, false);

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

        TextView textEco = view.findViewById(R.id.text_option_eco);
        TextView textBalanced = view.findViewById(R.id.text_option_balanced);
        TextView textComfort = view.findViewById(R.id.text_option_comfort);

        viewModel = new ViewModelProvider(this).get(TravelPathViewModel.class);

        String[] efforts = {"Faible", "Moyen", "Élevé"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                efforts
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEffort.setAdapter(adapter);

        viewModel.getRouteOptions().observe(getViewLifecycleOwner(), options -> {
            if (options == null || options.size() < 3) {
                Toast.makeText(requireContext(), "Pas assez d'options trouvées.", Toast.LENGTH_SHORT).show();
                return;
            }

            textEco.setText(formatOption(options.get(0)));
            textBalanced.setText(formatOption(options.get(1)));
            textComfort.setText(formatOption(options.get(2)));
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Toast.makeText(requireContext(), "Erreur: " + error, Toast.LENGTH_LONG).show();
            }
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

            viewModel.generateRoutes(prefs);
        });

        return view;
    }

    private String formatOption(RouteOption option) {
        StringBuilder builder = new StringBuilder();
        builder.append(option.getTitle())
                .append(" - Budget estimé: ")
                .append(option.getTotalEstimatedCost())
                .append(" €\n");

        List<Place> places = option.getPlaces();
        for (Place place : places) {
            builder.append("• ").append(place.getName()).append("\n");
        }

        return builder.toString();
    }
}