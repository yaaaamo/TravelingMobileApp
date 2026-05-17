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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LikedJourneysFragment extends Fragment {

    private LinearLayout container;
    private TextView emptyText;
    private Button btnTabLiked, btnTabDisliked;
    private boolean showingLiked = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_liked_journeys, parent, false);

        container = view.findViewById(R.id.liked_journeys_container);
        emptyText = view.findViewById(R.id.text_empty_liked);
        btnTabLiked = view.findViewById(R.id.btn_tab_liked);
        btnTabDisliked = view.findViewById(R.id.btn_tab_disliked);

        view.findViewById(R.id.btn_back_liked).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        btnTabLiked.setOnClickListener(v -> {
            showingLiked = true;
            btnTabLiked.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#4CAF50")));
            btnTabDisliked.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#BDBDBD")));
            loadReactions(true);
        });

        btnTabDisliked.setOnClickListener(v -> {
            showingLiked = false;
            btnTabDisliked.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#F44336")));
            btnTabLiked.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                            android.graphics.Color.parseColor("#BDBDBD")));
            loadReactions(false);
        });

        loadReactions(true);

        return view;
    }

    private void loadReactions(boolean liked) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || user.isAnonymous()) {
            emptyText.setText("Connectez-vous pour voir vos réactions.");
            emptyText.setVisibility(View.VISIBLE);
            return;
        }

        container.removeAllViews();
        emptyText.setVisibility(View.GONE);

        FirebaseFirestore.getInstance()
                .collection("Users")
                .document(user.getUid())
                .collection("savedJourneys")
                .whereEqualTo(liked ? "liked" : "disliked", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        emptyText.setText(liked
                                ? "No liked journeys yet."
                                : "No disliked journeys yet.");
                        emptyText.setVisibility(View.VISIBLE);
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        View cardView = getLayoutInflater().inflate(
                                R.layout.item_liked_journey, container, false);

                        TextView emoji = cardView.findViewById(R.id.text_liked_emoji);
                        TextView title = cardView.findViewById(R.id.text_liked_title);
                        TextView date = cardView.findViewById(R.id.text_liked_date);
                        TextView places = cardView.findViewById(R.id.text_liked_places);

                        emoji.setText(liked ? "👍" : "👎");

                        String routeTitle = doc.getString("title");
                        title.setText(routeTitle != null ? routeTitle + " Journey" : "Journey");

                        com.google.firebase.Timestamp ts = doc.getTimestamp("savedAt");
                        if (ts != null) {
                            String formatted = new SimpleDateFormat(
                                    "dd MMM yyyy", Locale.getDefault())
                                    .format(ts.toDate());
                            date.setText("🗓 " + formatted);
                        }


                        List<Map<String, Object>> placesList =
                                (List<Map<String, Object>>) doc.get("places");
                        if (placesList != null && places != null) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < placesList.size(); i++) {
                                sb.append(i + 1).append(". ")
                                        .append(placesList.get(i).get("name"));
                                if (i < placesList.size() - 1) sb.append("\n");
                            }
                            places.setText(sb.toString());
                        }

                        container.addView(cardView);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Erreur: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}