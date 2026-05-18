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

import com.example.traveling.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

//    private final ActivityResultLauncher<String> pickImageLauncher =
//            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
//                if (uri != null) {
//                    selectedImageUri = uri;
//                    imageView.setImageURI(uri);
//                }
//            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setCurrentFragment(new Home());

        analytics = FirebaseAnalytics.getInstance(this);


        //pickBtn.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

//        uploadBtn.setOnClickListener(v -> {
//            if (selectedImageUri != null) {
//                storageService.uploadFile(selectedImageUri);
//            } else {
//                Toast.makeText(this, "Pick an image first", Toast.LENGTH_SHORT).show();
//            }
//        });

        if (FirebaseApp.getInstance() != null) {
            Log.d("Firebase", "Firebase is connected!");
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (getIntent().getBooleanExtra("openAddPhoto", false)) {
            String gid = getIntent().getStringExtra("groupId");
            AddPhoto addPhotoFragment = new AddPhoto();
            if (gid != null) {
                Bundle bundle = new Bundle();
                bundle.putString("groupId", gid);
                addPhotoFragment.setArguments(bundle);
            }
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, addPhotoFragment)
                    .addToBackStack(null)
                    .commit();
        }

        BottomNavigationView bot = findViewById(R.id.bottomNavigationView);
        bot.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.share) {
                setCurrentFragment(new Home());
            } else if (id == R.id.maps) {
                setCurrentFragment(new Maps());
            } else if (id == R.id.path) {
                setCurrentFragment(new TravelPath());
            } else if (id == R.id.groups) {
                setCurrentFragment(new Groups());
            } else if (id == R.id.profile) {
                setCurrentFragment(new Profile());
            }

            return true;
        });

    }
    public void navigateToHome(){
        setCurrentFragment(new Home());
    }

    public void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }
    private void setCurrentFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

}