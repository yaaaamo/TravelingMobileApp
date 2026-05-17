package com.example.traveling;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JourneyCache {

    private static final String PREFS_NAME = "journey_cache";
    private static final String KEY_JOURNEYS = "cached_journeys";
    private final SharedPreferences prefs;
    private final Gson gson;

    public JourneyCache(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }


    public void saveJourneys(List<Map<String, Object>> journeys) {
        String json = gson.toJson(journeys);
        prefs.edit().putString(KEY_JOURNEYS, json).apply();
    }

    public List<Map<String, Object>> loadJourneys() {
        String json = prefs.getString(KEY_JOURNEYS, null);
        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> result = gson.fromJson(json, type);
        return result != null ? result : new ArrayList<>();
    }


    public boolean hasCache() {
        return prefs.contains(KEY_JOURNEYS);
    }


    public void clearCache() {
        prefs.edit().remove(KEY_JOURNEYS).apply();
    }


    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}