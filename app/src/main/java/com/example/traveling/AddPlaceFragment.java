package com.example.traveling;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.HashMap;
import java.util.Map;

public class AddPlaceFragment extends Fragment {

    private PlacesApiService placesApiService;
    private PlacesApiService.PlaceSearchResult searchResult;

    private EditText editSearch;
    private TextView textFoundName;
    private EditText editPrice;
    private Spinner spinnerCategory;
    private Spinner spinnerEffort;
    private CheckBox checkCold, checkHeat, checkRain;
    private Button btnSearch, btnSave;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_place, container, false);

        placesApiService = new PlacesApiService();

        // View'ları bağla
        editSearch = view.findViewById(R.id.edit_place_search);
        textFoundName = view.findViewById(R.id.text_found_name);
        editPrice = view.findViewById(R.id.edit_place_price);
        spinnerCategory = view.findViewById(R.id.spinner_category);
        spinnerEffort = view.findViewById(R.id.spinner_effort_place);
        checkCold = view.findViewById(R.id.check_place_cold);
        checkHeat = view.findViewById(R.id.check_place_heat);
        checkRain = view.findViewById(R.id.check_place_rain);
        btnSearch = view.findViewById(R.id.btn_search_place);
        btnSave = view.findViewById(R.id.btn_save_place);
        progressBar = view.findViewById(R.id.progress_add_place);

        // Spinner'ları doldur
        String[] categories = {"culture", "restauration", "loisirs", "decouvertes"};
        spinnerCategory.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categories));

        String[] efforts = {"1", "2", "3"};
        spinnerEffort.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, efforts));

        // Kaydet butonu başta disabled
        btnSave.setEnabled(false);

        // Ara butonuna tıkla
        btnSearch.setOnClickListener(v -> {
            String query = editSearch.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(getContext(), "Yer adı gir", Toast.LENGTH_SHORT).show();
                return;
            }
            searchPlace(query);
        });

        // Kaydet butonuna tıkla
        btnSave.setOnClickListener(v -> savePlace());

        return view;
    }

    private void searchPlace(String query) {
        progressBar.setVisibility(View.VISIBLE);
        btnSearch.setEnabled(false);
        textFoundName.setText("Aranıyor...");
        btnSave.setEnabled(false);

        placesApiService.searchPlace(query, new PlacesApiService.PlaceSearchCallback() {
            @Override
            public void onSuccess(PlacesApiService.PlaceSearchResult result) {
                searchResult = result;
                progressBar.setVisibility(View.GONE);
                btnSearch.setEnabled(true);
                textFoundName.setText("Bulundu: " + result.name
                        + "\n(" + result.lat + ", " + result.lng + ")"
                        + (result.photoReference != null ? "\n📷 Fotoğraf var" : "\n❌ Fotoğraf yok"));
                btnSave.setEnabled(true);
            }

            @Override
            public void onFailure(Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSearch.setEnabled(true);
                textFoundName.setText("Hata: " + e.getMessage());
                btnSave.setEnabled(false);
            }
        });
    }

    private void savePlace() {
        if (searchResult == null) return;

        String priceText = editPrice.getText().toString().trim();
        if (priceText.isEmpty()) {
            Toast.makeText(getContext(), "Fiyat gir", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        Map<String, Object> placeData = new HashMap<>();
        placeData.put("name", searchResult.name);
        placeData.put("category", spinnerCategory.getSelectedItem().toString());
        placeData.put("location", new GeoPoint(searchResult.lat, searchResult.lng));
        placeData.put("price", Double.parseDouble(priceText));
        placeData.put("effortLevel", Integer.parseInt(spinnerEffort.getSelectedItem().toString()));
        placeData.put("goodForCold", checkCold.isChecked());
        placeData.put("goodForHeat", checkHeat.isChecked());
        placeData.put("goodForRain", checkRain.isChecked());
        placeData.put("photoReference", searchResult.photoReference);

        FirebaseFirestore.getInstance()
                .collection("places")
                .add(placeData)
                .addOnSuccessListener(ref -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            searchResult.name + " Firestore'a eklendi!", Toast.LENGTH_LONG).show();
                    // Formu sıfırla
                    editSearch.setText("");
                    editPrice.setText("");
                    textFoundName.setText("");
                    btnSave.setEnabled(false);
                    searchResult = null;
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    Toast.makeText(getContext(), "Kayıt hatası: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}