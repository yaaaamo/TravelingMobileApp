package com.example.traveling;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DirectionsService {

    private static final String TAG = "DirectionsService";
    private static final String BASE_URL = "https://maps.googleapis.com/maps/api/directions/json";

    // in production put it in BuildConfig
    private static final String API_KEY = "AIzaSyDxCfUdpFdYXDoVqk91QhWDeRqf2XTOP8c";

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface RouteCallback {
        void onSuccess(RouteDetails details);
        void onFailure(Exception e);
    }

    public void getOptimizedRoute(LatLng origin, List<LatLng> waypoints,
                                  String mode, RouteCallback callback) {

        if (waypoints == null || waypoints.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Waypoints boş olamaz"));
            return;
        }

        String url = buildUrl(origin, waypoints, mode);
        Log.d(TAG, "Request URL: " + url);

        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error", e);
                mainHandler.post(() -> callback.onFailure(e));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onFailure(
                            new IOException("HTTP " + response.code())));
                    return;
                }

                String body = response.body().string();
                try {
                    RouteDetails details = parseResponse(body);
                    mainHandler.post(() -> callback.onSuccess(details));
                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }


    private String buildUrl(LatLng origin, List<LatLng> waypoints, String mode) {
        // Son waypoint'i destination olarak kullan
        LatLng destination = waypoints.get(waypoints.size() - 1);

        StringBuilder url = new StringBuilder(BASE_URL);
        url.append("?origin=").append(latLngToString(origin));
        url.append("&destination=").append(latLngToString(destination));

        if (waypoints.size() > 1) {
            url.append("&waypoints=optimize:true");
            for (int i = 0; i < waypoints.size() - 1; i++) {
                url.append("|").append(latLngToString(waypoints.get(i)));
            }
        }

        url.append("&mode=").append(mode);
        url.append("&key=").append(API_KEY);

        return url.toString();
    }

    private String latLngToString(LatLng point) {
        return point.latitude + "," + point.longitude;
    }


    private RouteDetails parseResponse(String jsonBody) throws Exception {
        JSONObject root = new JSONObject(jsonBody);

        // Status kontrolü
        String status = root.optString("status", "UNKNOWN");
        if (!"OK".equals(status)) {
            String errorMessage = root.optString("error_message", "Status: " + status);
            throw new IOException("Directions API error: " + errorMessage);
        }

        JSONArray routes = root.getJSONArray("routes");
        if (routes.length() == 0) {
            throw new IOException("No route found");
        }

        JSONObject route = routes.getJSONObject(0);

        // 1. Polyline'ı decode et
        String encodedPolyline = route
                .getJSONObject("overview_polyline")
                .getString("points");
        List<LatLng> polylinePoints = PolyUtil.decode(encodedPolyline);

        // 2. Optimize edilmiş waypoint sırası
        List<Integer> waypointOrder = new ArrayList<>();
        if (route.has("waypoint_order")) {
            JSONArray order = route.getJSONArray("waypoint_order");
            for (int i = 0; i < order.length(); i++) {
                waypointOrder.add(order.getInt(i));
            }
        }

        JSONArray legs = route.getJSONArray("legs");
        long totalDistanceMeters = 0;
        long totalDurationSeconds = 0;

        for (int i = 0; i < legs.length(); i++) {
            JSONObject leg = legs.getJSONObject(i);
            totalDistanceMeters += leg.getJSONObject("distance").getLong("value");
            totalDurationSeconds += leg.getJSONObject("duration").getLong("value");
        }

        double distanceKm = totalDistanceMeters / 1000.0;
        int durationMin = (int) (totalDurationSeconds / 60);

        return new RouteDetails(distanceKm, durationMin, polylinePoints, waypointOrder);
    }
}