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

public class WeatherService {

    private static final String TAG = "WeatherService";
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface WeatherCallback {
        void onSuccess(WeatherInfo info);
        void onFailure(Exception e);
    }


    public void getTodayForecast(double latitude, double longitude,
                                 WeatherCallback callback) {
        String url = BASE_URL
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&daily=temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code"
                + "&timezone=auto"
                + "&forecast_days=1";

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
                    WeatherInfo info = parseResponse(body);
                    mainHandler.post(() -> callback.onSuccess(info));
                } catch (Exception e) {
                    Log.e(TAG, "Parse error", e);
                    mainHandler.post(() -> callback.onFailure(e));
                }
            }
        });
    }


    private WeatherInfo parseResponse(String jsonBody) throws Exception {
        JSONObject root = new JSONObject(jsonBody);
        JSONObject daily = root.getJSONObject("daily");

        JSONArray tempMaxArr = daily.getJSONArray("temperature_2m_max");
        JSONArray tempMinArr = daily.getJSONArray("temperature_2m_min");
        JSONArray precipitationArr = daily.getJSONArray("precipitation_sum");
        JSONArray weatherCodeArr = daily.getJSONArray("weather_code");

        if (tempMaxArr.length() == 0) {
            throw new IOException("No weather data");
        }

        double tempMax = tempMaxArr.getDouble(0);
        double tempMin = tempMinArr.getDouble(0);
        double precipitation = precipitationArr.getDouble(0);
        int weatherCode = weatherCodeArr.getInt(0);

        return new WeatherInfo(tempMax, tempMin, precipitation, weatherCode);
    }
}