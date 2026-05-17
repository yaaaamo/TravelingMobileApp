package com.example.traveling;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JourneyPreviewBottomSheet extends BottomSheetDialogFragment {

    private QueryDocumentSnapshot doc;

    public static JourneyPreviewBottomSheet newInstance(QueryDocumentSnapshot doc) {
        JourneyPreviewBottomSheet sheet = new JourneyPreviewBottomSheet();
        sheet.doc = doc;
        return sheet;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_journey_preview, container, false);

        TextView title = view.findViewById(R.id.preview_title);
        TextView cost = view.findViewById(R.id.preview_cost);
        TextView distance = view.findViewById(R.id.preview_distance);
        TextView date = view.findViewById(R.id.preview_date);
        LinearLayout placesContainer = view.findViewById(R.id.preview_places_container);

        // Başlık
        String routeTitle = doc.getString("title");
        title.setText(routeTitle != null ? routeTitle + " Journey" : "Journey");

        // Maliyet
        Double cost_ = doc.getDouble("totalCost");
        cost.setText(cost_ != null ? String.format("💰 %.0f €", cost_) : "—");

        // Mesafe
        String dist = doc.getString("distance");
        distance.setText(dist != null ? "📍 " + dist : "—");

        // Tarih
        Timestamp ts = doc.getTimestamp("savedAt");
        if (ts != null) {
            String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(ts.toDate());
            date.setText("🗓 " + formatted);
        }

        // Mekânlar
        List<Map<String, Object>> placesList =
                (List<Map<String, Object>>) doc.get("places");

        if (placesList != null) {
            for (Map<String, Object> place : placesList) {
                View placeView = inflater.inflate(
                        R.layout.item_preview_place, placesContainer, false);

                TextView creneau = placeView.findViewById(R.id.preview_place_creneau);
                TextView name = placeView.findViewById(R.id.preview_place_name);
                TextView category = placeView.findViewById(R.id.preview_place_category);
                TextView price = placeView.findViewById(R.id.preview_place_price);

                Object creneauObj = place.get("creneau");
                Object timeObj = place.get("time");
                if (creneauObj != null) {
                    String creneauStr = creneauObj.toString();
                    String timeStr = timeObj != null ? " • " + timeObj.toString() : "";
                    creneau.setText(creneauStr + timeStr);
                }

                Object nameObj = place.get("name");
                name.setText(nameObj != null ? nameObj.toString() : "—");

                Object categoryObj = place.get("category");
                if (categoryObj != null) {
                    switch (categoryObj.toString()) {
                        case "culture": category.setText("🏛 Culture"); break;
                        case "restauration": category.setText("🍽 Restauration"); break;
                        case "loisirs": category.setText("🎭 Loisirs"); break;
                        case "decouvertes": category.setText("🔍 Découvertes"); break;
                        default: category.setText(categoryObj.toString());
                    }
                }

                Object priceObj = place.get("price");
                price.setText(priceObj != null
                        ? String.format("%.0f €", ((Number) priceObj).doubleValue())
                        : "—");

                placesContainer.addView(placeView);
            }
        }

        return view;
    }
}