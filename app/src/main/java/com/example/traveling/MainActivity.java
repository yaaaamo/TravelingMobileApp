package com.example.traveling;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.traveling.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setCurrentFragment(new Home());

        // Firebase auto-initializes with google-services plugin
        // Manual init only if needed:
        // FirebaseApp.initializeApp(this);

        // Verify connection
        if (FirebaseApp.getInstance() != null) {
            Log.d("Firebase", "Firebase is connected!");
        }
        FirebaseAuth.getInstance().addAuthStateListener(auth -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                // User is signed in - show main content
                Log.d("Auth", "Signed in: " + user.getEmail());
                navigateToHome();
            } else {
                // User is signed out - show login screen
                navigateToLogin();
            }
        });
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
    public void navigateToLogin(){
        Intent intent = new Intent(this, LoginActivity.class); // or whatever your main screen is
        startActivity(intent);

    }
    private void setCurrentFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

}