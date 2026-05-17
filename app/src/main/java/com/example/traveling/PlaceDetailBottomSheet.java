package com.example.traveling;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PlaceDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String API_KEY = "AIzaSyDxCfUdpFdYXDoVqk91QhWDeRqf2XTOP8c";
    private Map<String, Object> placeData;
    private final OkHttpClient client = new OkHttpClient();

    public static PlaceDetailBottomSheet newInstance(Map<String, Object> place) {
        PlaceDetailBottomSheet sheet = new PlaceDetailBottomSheet();
        sheet.placeData = place;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(
                R.layout.bottom_sheet_place_detail, container, false);

        TextView name = view.findViewById(R.id.place_detail_name);
        TextView category = view.findViewById(R.id.place_detail_category);
        TextView creneau = view.findViewById(R.id.place_detail_creneau);
        TextView price = view.findViewById(R.id.place_detail_price);
        LinearLayout photosContainer = view.findViewById(R.id.photos_container);
        LinearLayout videosContainer = view.findViewById(R.id.videos_container);


        Object nameObj = placeData.get("name");
        name.setText(nameObj != null ? nameObj.toString() : "—");

        Object categoryObj = placeData.get("category");
        if (categoryObj != null) {
            switch (categoryObj.toString()) {
                case "culture": category.setText("🏛 Culture"); break;
                case "restauration": category.setText("🍽 Restauration"); break;
                case "loisirs": category.setText("🎭 Loisirs"); break;
                case "decouvertes": category.setText("🔍 Découvertes"); break;
                default: category.setText(categoryObj.toString());
            }
        }

        Object creneauObj = placeData.get("creneau");
        Object timeObj = placeData.get("time");
        if (creneauObj != null) {
            creneau.setText(creneauObj.toString() +
                    (timeObj != null ? " • " + timeObj : ""));
        }

        Object priceObj = placeData.get("price");
        if (priceObj instanceof Number) {
            price.setText(String.format("💰 %.0f €",
                    ((Number) priceObj).doubleValue()));
        }


        Object photoRefObj = placeData.get("photoReference");
        if (photoRefObj != null && !photoRefObj.toString().isEmpty()) {
            addPhoto(photosContainer, photoRefObj.toString());
        }


        Object videoUrlsObj = placeData.get("videoUrls");
        if (videoUrlsObj instanceof List) {
            List<String> videoUrls = (List<String>) videoUrlsObj;
            for (String videoUrl : videoUrls) {
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    addVideoLink(videosContainer, videoUrl);
                }
            }
        }


        Object nameForSearch = placeData.get("name");
        if (nameForSearch != null) {
            fetchMorePhotos(nameForSearch.toString(), photosContainer);
        }

        return view;
    }

    private void fetchMorePhotos(String placeName, LinearLayout container) {
        JourneyCache cache = new JourneyCache(requireContext());


        if (cache.hasPlacePhotos(placeName)) {
            List<String> cachedRefs = cache.loadPlacePhotos(placeName);
            for (String ref : cachedRefs) {
                addPhoto(container, ref);
            }
            return;
        }


        String url = "https://maps.googleapis.com/maps/api/place/textsearch/json"
                + "?query=" + placeName.replace(" ", "+")
                + "&key=" + API_KEY;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;

                try {
                    String body = response.body().string();
                    JSONObject root = new JSONObject(body);
                    JSONArray results = root.getJSONArray("results");
                    if (results.length() == 0) return;

                    String placeId = results.getJSONObject(0).optString("place_id");
                    if (!placeId.isEmpty()) {
                        fetchPlacePhotos(placeId, placeName, container, cache);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void fetchPlacePhotos(String placeId, String placeName,
                                  LinearLayout container, JourneyCache cache) {
        String url = "https://maps.googleapis.com/maps/api/place/details/json"
                + "?place_id=" + placeId
                + "&fields=photos"
                + "&key=" + API_KEY;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call,
                                   @NonNull Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;

                try {
                    String body = response.body().string();
                    JSONObject root = new JSONObject(body);
                    JSONObject result = root.optJSONObject("result");
                    if (result == null) return;

                    JSONArray photos = result.optJSONArray("photos");
                    if (photos == null) return;

                    int maxPhotos = Math.min(photos.length(), 5);


                    List<String> photoRefs = new ArrayList<>();

                    for (int i = 1; i < maxPhotos; i++) {
                        String photoRef = photos.getJSONObject(i)
                                .optString("photo_reference");
                        if (!photoRef.isEmpty()) {
                            photoRefs.add(photoRef);
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() ->
                                        addPhoto(container, photoRef));
                            }
                        }
                    }


                    cache.savePlacePhotos(placeName, photoRefs);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addPhoto(LinearLayout container, String photoReference) {
        if (getContext() == null) return;

        String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                + "?maxwidth=800"
                + "&photo_reference=" + photoReference
                + "&key=" + API_KEY;

        ImageView imageView = new ImageView(requireContext());
        int size = (int) (280 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(size, size);
        params.setMargins(0, 0, margin, 0);
        imageView.setLayoutParams(params);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        Glide.with(requireContext())
                .load(photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(imageView);

        container.addView(imageView);
    }

    private void addVideoLink(LinearLayout container, String videoUrl) {
        if (getContext() == null) return;

        TextView videoLink = new TextView(requireContext());
        videoLink.setText("▶ Watch video");
        videoLink.setTextSize(14);
        videoLink.setTextColor(
                android.graphics.Color.rgb(46, 125, 50));
        videoLink.setPadding(0, 8, 0, 8);
        videoLink.setOnClickListener(v -> {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW, Uri.parse(videoUrl));
            startActivity(intent);
        });

        container.addView(videoLink);
    }
}