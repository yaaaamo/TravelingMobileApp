package com.example.traveling;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import android.location.Address;
import android.location.Geocoder;
import com.google.firebase.firestore.GeoPoint;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

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

    // group data for spinner
    private List<String> groupNames = new ArrayList<>();
    private List<String> groupIds   = new ArrayList<>();

    // if opened from GroupDetailsActivity, this is pre-set
    private String preselectedGroupId = null;

//for google location
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

        db      = FirebaseFirestore.getInstance();
        auth    = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        // bind views
        imageView       = view.findViewById(R.id.imageView);
        pickBtn         = view.findViewById(R.id.pickBtn);
        uploadBtn       = view.findViewById(R.id.uploadBtn);
        captionInput    = view.findViewById(R.id.captionInput);
        locationInput   = view.findViewById(R.id.locationInput);
        countryInput    = view.findViewById(R.id.countryInput);
        tagsInput       = view.findViewById(R.id.tagsInput);
        travelTypeInput = view.findViewById(R.id.travelTypeInput);
        postTargetGroup = view.findViewById(R.id.postTargetGroup);
        groupSpinner    = view.findViewById(R.id.groupSpinner);

        //for google location !!!
        Button addToPlacesBtn = view.findViewById(R.id.addToPlacesBtn);
        addToPlacesBtn.setOnClickListener(l ->{
            showAddPlaceDialog("blablabla");
        });
        EditText placeSearchInput    = view.findViewById(R.id.placeSearchInput);
        Button searchPlaceBtn        = view.findViewById(R.id.searchPlaceBtn);
        TextView confirmedPlaceText  = view.findViewById(R.id.confirmedPlaceText);

        searchPlaceBtn.setOnClickListener(v -> {
            String query = placeSearchInput.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(getContext(), "Enter a place name", Toast.LENGTH_SHORT).show();
                return;
            }

            searchPlaceBtn.setEnabled(false);
            searchPlaceBtn.setText("Searching...");
            addToPlacesBtn.setVisibility(View.GONE);

            // search your own places collection first
            db.collection("places")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        String lowerQuery = query.toLowerCase().trim();

                        // look for a place whose name contains the query
                        DocumentSnapshot match = null;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String name = doc.getString("name");
                            if (name != null && name.toLowerCase().contains(lowerQuery)) {
                                match = doc;
                                break;
                            }
                        }

                        searchPlaceBtn.setEnabled(true);
                        searchPlaceBtn.setText("Search");

                        if (match != null) {
                            // found in your db — use it
                            foundPlaceId   = match.getId();
                            foundPlaceName = match.getString("name");

                            com.google.firebase.firestore.GeoPoint geoPoint =
                                    match.getGeoPoint("location");
                            if (geoPoint != null) {
                                foundLat = geoPoint.getLatitude();
                                foundLng = geoPoint.getLongitude();
                            }

                            confirmedPlaceText.setText("✓ " + foundPlaceName + " (from your database)");
                            confirmedPlaceText.setVisibility(View.VISIBLE);

                        } else {
                            // not found — show the manual add button
                            Toast.makeText(getContext(),
                                    "Place not found in database — add it manually",
                                    Toast.LENGTH_SHORT).show();
                            addToPlacesBtn.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        searchPlaceBtn.setEnabled(true);
                        searchPlaceBtn.setText("Search");
                        Toast.makeText(getContext(),
                                "Search failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
        // check if we were opened from GroupDetailsActivity with a preselected group
        if (getArguments() != null) {
            preselectedGroupId = getArguments().getString("groupId");
        }

        // load groups into spinner
        loadGroups();

        // show/hide group spinner based on radio selection
        postTargetGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioGroup) {
                groupSpinner.setVisibility(View.VISIBLE);
            } else {
                groupSpinner.setVisibility(View.GONE);
            }
        });

        pickBtn.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        uploadBtn.setOnClickListener(v -> {
            if (selectedImageUri == null) {
                Toast.makeText(getContext(), "Pick an image first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (auth.getCurrentUser() == null) {
                Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
                return;
            }

            String caption    = captionInput.getText().toString().trim();
            String location   = locationInput.getText().toString().trim();
            String country    = countryInput.getText().toString().trim();
            String tags       = tagsInput.getText().toString().trim();
            String travelType = travelTypeInput.getText().toString().trim();

            if (caption.isEmpty() || location.isEmpty() || country.isEmpty()) {
                Toast.makeText(getContext(),
                        "Caption, location and country are required",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // determine groupId — preselected (from group page) takes priority
            String selectedGroupId = null;
            if (preselectedGroupId != null) {
                selectedGroupId = preselectedGroupId;
            } else if (postTargetGroup.getCheckedRadioButtonId() == R.id.radioGroup
                    && !groupIds.isEmpty()) {
                selectedGroupId = groupIds.get(groupSpinner.getSelectedItemPosition());
            }

            final String finalGroupId = selectedGroupId;

            // upload image to Firebase Storage first
            uploadBtn.setEnabled(false);
            String filename = "posts/" + UUID.randomUUID().toString();
            StorageReference ref = storage.getReference().child(filename);

            ref.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            ref.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                String imageUrl = downloadUri.toString();
                                savePostToFirestore(imageUrl, caption, location, country,
                                        tags, travelType, finalGroupId);
                            })
                    )
                    .addOnFailureListener(e -> {
                        uploadBtn.setEnabled(true);
                        Toast.makeText(getContext(),
                                "Image upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });

        return view;
    }

    private void loadGroups() {
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("Users").document(userId).get()
                .addOnSuccessListener(userDoc -> {
                    List<String> ids = (List<String>) userDoc.get("groupIds");
                    if (ids == null || ids.isEmpty()) return;

                    List<String> safeIds = ids.size() > 10 ? ids.subList(0, 10) : ids;

        String currentUserId = auth.getCurrentUser() != null
                ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) return;

        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(userDoc -> {
                    List<String> userGroupIds = (List<String>) userDoc.get("groupIds");
                    if (userGroupIds == null || userGroupIds.isEmpty()) {
                        groupNames.clear();
                        groupIds.clear();
                        return;
                    }

                    List<String> safeIds = userGroupIds.size() > 10
                            ? userGroupIds.subList(0, 10) : userGroupIds;

                    db.collection("groups")
                            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), safeIds)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                groupNames.clear();
                                groupIds.clear();

                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    String name = doc.getString("name");
                                    if (name != null) {
                                        groupNames.add(name);
                                        groupIds.add(doc.getId());
                                    }
                                }

                                ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                                        requireContext(),
                                        android.R.layout.simple_spinner_item,
                                        groupNames);
                                spinnerAdapter.setDropDownViewResource(
                                        android.R.layout.simple_spinner_dropdown_item);
                                groupSpinner.setAdapter(spinnerAdapter);

                                if (preselectedGroupId != null) {
                                    postTargetGroup.check(R.id.radioGroup);
                                    groupSpinner.setVisibility(View.VISIBLE);
                                    int index = groupIds.indexOf(preselectedGroupId);
                                    if (index >= 0) groupSpinner.setSelection(index);
                                }
                            });
                            });

                                                        });

    }
    private void savePostToFirestore(String imageUrl, String caption, String location,
                                     String country, String tags, String travelType,
                                     String groupId) {

        String userId    = auth.getCurrentUser().getUid();
        String timestamp = String.valueOf(System.currentTimeMillis());

        // fetch the user's profile to get username and profile picture
        db.collection("Users").document(userId).get()
                .addOnSuccessListener(userDoc -> {

                    String username       = userDoc.getString("fullname");
                    String profilePicture = userDoc.getString("profilePicture");

                    ModelPost post = new ModelPost(
                            username, caption, imageUrl, timestamp,
                            0, 0, profilePicture,
                            location, country, tags, 0, travelType, groupId, userId);
//for google location im trying something
                    post.setGooglePlaceId(foundPlaceId);
                    post.setLat(foundLat);
                    post.setLng(foundLng);
// use found place name as location if user didn't type one separately
                    if (foundPlaceName != null && location.isEmpty()) {
                        post.setLocation(foundPlaceName);
                    }
                    post.setUserID(auth.getCurrentUser().getUid());
                    db.collection("posts")
                            .add(post)
                            .addOnSuccessListener(ref -> {
                                FollowHelper.notifyFollowersOfNewPost(userId, username, ref.getId());
                                Toast.makeText(getContext(),
                                        "Posted!", Toast.LENGTH_SHORT).show();
                                uploadBtn.setEnabled(true);
                                // go back
                                requireActivity().getSupportFragmentManager().popBackStack();
                            })
                            .addOnFailureListener(e -> {
                                uploadBtn.setEnabled(true);
                                Toast.makeText(getContext(),
                                        "Failed to save post: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }

    //Trying something out ?? instead of typing manually lat and long
    private void showAddPlaceDialog(String prefillName) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_place, null);

        EditText nameInput     = dialogView.findViewById(R.id.placeNameInput);
        EditText categoryInput = dialogView.findViewById(R.id.placeCategoryInput);

        nameInput.setText(prefillName);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Place to Database")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name     = nameInput.getText().toString().trim();
                    String category = categoryInput.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Enter a place name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // convert place name to coordinates automatically
                    try {
                        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocationName(name, 1);

                        if (addresses == null || addresses.isEmpty()) {
                            Toast.makeText(getContext(),
                                    "Could not find coordinates for this place",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        double lat = addresses.get(0).getLatitude();
                        double lng = addresses.get(0).getLongitude();

                        HashMap<String, Object> placeData = new HashMap<>();
                        placeData.put("name", name);
                        placeData.put("category", category);
                        placeData.put("location", new GeoPoint(lat, lng));
                        placeData.put("googlePlaceId", null);
                        placeData.put("effortLevel", 1);
                        placeData.put("goodForCold", false);
                        placeData.put("goodForHeat", false);
                        placeData.put("goodForRain", false);

                        db.collection("places")
                                .add(placeData)
                                .addOnSuccessListener(ref -> {
                                    foundPlaceId   = ref.getId();
                                    foundLat       = lat;
                                    foundLng       = lng;
                                    foundPlaceName = name;

                                    TextView confirmedPlaceText =
                                            getView().findViewById(R.id.confirmedPlaceText);
                                    confirmedPlaceText.setText("✓ " + name + " (added to database)");
                                    confirmedPlaceText.setVisibility(View.VISIBLE);

                                    Toast.makeText(getContext(),
                                            "Place added!", Toast.LENGTH_SHORT).show();
                                });

                    } catch (IOException e) {
                        Toast.makeText(getContext(),
                                "Geocoder error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}