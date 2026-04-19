package com.example.traveling;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PlaceRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface PlacesCallback {
        void onSuccess(List<Place> places);
        void onFailure(Exception e);
    }

    public void getAllPlaces(PlacesCallback callback) {
        db.collection("places")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Place> places = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Place place = doc.toObject(Place.class);
                        place.setId(doc.getId());
                        places.add(place);
                    }

                    callback.onSuccess(places);
                })
                .addOnFailureListener(callback::onFailure);
    }
}