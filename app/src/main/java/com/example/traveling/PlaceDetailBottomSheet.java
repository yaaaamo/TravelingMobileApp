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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private static final String API_KEY = BuildConfig.GOOGLE_API_KEY;
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

        // Community posts
        LinearLayout communityPostsContainer = view.findViewById(R.id.community_posts_container);
        Object googlePlaceIdObj = placeData.get("googlePlaceId");
        fetchCommunityPosts(nameForSearch != null ? nameForSearch.toString() : null,
                googlePlaceIdObj != null ? googlePlaceIdObj.toString() : null,
                communityPostsContainer);

        return view;
    }

    private void fetchCommunityPosts(String placeName, String googlePlaceId, LinearLayout container) {
        if (getContext() == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (placeName != null && !placeName.isEmpty()) {
            db.collection("places")
                    .whereEqualTo("name", placeName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(placesSnapshot -> {
                        if (getActivity() == null) return;

                        if (!placesSnapshot.isEmpty()) {

                            String firestorePlaceId = placesSnapshot.getDocuments().get(0).getId();
                            android.util.Log.d("CommunityPosts", "Found place doc ID: " + firestorePlaceId);

                            db.collection("posts")
                                    .whereEqualTo("googlePlaceId", firestorePlaceId)
                                    .limit(10)
                                    .get()
                                    .addOnSuccessListener(postsSnapshot -> {
                                        if (getActivity() == null) return;
                                        android.util.Log.d("CommunityPosts", "Posts found: " + postsSnapshot.size());
                                        if (!postsSnapshot.isEmpty()) {
                                            showCommunityPosts(postsSnapshot.getDocuments(), container);
                                        } else {
                                            showNoPosts(container);
                                        }
                                    })
                                    .addOnFailureListener(e -> showNoPosts(container));
                        } else {
                            showNoPosts(container);
                        }
                    })
                    .addOnFailureListener(e -> showNoPosts(container));
        }
    }

    private void searchPostsByName(String placeName, LinearLayout container, FirebaseFirestore db) {
        android.util.Log.d("CommunityPosts", "Searching for placeName: " + placeName);
        if (getActivity() == null) return;

        db.collection("posts")
                .whereEqualTo("location", placeName)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (getActivity() == null) return;
                    if (!snapshot.isEmpty()) {
                        showCommunityPosts(snapshot.getDocuments(), container);
                    } else {
                        showNoPosts(container);
                    }
                })
                .addOnFailureListener(e -> showNoPosts(container));
    }

    private void showCommunityPosts(List<DocumentSnapshot> docs, LinearLayout container) {
        if (getActivity() == null || getContext() == null) return;

        getActivity().runOnUiThread(() -> {
            container.removeAllViews();
            for (DocumentSnapshot doc : docs) {
                View card = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_place_community_post, container, false);

                TextView username = card.findViewById(R.id.post_username);
                TextView caption  = card.findViewById(R.id.post_caption);
                ImageView postImg = card.findViewById(R.id.post_image);
                ImageView profileImg = card.findViewById(R.id.post_profile_pic);

                String u = doc.getString("username");
                String c = doc.getString("caption");
                String img = doc.getString("imageUrl");
                String prof = doc.getString("profilePicture");

                username.setText(u != null ? u : "Unknown");
                caption.setText(c != null ? c : "");

                if (img != null && !img.isEmpty()) {
                    Glide.with(requireContext()).load(img).centerCrop().into(postImg);
                }
                if (prof != null && !prof.isEmpty()) {
                    Glide.with(requireContext()).load(prof).circleCrop().into(profileImg);
                }

                container.addView(card);
            }
        });
    }

    private void showNoPosts(LinearLayout container) {
        if (getActivity() == null || getContext() == null) return;
        getActivity().runOnUiThread(() -> {
            TextView empty = new TextView(requireContext());
            empty.setText("No community posts for this place yet.");
            empty.setTextSize(13f);
            empty.setPadding(0, 8, 0, 8);
            container.addView(empty);
        });
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