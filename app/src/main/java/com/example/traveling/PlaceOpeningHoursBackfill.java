package com.example.traveling;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceOpeningHoursBackfill {

    private static final String TAG = "OpeningHoursBackfill";

    private final Context context;
    private final FirebaseFirestore db;
    private final RequestQueue queue;
    private final String apiKey;

    public PlaceOpeningHoursBackfill(Context context, String apiKey) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.queue = Volley.newRequestQueue(this.context);
        this.apiKey = apiKey;
    }

    public void run() {
        db.collection("places")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Log.d(TAG, "Places found: " + snapshot.size());

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String name = doc.getString("name");
                        GeoPoint location = doc.getGeoPoint("location");
                        String existingPlaceId = doc.getString("googlePlaceId");

                        if (existingPlaceId != null && !existingPlaceId.isEmpty()) {
                            fetchDetailsAndUpdate(doc.getId(), existingPlaceId);
                        } else {
                            findPlaceIdThenUpdate(doc.getId(), name, location);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Log.e(TAG, "Firestore places fetch failed", e)
                );
    }

    private void findPlaceIdThenUpdate(String firestoreDocId, String name, GeoPoint location) {
        if (name == null || location == null) {
            Log.w(TAG, "Missing name/location for doc: " + firestoreDocId);
            return;
        }

        String input = name;
        String locationBias = "circle:2000@" + location.getLatitude() + "," + location.getLongitude();

        Uri uri = Uri.parse("https://maps.googleapis.com/maps/api/place/findplacefromtext/json")
                .buildUpon()
                .appendQueryParameter("input", input)
                .appendQueryParameter("inputtype", "textquery")
                .appendQueryParameter("fields", "place_id,name")
                .appendQueryParameter("locationbias", locationBias)
                .appendQueryParameter("key", apiKey)
                .build();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                uri.toString(),
                null,
                response -> {
                    try {
                        JSONArray candidates = response.optJSONArray("candidates");

                        if (candidates == null || candidates.length() == 0) {
                            Log.w(TAG, "No Google place found for: " + name);
                            return;
                        }

                        JSONObject first = candidates.getJSONObject(0);
                        String googlePlaceId = first.optString("place_id", null);

                        if (googlePlaceId == null || googlePlaceId.isEmpty()) {
                            Log.w(TAG, "No place_id for: " + name);
                            return;
                        }

                        db.collection("places")
                                .document(firestoreDocId)
                                .update("googlePlaceId", googlePlaceId)
                                .addOnSuccessListener(unused ->
                                        fetchDetailsAndUpdate(firestoreDocId, googlePlaceId)
                                )
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Failed to save googlePlaceId", e)
                                );

                    } catch (Exception e) {
                        Log.e(TAG, "findPlaceId parse error", e);
                    }
                },
                error -> Log.e(TAG, "findPlaceId request failed", error)
        );

        queue.add(request);
    }

    private void fetchDetailsAndUpdate(String firestoreDocId, String googlePlaceId) {
        Uri uri = Uri.parse("https://maps.googleapis.com/maps/api/place/details/json")
                .buildUpon()
                .appendQueryParameter("place_id", googlePlaceId)
                .appendQueryParameter("fields", "opening_hours")
                .appendQueryParameter("key", apiKey)
                .build();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                uri.toString(),
                null,
                response -> {
                    try {
                        JSONObject result = response.optJSONObject("result");

                        if (result == null) {
                            Log.w(TAG, "No result for placeId: " + googlePlaceId);
                            return;
                        }

                        JSONObject openingHours = result.optJSONObject("opening_hours");

                        if (openingHours == null) {
                            Log.w(TAG, "No opening_hours for placeId: " + googlePlaceId);
                            return;
                        }

                        Boolean openNow = openingHours.has("open_now")
                                ? openingHours.optBoolean("open_now")
                                : null;

                        List<String> weekdayText = new ArrayList<>();
                        JSONArray weekdayArray = openingHours.optJSONArray("weekday_text");
                        if (weekdayArray != null) {
                            for (int i = 0; i < weekdayArray.length(); i++) {
                                weekdayText.add(weekdayArray.optString(i));
                            }
                        }

                        List<Map<String, Object>> openingPeriods = new ArrayList<>();
                        JSONArray periodsArray = openingHours.optJSONArray("periods");

                        if (periodsArray != null) {
                            for (int i = 0; i < periodsArray.length(); i++) {
                                JSONObject periodObj = periodsArray.getJSONObject(i);

                                Map<String, Object> periodMap = new HashMap<>();

                                JSONObject openObj = periodObj.optJSONObject("open");
                                JSONObject closeObj = periodObj.optJSONObject("close");

                                if (openObj != null) {
                                    Map<String, Object> openMap = new HashMap<>();
                                    openMap.put("day", openObj.optInt("day"));
                                    openMap.put("time", openObj.optString("time"));
                                    periodMap.put("open", openMap);
                                }

                                if (closeObj != null) {
                                    Map<String, Object> closeMap = new HashMap<>();
                                    closeMap.put("day", closeObj.optInt("day"));
                                    closeMap.put("time", closeObj.optString("time"));
                                    periodMap.put("close", closeMap);
                                }

                                openingPeriods.add(periodMap);
                            }
                        }

                        Map<String, Object> update = new HashMap<>();
                        update.put("googlePlaceId", googlePlaceId);
                        update.put("openNow", openNow);
                        update.put("weekdayText", weekdayText);
                        update.put("openingPeriods", openingPeriods);

                        db.collection("places")
                                .document(firestoreDocId)
                                .update(update)
                                .addOnSuccessListener(unused ->
                                        Log.d(TAG, "Updated opening hours: " + firestoreDocId)
                                )
                                .addOnFailureListener(e ->
                                        Log.e(TAG, "Firestore update failed", e)
                                );

                    } catch (Exception e) {
                        Log.e(TAG, "details parse error", e);
                    }
                },
                error -> Log.e(TAG, "details request failed", error)
        );

        queue.add(request);
    }
}
