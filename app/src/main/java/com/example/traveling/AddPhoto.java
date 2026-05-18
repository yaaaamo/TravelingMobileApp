package com.example.traveling;

import android.app.AlertDialog;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.*;

public class AddPhoto extends Fragment {

    private Uri selectedImageUri;
    private ImageView imageView;
    private Button pickBtn, uploadBtn;

    private EditText captionInput, locationInput, countryInput, tagsInput, travelTypeInput;
    private RadioGroup postTargetGroup;
    private Spinner groupSpinner;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseStorage storage;

    private final List<String> groupNames = new ArrayList<>();
    private final List<String> groupIds = new ArrayList<>();

    private String preselectedGroupId = null;

    // place data
    private String foundPlaceId = null;
    private double foundLat = 0;
    private double foundLng = 0;
    private String foundPlaceName = null;

    private final PlacesApiService placesApiService = new PlacesApiService();

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imageView.setImageURI(uri);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_add_photo, container, false);

        initFirebase();
        bindViews(view);
        initArguments();
        setupUI(view); // ← pass view down

        return view;
    }

    // ───────────────────────── INIT ─────────────────────────

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    private void bindViews(View view) {
        imageView        = view.findViewById(R.id.imageView);
        pickBtn          = view.findViewById(R.id.pickBtn);
        uploadBtn        = view.findViewById(R.id.uploadBtn);

        captionInput     = view.findViewById(R.id.captionInput);
        locationInput    = view.findViewById(R.id.locationInput);
        countryInput     = view.findViewById(R.id.countryInput);
        tagsInput        = view.findViewById(R.id.tagsInput);
        travelTypeInput  = view.findViewById(R.id.travelTypeInput);

        postTargetGroup  = view.findViewById(R.id.postTargetGroup);
        groupSpinner     = view.findViewById(R.id.groupSpinner);
    }

    private void initArguments() {
        if (getArguments() != null) {
            preselectedGroupId = getArguments().getString("groupId");
        }
    }

    // FIX: view is now passed as a parameter so setupPlacesUI can use it
    // instead of calling getView() which is null during onCreateView.
    private void setupUI(View view) {
        loadGroups();

        postTargetGroup.setOnCheckedChangeListener((g, id) ->
                groupSpinner.setVisibility(id == R.id.radioGroup ? View.VISIBLE : View.GONE)
        );

        pickBtn.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        uploadBtn.setOnClickListener(v -> uploadPost());

        setupPlacesUI(view); // ← pass view here too
    }

    // ───────────────────────── PLACES ─────────────────────────

    // FIX: accepts the inflated view instead of calling getView()
    private void setupPlacesUI(View view) {
        Button   addToPlacesBtn = view.findViewById(R.id.addToPlacesBtn);
        EditText searchInput    = view.findViewById(R.id.placeSearchInput);
        Button   searchBtn      = view.findViewById(R.id.searchPlaceBtn);
        TextView confirmed      = view.findViewById(R.id.confirmedPlaceText);

        addToPlacesBtn.setOnClickListener(v -> showAddPlaceDialog("blablabla"));

        searchBtn.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();

            if (query.isEmpty()) {
                toast("Enter a place name");
                return;
            }

            searchBtn.setEnabled(false);
            searchBtn.setText("Searching...");
            addToPlacesBtn.setVisibility(View.GONE);

            db.collection("places").get()
                    .addOnSuccessListener(snapshot -> {
                        DocumentSnapshot match = findPlace(snapshot, query);

                        searchBtn.setEnabled(true);
                        searchBtn.setText("Search");

                        if (match != null) {
                            setFoundPlace(match);
                            confirmed.setText("✓ " + foundPlaceName + " (from your database)");
                            confirmed.setVisibility(View.VISIBLE);
                        } else {
                            toast("Place not found in database — add it manually");
                            addToPlacesBtn.setVisibility(View.VISIBLE);
                        }
                    });
        });
    }

    private DocumentSnapshot findPlace(QuerySnapshot snapshot, String query) {
        String q = query.toLowerCase();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            String name = doc.getString("name");
            if (name != null && name.toLowerCase().contains(q)) {
                return doc;
            }
        }
        return null;
    }

    private void setFoundPlace(DocumentSnapshot doc) {
        foundPlaceId   = doc.getId();
        foundPlaceName = doc.getString("name");

        GeoPoint geo = doc.getGeoPoint("location");
        if (geo != null) {
            foundLat = geo.getLatitude();
            foundLng = geo.getLongitude();
        }
    }

    // ───────────────────────── GROUPS ─────────────────────────

    private void loadGroups() {
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    List<String> ids = (List<String>) doc.get("groupIds");

                    groupNames.clear();
                    groupIds.clear();

                    if (ids == null || ids.isEmpty()) {
                        updateSpinner();
                        return;
                    }

                    List<String> safeIds = ids.size() > 10 ? ids.subList(0, 10) : ids;

                    db.collection("groups")
                            .whereIn(FieldPath.documentId(), safeIds)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                for (DocumentSnapshot d : snapshot.getDocuments()) {
                                    String name = d.getString("name");
                                    if (name != null) {
                                        groupNames.add(name);
                                        groupIds.add(d.getId());
                                    }
                                }
                                updateSpinner();
                                applyPreselectedGroup();
                            });
                });
    }

    private void updateSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                groupNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        groupSpinner.setAdapter(adapter);
    }

    private void applyPreselectedGroup() {
        if (preselectedGroupId == null) return;

        postTargetGroup.check(R.id.radioGroup);
        groupSpinner.setVisibility(View.VISIBLE);

        int index = groupIds.indexOf(preselectedGroupId);
        if (index >= 0) groupSpinner.setSelection(index);
    }

    // ───────────────────────── UPLOAD ─────────────────────────

    private void uploadPost() {
        if (selectedImageUri == null) {
            toast("Pick an image first");
            return;
        }

        if (auth.getCurrentUser() == null) {
            toast("Not logged in");
            return;
        }

        String caption    = captionInput.getText().toString().trim();
        String location   = locationInput.getText().toString().trim();
        String country    = countryInput.getText().toString().trim();
        String tags       = tagsInput.getText().toString().trim();
        String travelType = travelTypeInput.getText().toString().trim();

        if (caption.isEmpty() || location.isEmpty() || country.isEmpty()) {
            toast("Caption, location and country are required");
            return;
        }

        String groupId = resolveGroupId();

        uploadBtn.setEnabled(false);

        StorageReference ref = storage.getReference()
                .child("posts/" + UUID.randomUUID());

        ref.putFile(selectedImageUri)
                .addOnSuccessListener(t ->
                        ref.getDownloadUrl().addOnSuccessListener(uri ->
                                savePost(uri.toString(), caption, location,
                                        country, tags, travelType, groupId)
                        )
                )
                .addOnFailureListener(e -> {
                    uploadBtn.setEnabled(true);
                    toast("Upload failed: " + e.getMessage());
                });
    }

    private String resolveGroupId() {
        if (preselectedGroupId != null) return preselectedGroupId;

        if (postTargetGroup.getCheckedRadioButtonId() == R.id.radioGroup
                && !groupIds.isEmpty()) {
            return groupIds.get(groupSpinner.getSelectedItemPosition());
        }

        return null;
    }

    private void savePost(String imageUrl, String caption, String location,
                          String country, String tags, String travelType,
                          String groupId) {

        String userId = auth.getCurrentUser().getUid();

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(user -> {

                    ModelPost post = new ModelPost(
                            user.getString("fullname"),
                            caption,
                            imageUrl,
                            String.valueOf(System.currentTimeMillis()),
                            0, 0,
                            user.getString("profilePicture"),
                            location, country, tags,
                            0, travelType,
                            groupId,
                            userId
                    );

                    post.setGooglePlaceId(foundPlaceId);
                    post.setLat(foundLat);
                    post.setLng(foundLng);

                    if (foundPlaceName != null && location.isEmpty()) {
                        post.setLocation(foundPlaceName);
                    }

                    post.setUserID(userId);

                    db.collection("posts").add(post)
                            .addOnSuccessListener(r -> {
                                FollowHelper.notifyFollowersOfNewPost(userId,
                                        user.getString("fullname"),
                                        r.getId());

                                toast("Posted!");
                                uploadBtn.setEnabled(true);
                                requireActivity().getSupportFragmentManager().popBackStack();
                            })
                            .addOnFailureListener(e -> {
                                uploadBtn.setEnabled(true);
                                toast("Failed: " + e.getMessage());
                            });
                });
    }

    // ───────────────────────── DIALOG ─────────────────────────

    private void showAddPlaceDialog(String prefill) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_place, null);

        EditText name     = v.findViewById(R.id.placeNameInput);
        EditText category = v.findViewById(R.id.placeCategoryInput);

        name.setText(prefill);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Place")
                .setView(v)
                .setPositiveButton("Add", (d, w) -> {
                    String n = name.getText().toString().trim();
                    String c = category.getText().toString().trim();

                    if (n.isEmpty()) {
                        toast("Enter a place name");
                        return;
                    }

                    try {
                        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                        List<Address> list = geocoder.getFromLocationName(n, 1);

                        if (list == null || list.isEmpty()) {
                            toast("Could not find coordinates");
                            return;
                        }

                        Address a   = list.get(0);
                        GeoPoint geo = new GeoPoint(a.getLatitude(), a.getLongitude());

                        HashMap<String, Object> place = new HashMap<>();
                        place.put("name",          n);
                        place.put("category",      c);
                        place.put("location",      geo);
                        place.put("googlePlaceId", null);
                        place.put("effortLevel",   1);
                        place.put("goodForCold",   false);
                        place.put("goodForHeat",   false);
                        place.put("goodForRain",   false);

                        db.collection("places").add(place)
                                .addOnSuccessListener(ref -> {
                                    foundPlaceId   = ref.getId();
                                    foundLat       = a.getLatitude();
                                    foundLng       = a.getLongitude();
                                    foundPlaceName = n;

                                    // getView() is safe here — we're in an async callback,
                                    // long after onCreateView has returned.
                                    View root = getView();
                                    if (root != null) {
                                        TextView tv = root.findViewById(R.id.confirmedPlaceText);
                                        tv.setText("✓ " + n + " (added)");
                                        tv.setVisibility(View.VISIBLE);
                                    }

                                    toast("Place added!");
                                });

                    } catch (IOException e) {
                        toast("Geocoder error: " + e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }
}