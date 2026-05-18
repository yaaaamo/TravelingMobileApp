package com.example.traveling;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlacesApiService {

    private static final String TAG = "PlacesApiService";
    private static final String API_KEY = BuildConfig.GOOGLE_API_KEY;
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json";

    private final OkHttpClient client = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface PlaceSearchCallback {
        void onSuccess(PlaceSearchResult result);
        void onFailure(Exception e);
    }

    public static class PlaceSearchResult {
        public final String placeId;
        public final String name;
        public final String photoReference;
        public final double lat;
        public final double lng;

        public PlaceSearchResult(String placeId, String name, String photoReference, double lat, double lng) {
            this.placeId = placeId;
            this.name = name;
            this.photoReference = photoReference;
            this.lat = lat;
            this.lng = lng;
        }
    }

    public void searchPlace(String query, PlaceSearchCallback callback) {
        String url = BASE_URL
                + "?query=" + query.replace(" ", "+")
                + "&key=" + API_KEY;

        Log.d(TAG, "Searching: " + url);

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                try {
                    JSONObject root = new JSONObject(body);
                    String status = root.getString("status");

                    if (!"OK".equals(status)) {
                        mainHandler.post(() -> callback.onFailure(
                                new Exception("API error: " + status)));
                        return;
                    }

                    JSONArray results = root.getJSONArray("results");
                    if (results.length() == 0) {
                        mainHandler.post(() -> callback.onFailure(
                                new Exception("No results found")));
                        return;
                    }

                    JSONObject first = results.getJSONObject(0);
                    String placeId = first.getString("place_id");
                    String name = first.getString("name");


                    String photoRef = null;
                    if (first.has("photos")) {
                        photoRef = first.getJSONArray("photos")
                                .getJSONObject(0)
                                .getString("photo_reference");
                    }


                    JSONObject location = first
                            .getJSONObject("geometry")
                            .getJSONObject("location");
                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");

                    PlaceSearchResult result = new PlaceSearchResult(placeId, name, photoRef, lat, lng);
                    mainHandler.post(() -> callback.onSuccess(result));

                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }
}