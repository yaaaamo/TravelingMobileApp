package com.example.traveling;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SavedJourneysFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saved_journeys, container, false);

        LinearLayout listContainer = view.findViewById(R.id.saved_journeys_container);
        TextView emptyText = view.findViewById(R.id.text_empty);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return view;

        view.findViewById(R.id.btn_back_saved).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("savedJourneys")
                .orderBy("savedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        View cardView = inflater.inflate(R.layout.item_saved_journey,
                                listContainer, false);

                        TextView title = cardView.findViewById(R.id.text_saved_title);
                        TextView cost = cardView.findViewById(R.id.text_saved_cost);
                        TextView distance = cardView.findViewById(R.id.text_saved_distance);
                        TextView duration = cardView.findViewById(R.id.text_saved_duration);
                        TextView date = cardView.findViewById(R.id.text_saved_date);
                        TextView places = cardView.findViewById(R.id.text_saved_places);
                        Button btnLike = cardView.findViewById(R.id.btn_like_journey);
                        Button btnDislike = cardView.findViewById(R.id.btn_dislike_journey);

                        title.setText(doc.getString("title") + " Journey");

                        Double cost_ = doc.getDouble("totalCost");
                        cost.setText(cost_ != null ? String.format("💰 %.0f €", cost_) : "—");

                        String dist = doc.getString("distance");
                        distance.setText(dist != null ? "📍 " + dist : "—");

                        String dur = doc.getString("duration");
                        duration.setText(dur != null ? "⏱ " + dur : "—");

                        Timestamp ts = doc.getTimestamp("savedAt");
                        if (ts != null) {
                            String formatted = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .format(ts.toDate());
                            date.setText("🗓 " + formatted);
                        }

                        List<Map<String, Object>> placesList =
                                (List<Map<String, Object>>) doc.get("places");
                        if (placesList != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < placesList.size(); i++) {
                                sb.append(i + 1).append(". ")
                                        .append(placesList.get(i).get("name"));
                                if (i < placesList.size() - 1) sb.append("\n");
                            }
                            places.setText(sb.toString());
                        }


                        Boolean liked = doc.getBoolean("liked");
                        Boolean disliked = doc.getBoolean("disliked");

                        if (Boolean.TRUE.equals(liked)) {
                            btnLike.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            android.graphics.Color.parseColor("#4CAF50")));
                        } else if (Boolean.TRUE.equals(disliked)) {
                            btnDislike.setBackgroundTintList(
                                    android.content.res.ColorStateList.valueOf(
                                            android.graphics.Color.parseColor("#F44336")));
                        }

                        btnLike.setOnClickListener(v -> {
                            Boolean currentLiked = doc.getBoolean("liked");
                            boolean newLiked = !Boolean.TRUE.equals(currentLiked);

                            doc.getReference().update("liked", newLiked, "disliked", false)
                                    .addOnSuccessListener(unused -> {
                                        btnLike.setBackgroundTintList(
                                                android.content.res.ColorStateList.valueOf(
                                                        newLiked
                                                                ? android.graphics.Color.parseColor("#4CAF50")
                                                                : android.graphics.Color.parseColor("#E8F5E9")));
                                        btnDislike.setBackgroundTintList(
                                                android.content.res.ColorStateList.valueOf(
                                                        android.graphics.Color.parseColor("#FFEBEE")));
                                    });
                        });

                        btnDislike.setOnClickListener(v -> {
                            Boolean currentDisliked = doc.getBoolean("disliked");
                            boolean newDisliked = !Boolean.TRUE.equals(currentDisliked);

                            doc.getReference().update("disliked", newDisliked, "liked", false)
                                    .addOnSuccessListener(unused -> {
                                        btnDislike.setBackgroundTintList(
                                                android.content.res.ColorStateList.valueOf(
                                                        newDisliked
                                                                ? android.graphics.Color.parseColor("#F44336")
                                                                : android.graphics.Color.parseColor("#FFEBEE")));
                                        btnLike.setBackgroundTintList(
                                                android.content.res.ColorStateList.valueOf(
                                                        android.graphics.Color.parseColor("#E8F5E9")));
                                    });
                        });

                        cardView.findViewById(R.id.btn_delete_journey).setOnClickListener(v -> {
                            doc.getReference().delete()
                                    .addOnSuccessListener(unused -> {
                                        listContainer.removeView(cardView);
                                        if (listContainer.getChildCount() == 0) {
                                            emptyText.setVisibility(View.VISIBLE);
                                        }
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(requireContext(),
                                                    "Erreur: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show());
                        });


                        cardView.setOnClickListener(v -> {
                            JourneyPreviewBottomSheet sheet =
                                    JourneyPreviewBottomSheet.newInstance(doc);
                            sheet.show(getParentFragmentManager(), "journey_preview");
                        });

                        cardView.findViewById(R.id.btn_view_on_map).setOnClickListener(v -> {
                            List<Map<String, Object>> mapPlaces =
                                    (List<Map<String, Object>>) doc.get("places");
                            String title_ = doc.getString("title");

                            if (mapPlaces == null || mapPlaces.isEmpty()) {
                                Toast.makeText(requireContext(),
                                        "Aucune localisation disponible.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            Maps mapsFragment = Maps.newInstanceWithJourney(mapPlaces, title_);
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.fragment_container, mapsFragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        listContainer.addView(cardView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show());




        return view;
    }
}