package com.example.traveling;

import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;


public class AddPhoto extends Fragment {
    private Uri selectedImageUri;
    private ImageView imageView;
    private Button pickBtn, uploadBtn;
    private FirebaseAnalytics analytics;

    private final StorageService storageService = new StorageService();
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
        // Inflate the layout for this fragment
        View view=  inflater.inflate(R.layout.fragment_add_photo, container, false);
        analytics = FirebaseAnalytics.getInstance(getActivity());

        imageView = view.findViewById(R.id.imageView);
        pickBtn = view.findViewById(R.id.pickBtn);
        uploadBtn = view.findViewById(R.id.uploadBtn);

        pickBtn.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        uploadBtn.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                storageService.uploadFile(selectedImageUri);
            } else {
                Toast.makeText(getContext(), "Pick an image first", Toast.LENGTH_SHORT).show();
            }
        });


        return view;
    }
}