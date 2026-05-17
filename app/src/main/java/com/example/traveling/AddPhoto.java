package com.example.traveling;

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
import java.util.List;
import java.util.UUID;

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
        db.collection("groups").get().addOnSuccessListener(snapshot -> {
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

            // if opened from a group page, pre-select that group in the radio + spinner
            if (preselectedGroupId != null) {
                postTargetGroup.check(R.id.radioGroup);
                groupSpinner.setVisibility(View.VISIBLE);
                int index = groupIds.indexOf(preselectedGroupId);
                if (index >= 0) groupSpinner.setSelection(index);
            }
        });
    }

    private void savePostToFirestore(String imageUrl, String caption, String location,
                                     String country, String tags, String travelType,
                                     String groupId) {

        String userId    = auth.getCurrentUser().getUid();
        String timestamp = String.valueOf(System.currentTimeMillis());

        // fetch the user's profile to get username and profile picture
        db.collection("users").document(userId).get()
                .addOnSuccessListener(userDoc -> {

                    String username       = userDoc.getString("username");
                    String profilePicture = userDoc.getString("profilePicture");

                    ModelPost post = new ModelPost(
                            username, caption, imageUrl, timestamp,
                            0, 0, profilePicture,
                            location, country, tags, 0, travelType, groupId, userId);

                    post.setUserID(auth.getCurrentUser().getUid());
                    db.collection("posts")
                            .add(post)
                            .addOnSuccessListener(ref -> {
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
}