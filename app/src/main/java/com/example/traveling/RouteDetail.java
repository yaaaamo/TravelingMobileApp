package com.example.traveling;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouteDetail extends Fragment {

    private static final String ARG_PLAN_INDEX = "plan_index";
    private LinearLayout timelineContainer;
    private LayoutInflater savedInflater;

    private TravelPathViewModel viewModel;

    private RouteOption currentOption;

    private boolean isLiked = false;
    private boolean isDisliked = false;


    private static class TimeSlot {
        String label;
        String time;
        int startMinutes;
        int endMinutes;

        TimeSlot(String label, String time) {
            this.label = label;
            this.time = time;
            this.startMinutes = 0;
            this.endMinutes = 0;
        }

        TimeSlot(String label, String time, int startMinutes, int endMinutes) {
            this.label = label;
            this.time = time;
            this.startMinutes = startMinutes;
            this.endMinutes = endMinutes;
        }
    }

    private static class ScheduledStop {
        Place place;
        TimeSlot slot;

        ScheduledStop(Place place, TimeSlot slot) {
            this.place = place;
            this.slot = slot;
        }
    }

    public static RouteDetail newInstance(int planIndex) {
        RouteDetail fragment = new RouteDetail();
        Bundle args = new Bundle();
        args.putInt(ARG_PLAN_INDEX, planIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_detail, container, false);
        savedInflater = inflater;

        int planIndex = getArguments() != null
                ? getArguments().getInt(ARG_PLAN_INDEX, 0) : 0;

        viewModel =
                new ViewModelProvider(requireActivity())
                        .get(TravelPathViewModel.class);

        List<RouteOption> options = viewModel.getRouteOptions().getValue();
        if (options == null || planIndex >= options.size()) return view;

        timelineContainer = view.findViewById(R.id.timeline_container);

        TextView statPrice = view.findViewById(R.id.text_stat_price);
        TextView statEffort = view.findViewById(R.id.text_stat_effort);
        TextView statKm = view.findViewById(R.id.text_stat_km);
        TextView title = view.findViewById(R.id.text_journey_title);
        TextView description = view.findViewById(R.id.text_journey_description);

        currentOption = options.get(planIndex);
        setupMiniMap(currentOption);
        renderRoute(view, currentOption, title, description, statPrice, statEffort, statKm);
        setupWeather(view, currentOption);

        Button btnRegenerate = view.findViewById(R.id.btn_regenerate);

        viewModel.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            btnRegenerate.setEnabled(!isLoading);
            btnRegenerate.setText(isLoading ? "Loading..." : "🔄 Regenerate");
        });

        viewModel.getRouteOptions().observe(getViewLifecycleOwner(), updatedOptions -> {
            if (updatedOptions == null || planIndex >= updatedOptions.size()) return;
            if (savedInflater == null) return;
            currentOption = updatedOptions.get(planIndex); // ← currentOption güncelleniyor
            renderRoute(view, currentOption, title, description, statPrice, statEffort, statKm);
        });

        btnRegenerate.setOnClickListener(v ->
                showRegenerateDialog(planIndex)
        );

        Button btnSaveJourney = view.findViewById(R.id.btn_save_journey);
        Button btnExportPdf = view.findViewById(R.id.btn_export_pdf);
        Button btnShare = view.findViewById(R.id.btn_share_journey);


        if (!JourneyCache.isOnline(requireContext())) {
            btnSaveJourney.setVisibility(View.GONE);

        } else {
            btnSaveJourney.setOnClickListener(v ->
                    saveJourney(currentOption, btnSaveJourney));
        }

        btnExportPdf.setOnClickListener(v -> exportAsPdf(currentOption));
        btnShare.setOnClickListener(v -> shareJourney(currentOption));

        return view;
    }


    private void showRegenerateDialog(int planIndex) {
        android.app.AlertDialog.Builder builder =
                new android.app.AlertDialog.Builder(requireContext());
        builder.setTitle("🔄 Ajuster le parcours");

        View dialogView = getLayoutInflater().inflate(
                R.layout.dialog_regenerate_preferences, null);
        builder.setView(dialogView);

        android.widget.CheckBox checkCulture =
                dialogView.findViewById(R.id.check_regen_culture);
        android.widget.CheckBox checkRestauration =
                dialogView.findViewById(R.id.check_regen_restauration);
        android.widget.CheckBox checkLoisirs =
                dialogView.findViewById(R.id.check_regen_loisirs);
        android.widget.CheckBox checkDecouvertes =
                dialogView.findViewById(R.id.check_regen_decouvertes);
        android.widget.SeekBar seekBudget =
                dialogView.findViewById(R.id.seekbar_regen_budget);
        android.widget.TextView textBudget =
                dialogView.findViewById(R.id.text_regen_budget);

        seekBudget.setMax(200);
        seekBudget.setProgress(100);
        textBudget.setText("Budget : 100 €");

        seekBudget.setOnSeekBarChangeListener(
                new android.widget.SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(android.widget.SeekBar seekBar,
                                                  int progress, boolean fromUser) {
                        textBudget.setText("Budget : " + progress + " €");
                    }
                    @Override public void onStartTrackingTouch(android.widget.SeekBar s) {}
                    @Override public void onStopTrackingTouch(android.widget.SeekBar s) {}
                });

        builder.setPositiveButton("Régénérer", (dialog, which) -> {
            UserPreferences adjustedPrefs = new UserPreferences();
            adjustedPrefs.culture = checkCulture.isChecked();
            adjustedPrefs.restauration = checkRestauration.isChecked();
            adjustedPrefs.loisirs = checkLoisirs.isChecked();
            adjustedPrefs.decouvertes = checkDecouvertes.isChecked();
            adjustedPrefs.maxBudget = seekBudget.getProgress();
            adjustedPrefs.effort = "Moyen";
            adjustedPrefs.duration = "3";

            if (!adjustedPrefs.culture && !adjustedPrefs.restauration
                    && !adjustedPrefs.loisirs && !adjustedPrefs.decouvertes) {
                adjustedPrefs.culture = true;
                adjustedPrefs.restauration = true;
                adjustedPrefs.loisirs = true;
                adjustedPrefs.decouvertes = true;
            }

            viewModel.regenerateSingleRouteWithPrefs(planIndex, adjustedPrefs);
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    private void shareJourney(RouteOption option) {
        StringBuilder sb = new StringBuilder();
        sb.append("🗺 Mon parcours : ").append(option.getTitle()).append("\n\n");

        List<ScheduledStop> schedule = generateOpeningAwareSchedule(option, option.getPlaces());

        for (int i = 0; i < schedule.size(); i++) {
            ScheduledStop stop = schedule.get(i);
            sb.append(stop.slot.label)
                    .append(" • ")
                    .append(stop.slot.time)
                    .append("\n")
                    .append(i + 1)
                    .append(". ")
                    .append(stop.place.getName())
                    .append(" (")
                    .append(stop.place.getPrice())
                    .append(" €)\n\n");
        }

        RouteDetails details = option.getRouteDetails();
        if (details != null) {
            sb.append("📍 Distance : ").append(details.getFormattedDistance()).append("\n");
        }
        sb.append("💰 Total : ").append(String.format("%.0f €", option.getTotalEstimatedCost()));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mon parcours " + option.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        startActivity(Intent.createChooser(shareIntent, "Partager via..."));
    }

    private void exportAsPdf(RouteOption option) {

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint titlePaint = new Paint();
        Paint textPaint = new Paint();
        Paint linePaint = new Paint();

        titlePaint.setTextSize(24f);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(Color.BLACK);
        canvas.drawText("Your " + option.getTitle() + " Journey", 40, 60, titlePaint);

        linePaint.setColor(Color.GRAY);
        linePaint.setStrokeWidth(1f);
        canvas.drawLine(40, 75, 555, 75, linePaint);

        textPaint.setTextSize(13f);
        textPaint.setColor(Color.DKGRAY);

        RouteDetails details = option.getRouteDetails();
        String distance = details != null ? details.getFormattedDistance() : "—";
        String walkingDuration = details != null ? details.getFormattedDuration() : "—";
        String journeyDuration = option.getFormattedSelectedDuration();

        canvas.drawText("Total Cost: " + String.format("%.0f €", option.getTotalEstimatedCost()), 40, 105, textPaint);
        canvas.drawText("Distance: " + distance, 40, 125, textPaint);
        canvas.drawText("Journey Duration: " + journeyDuration, 40, 145, textPaint);
        canvas.drawText("Walking Time: " + walkingDuration, 40, 165, textPaint);

        canvas.drawLine(40, 180, 555, 180, linePaint);

        Paint boldPaint = new Paint();
        boldPaint.setTextSize(13f);
        boldPaint.setFakeBoldText(true);
        boldPaint.setColor(Color.BLACK);

        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(12f);

        int y = 205;

        List<ScheduledStop> schedule = generateOpeningAwareSchedule(option, option.getPlaces());

        for (int i = 0; i < schedule.size(); i++) {
            ScheduledStop stop = schedule.get(i);
            Place place = stop.place;
            TimeSlot slot = stop.slot;

            canvas.drawText((i + 1) + ". " + place.getName(), 40, y, boldPaint);
            canvas.drawText(slot.label + " " + slot.time + "  |  " + place.getCategory() + "  |  " + place.getPrice() + " €", 60, y + 18, textPaint);

            String openingWarning = getOpeningWarning(place, slot.startMinutes, slot.endMinutes);
            if (!openingWarning.isEmpty()) {
                canvas.drawText(openingWarning, 60, y + 36, textPaint);
                y += 65;
            } else {
                y += 50;
            }

            if (y > 800) break;
        }

        document.finishPage(page);

        String fileName = "Journey_" + option.getTitle() + "_" + System.currentTimeMillis() + ".pdf";

        try {
            File pdfFile;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.IS_PENDING, 1);

                ContentResolver resolver = requireContext().getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream out = resolver.openOutputStream(uri)) {
                        document.writeTo(out);
                    }
                    values.clear();
                    values.put(MediaStore.Downloads.IS_PENDING, 0);
                    resolver.update(uri, values, null, null);
                    Toast.makeText(getContext(), "PDF saved to Downloads!", Toast.LENGTH_LONG).show();
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                pdfFile = new File(downloadsDir, fileName);
                document.writeTo(new FileOutputStream(pdfFile));
                Toast.makeText(getContext(), "PDF saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("PDF", "Export failed", e);
        } finally {
            document.close();
        }
    }

    private void saveJourney(RouteOption option, Button btn) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "Connectez-vous pour sauvegarder.", Toast.LENGTH_SHORT).show();
            return;
        }

        btn.setEnabled(false);
        btn.setText("Saving...");

        List<ScheduledStop> schedule = generateOpeningAwareSchedule(option, option.getPlaces());

        List<Map<String, Object>> placesData = new ArrayList<>();
        for (ScheduledStop stop : schedule) {
            Place place = stop.place;
            TimeSlot slot = stop.slot;

            Map<String, Object> p = new HashMap<>();
            p.put("name", place.getName());
            p.put("category", place.getCategory());
            p.put("price", place.getPrice());
            p.put("photoReference", place.getPhotoReference());
            p.put("videoUrls", place.getVideoUrls());
            p.put("creneau", slot.label);
            p.put("time", slot.time);

            if (place.getLocation() != null) {
                p.put("lat", place.getLocation().getLatitude());
                p.put("lng", place.getLocation().getLongitude());
            }

            placesData.add(p);
        }

        Map<String, Object> journeyData = new HashMap<>();
        journeyData.put("title", option.getTitle());
        journeyData.put("totalCost", option.getTotalEstimatedCost());
        journeyData.put("savedAt", com.google.firebase.Timestamp.now());
        journeyData.put("places", placesData);
        journeyData.put("liked", false);
        journeyData.put("disliked", false);

        RouteDetails details = option.getRouteDetails();
        if (details != null) {
            journeyData.put("distance", details.getFormattedDistance());
            journeyData.put("duration", details.getFormattedDuration());
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .collection("savedJourneys")
                .add(journeyData)
                .addOnSuccessListener(ref -> {
                    btn.setEnabled(true);
                    btn.setText("✅ Saved!");
                })
                .addOnFailureListener(e -> {
                    btn.setEnabled(true);
                    btn.setText("💾 Save");
                    Toast.makeText(requireContext(), "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void renderRoute(View view, RouteOption option,
                             TextView title, TextView description,
                             TextView statPrice, TextView statEffort, TextView statKm) {

        if (savedInflater == null || timelineContainer == null) return;


        String API_KEY = "AIzaSyDxCfUdpFdYXDoVqk91QhWDeRqf2XTOP8c";

        title.setText("Your " + option.getTitle() + " Escape");
        description.setText("A curated journey through " + option.getPlaces().size()
                + " unique places, balancing culture, taste, and discovery.");

        timelineContainer.removeAllViews();

        List<ScheduledStop> schedule = generateOpeningAwareSchedule(option, option.getPlaces());

        for (int i = 0; i < schedule.size(); i++) {
            try {
                ScheduledStop stop = schedule.get(i);
                Place place = stop.place;
                TimeSlot slot = stop.slot;

                View itemView = savedInflater.inflate(R.layout.item_timeline_place,
                        timelineContainer, false);

                ((TextView) itemView.findViewById(R.id.text_time_label)).setText(slot.label);
                ((TextView) itemView.findViewById(R.id.text_time)).setText(slot.time);
                ((TextView) itemView.findViewById(R.id.text_place_name)).setText(place.getName());

                String placeDescription = getGenericDescription(place);
                String openingWarning = getOpeningWarning(place, slot.startMinutes, slot.endMinutes);

                if (!openingWarning.isEmpty()) {
                    placeDescription += "\n" + openingWarning;
                }

                ((TextView) itemView.findViewById(R.id.text_place_description))
                        .setText(placeDescription);

                if (place.getPhotoReference() != null && !place.getPhotoReference().isEmpty()) {
                    String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                            + "?maxwidth=800"
                            + "&photo_reference=" + place.getPhotoReference()
                            + "&key=" + API_KEY;
                    Glide.with(requireContext())
                            .load(photoUrl)
                            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .centerCrop()
                            .into((ImageView) itemView.findViewById(R.id.image_place));
                }

                LinearLayout videoLinksLayout = itemView.findViewById(R.id.layout_video_links);

                if (videoLinksLayout != null) {
                    videoLinksLayout.removeAllViews();

                    List<String> videoUrls = place.getVideoUrls();

                    if (videoUrls != null && !videoUrls.isEmpty()) {
                        videoLinksLayout.setVisibility(View.VISIBLE);

                        for (int videoIndex = 0; videoIndex < videoUrls.size(); videoIndex++) {
                            String videoUrl = videoUrls.get(videoIndex);

                            if (videoUrl == null || videoUrl.trim().isEmpty()) {
                                continue;
                            }

                            TextView videoLink = new TextView(requireContext());
                            videoLink.setText("▶ Watch video " + (videoIndex + 1));
                            videoLink.setTextSize(14);
                            videoLink.setTextColor(Color.rgb(46, 125, 50));
                            videoLink.setPadding(0, 6, 0, 6);

                            videoLink.setOnClickListener(v -> {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                                startActivity(intent);
                            });

                            videoLinksLayout.addView(videoLink);
                        }

                        if (videoLinksLayout.getChildCount() == 0) {
                            videoLinksLayout.setVisibility(View.GONE);
                        }
                    } else {
                        videoLinksLayout.setVisibility(View.GONE);
                    }
                }

                timelineContainer.addView(itemView);

            } catch (Exception e) {
                android.util.Log.e("RouteDetail", "Timeline error at index " + i, e);
            }
        }

        statPrice.setText(String.format("%.0f €", option.getTotalEstimatedCost()));

        List<Place> places = option.getPlaces();
        if (!places.isEmpty()) {
            double avg = 0;
            for (Place p : places) avg += p.getEffortLevel();
            avg = avg / places.size();
            statEffort.setText(String.format("%.1f / 3", avg));
        }

        RouteDetails details = option.getRouteDetails();
        statKm.setText(details != null ? details.getFormattedDistance() : "—");

        updateMap(option);
    }

    private List<ScheduledStop> generateOpeningAwareSchedule(RouteOption option, List<Place> originalPlaces) {
        List<ScheduledStop> bestSchedule = new ArrayList<>();

        if (originalPlaces == null || originalPlaces.isEmpty()) {
            return bestSchedule;
        }

        int totalDurationMinutes = extractDurationMinutes(option);

        if (totalDurationMinutes <= 0) {
            totalDurationMinutes = originalPlaces.size() * 60;
        }

        int minutesPerPlace = Math.max(45, totalDurationMinutes / originalPlaces.size());

        int plannedDuration = minutesPerPlace * originalPlaces.size();

        if (plannedDuration > 15 * 60) {
            minutesPerPlace = (15 * 60) / originalPlaces.size();
            plannedDuration = minutesPerPlace * originalPlaces.size();
        }

        int latestStart = 24 * 60 - plannedDuration;
        if (latestStart < 9 * 60) {
            latestStart = 9 * 60;
        }

        int bestScore = Integer.MIN_VALUE;

        for (int candidateStart = 9 * 60; candidateStart <= latestStart; candidateStart += 30) {
            List<ScheduledStop> candidateSchedule =
                    buildScheduleFromStart(originalPlaces, candidateStart, minutesPerPlace);

            int score = scoreSchedule(candidateSchedule);

            if (score > bestScore) {
                bestScore = score;
                bestSchedule = candidateSchedule;
            }
        }

        return bestSchedule;
    }

    private List<ScheduledStop> buildScheduleFromStart(List<Place> originalPlaces,
                                                       int startMinutes,
                                                       int minutesPerPlace) {

        List<ScheduledStop> schedule = new ArrayList<>();
        List<Place> remainingPlaces = new ArrayList<>(originalPlaces);

        int currentMinutes = startMinutes;

        while (!remainingPlaces.isEmpty()) {
            int endMinutes = currentMinutes + minutesPerPlace;

            if (endMinutes > 24 * 60) {
                endMinutes = 24 * 60;
            }

            Place bestPlace = findBestPlaceForTime(remainingPlaces, currentMinutes, endMinutes);

            TimeSlot slot = new TimeSlot(
                    getCreneauLabel(currentMinutes),
                    formatMinutes(currentMinutes) + " - " + formatMinutes(endMinutes),
                    currentMinutes,
                    endMinutes
            );

            schedule.add(new ScheduledStop(bestPlace, slot));
            remainingPlaces.remove(bestPlace);

            currentMinutes = endMinutes;
        }

        return schedule;
    }

    private Place findBestPlaceForTime(List<Place> places, int startMinutes, int endMinutes) {
        Place bestPlace = places.get(0);
        int bestScore = Integer.MIN_VALUE;

        for (Place place : places) {
            int score = 0;

            boolean hasOpeningHours =
                    place.getOpeningPeriods() != null && !place.getOpeningPeriods().isEmpty();

            if (hasOpeningHours) {
                if (isPlaceOpenDuringSlot(place, startMinutes, endMinutes)) {
                    score += 100;
                } else {
                    score -= 100;
                }
            } else {
                score += 10;
            }

            String category = place.getCategory();

            if (category != null && category.equals("restauration")) {
                if ((startMinutes >= 12 * 60 && startMinutes <= 14 * 60)
                        || (startMinutes >= 18 * 60 && startMinutes <= 21 * 60)) {
                    score += 35;
                } else {
                    score -= 10;
                }
            }

            if (category != null && category.equals("culture")) {
                if (startMinutes >= 9 * 60 && startMinutes <= 17 * 60) {
                    score += 20;
                } else {
                    score -= 15;
                }
            }

            if (category != null
                    && (category.equals("loisirs") || category.equals("decouvertes"))) {
                if (startMinutes >= 15 * 60 && startMinutes <= 21 * 60) {
                    score += 15;
                }
            }

            if (startMinutes < 12 * 60) {
                score += 5;
            } else if (startMinutes < 17 * 60) {
                score += 8;
            } else {
                score += 6;
            }

            if (score > bestScore) {
                bestScore = score;
                bestPlace = place;
            }
        }

        return bestPlace;
    }

    private int scoreSchedule(List<ScheduledStop> schedule) {
        int score = 0;

        boolean hasMatin = false;
        boolean hasApresMidi = false;
        boolean hasSoir = false;

        for (ScheduledStop stop : schedule) {
            Place place = stop.place;
            TimeSlot slot = stop.slot;

            boolean hasOpeningHours =
                    place.getOpeningPeriods() != null && !place.getOpeningPeriods().isEmpty();

            if (hasOpeningHours) {
                if (isPlaceOpenDuringSlot(place, slot.startMinutes, slot.endMinutes)) {
                    score += 100;
                } else {
                    score -= 120;
                }
            } else {
                score += 10;
            }

            if (slot.startMinutes < 12 * 60) {
                hasMatin = true;
            } else if (slot.startMinutes < 17 * 60) {
                hasApresMidi = true;
            } else {
                hasSoir = true;
            }

            if (slot.endMinutes > 22 * 60) {
                score -= 10;
            }

            if (slot.startMinutes < 9 * 60) {
                score -= 30;
            }
        }

        if (hasMatin) score += 5;
        if (hasApresMidi) score += 5;
        if (hasSoir) score += 5;

        return score;
    }

    private String getCreneauLabel(int minutesFromMidnight) {
        if (minutesFromMidnight < 12 * 60) {
            return "MATIN";
        } else if (minutesFromMidnight < 17 * 60) {
            return "APRÈS-MIDI";
        } else {
            return "SOIR";
        }
    }

    private String getOpeningWarning(Place place, int startMinutes, int endMinutes) {
        List<Map<String, Object>> periods = place.getOpeningPeriods();

        if (periods == null || periods.isEmpty()) {
            return "ℹ️ Suggested time, opening hours unavailable";
        }

        boolean open = isPlaceOpenDuringSlot(place, startMinutes, endMinutes);

        if (open) {
            return "";
        } else {
            return "⚠️ May be closed at this time";
        }
    }

    private boolean isPlaceOpenDuringSlot(Place place, int startMinutes, int endMinutes) {
        List<Map<String, Object>> periods = place.getOpeningPeriods();

        if (periods == null || periods.isEmpty()) {
            return false;
        }

        int today = getTodayForGoogleOpeningHours();

        for (Map<String, Object> period : periods) {
            Object openObj = period.get("open");
            Object closeObj = period.get("close");

            if (!(openObj instanceof Map)) continue;

            Map<String, Object> openMap = (Map<String, Object>) openObj;

            int openDay = getIntFromObject(openMap.get("day"));
            int openTime = parseGoogleTimeToMinutes(openMap.get("time"));

            int closeDay;
            int closeTime;

            if (closeObj instanceof Map) {
                Map<String, Object> closeMap = (Map<String, Object>) closeObj;
                closeDay = getIntFromObject(closeMap.get("day"));
                closeTime = parseGoogleTimeToMinutes(closeMap.get("time"));
            } else {
                closeDay = openDay;
                closeTime = 24 * 60;
            }

            if (isSlotInsideOpeningPeriod(today, startMinutes, endMinutes,
                    openDay, openTime, closeDay, closeTime)) {
                return true;
            }
        }

        return false;
    }

    private boolean isSlotInsideOpeningPeriod(int today,
                                              int slotStart,
                                              int slotEnd,
                                              int openDay,
                                              int openTime,
                                              int closeDay,
                                              int closeTime) {

        int slotStartAbs = today * 24 * 60 + slotStart;
        int slotEndAbs = today * 24 * 60 + slotEnd;

        int openAbs = openDay * 24 * 60 + openTime;
        int closeAbs = closeDay * 24 * 60 + closeTime;

        if (closeAbs <= openAbs) {
            closeAbs += 7 * 24 * 60;
        }

        if (slotEndAbs <= slotStartAbs) {
            slotEndAbs += 24 * 60;
        }

        if (openAbs > slotStartAbs) {
            int shiftedOpenAbs = openAbs - 7 * 24 * 60;
            int shiftedCloseAbs = closeAbs - 7 * 24 * 60;

            if (slotStartAbs >= shiftedOpenAbs && slotEndAbs <= shiftedCloseAbs) {
                return true;
            }
        }

        return slotStartAbs >= openAbs && slotEndAbs <= closeAbs;
    }

    private int getTodayForGoogleOpeningHours() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        int javaDay = calendar.get(java.util.Calendar.DAY_OF_WEEK);

        return javaDay - 1;
    }

    private int parseGoogleTimeToMinutes(Object timeObj) {
        if (timeObj == null) return 0;

        String time = String.valueOf(timeObj);

        if (time.length() < 4) {
            return 0;
        }

        try {
            int hour = Integer.parseInt(time.substring(0, 2));
            int minute = Integer.parseInt(time.substring(2, 4));
            return hour * 60 + minute;
        } catch (Exception e) {
            return 0;
        }
    }

    private int getIntFromObject(Object value) {
        if (value == null) return 0;

        if (value instanceof Long) {
            return ((Long) value).intValue();
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Double) {
            return ((Double) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private int extractDurationMinutes(RouteOption option) {
        int selectedDuration = option.getSelectedDurationMinutes();

        if (selectedDuration > 0) {
            return selectedDuration;
        }

        // Fallback:
        // Eğer selectedDurationMinutes set edilmemişse 3 saat kabul ediyoruz.
        // Burada Google Directions duration kullanmıyoruz,
        // çünkü o yürüyüş/ulaşım süresi; user'ın seçtiği gezi süresi değil.
        return 180;
    }

    private String formatMinutes(int minutesFromMidnight) {
        if (minutesFromMidnight == 24 * 60) {
            return "24:00";
        }

        int hour = (minutesFromMidnight / 60) % 24;
        int minute = minutesFromMidnight % 60;

        return String.format("%02d:%02d", hour, minute);
    }

    private com.google.android.gms.maps.GoogleMap savedMap;

    private void setupMiniMap(RouteOption selectedOption) {
        FragmentManager fragmentManager = getChildFragmentManager();
        SupportMapFragment mapFragment =
                (SupportMapFragment) fragmentManager.findFragmentById(R.id.mini_map);

        if (mapFragment == null) return;

        mapFragment.getMapAsync(googleMap -> {
            savedMap = googleMap;
            updateMap(selectedOption);
        });
    }

    private void updateMap(RouteOption option) {
        if (savedMap == null) return;

        savedMap.clear();

        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasValidLocation = false;

        for (Place place : option.getPlaces()) {
            if (place.getLocation() != null) {
                LatLng latLng = new LatLng(
                        place.getLocation().getLatitude(),
                        place.getLocation().getLongitude());

                savedMap.addMarker(new MarkerOptions()
                        .position(latLng)
                        .title(place.getName()));

                boundsBuilder.include(latLng);
                hasValidLocation = true;
            }
        }

        RouteDetails details = option.getRouteDetails();
        if (details != null && details.getPolylinePoints() != null) {
            savedMap.addPolyline(new PolylineOptions()
                    .addAll(details.getPolylinePoints())
                    .color(Color.BLUE)
                    .width(8));
        }

        if (hasValidLocation) {
            savedMap.moveCamera(
                    CameraUpdateFactory.newLatLngBounds(
                            boundsBuilder.build(), 100));
        }
    }

    private void setupWeather(View view, RouteOption option) {
        double sumLat = 0, sumLng = 0;
        int count = 0;

        for (Place place : option.getPlaces()) {
            if (place.getLocation() != null) {
                sumLat += place.getLocation().getLatitude();
                sumLng += place.getLocation().getLongitude();
                count++;
            }
        }

        if (count == 0) return;

        double centerLat = sumLat / count;
        double centerLng = sumLng / count;

        WeatherService weatherService = new WeatherService();
        weatherService.getTodayForecast(centerLat, centerLng,
                new WeatherService.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherInfo info) {
                        LinearLayout layout = view.findViewById(R.id.layout_weather);
                        TextView text = view.findViewById(R.id.text_weather);
                        text.setText(info.getShortDisplay());
                        layout.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e("RouteDetail", "Weather fetch failed", e);
                    }
                });
    }

    private String getGenericDescription(Place place) {
        if (place.getCategory() == null) return "A noteworthy stop on your journey.";
        switch (place.getCategory()) {
            case "culture": return "A cultural landmark not to miss during your visit.";
            case "restauration": return "A culinary stop to recharge and enjoy local flavors.";
            case "loisirs": return "A relaxing spot to take a break and enjoy the atmosphere.";
            case "decouvertes": return "An unexpected discovery waiting to be explored.";
            default: return "A noteworthy stop on your journey.";
        }
    }
}