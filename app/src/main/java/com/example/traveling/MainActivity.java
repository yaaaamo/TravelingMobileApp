package com.example.traveling;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.traveling.R;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
    }
    public void navigateToHome(){
    }
    public void navigateToLogin(){
        Intent intent = new Intent(this, LoginActivity.class); // or whatever your main screen is
        startActivity(intent);

    }

}