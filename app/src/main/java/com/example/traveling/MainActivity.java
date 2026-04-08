package com.example.traveling;

import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// MainActivity.java
import com.google.firebase.FirebaseApp;

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
    }
}