package com.example.traveling;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private Uri selectedImageUri;
    private ImageView imageView;
    private Button pickBtn, uploadBtn;
    private final StorageService storageService = new StorageService();

    private FirebaseAnalytics analytics;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    imageView.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        analytics = FirebaseAnalytics.getInstance(this);

        imageView = findViewById(R.id.imageView);
        pickBtn = findViewById(R.id.pickBtn);
        uploadBtn = findViewById(R.id.uploadBtn);

        pickBtn.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        uploadBtn.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                storageService.uploadFile(selectedImageUri);
            } else {
                Toast.makeText(this, "Pick an image first", Toast.LENGTH_SHORT).show();
            }
        });

        if (FirebaseApp.getInstance() != null) {
            Log.d("Firebase", "Firebase is connected!");
        }

        /*FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                Log.d("Auth", "Signed in: " + user.getEmail());
                navigateToHome();
            } else {
                navigateToLogin();
            }
        });*/
    }

    public void navigateToHome() {
    }

    public void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
}