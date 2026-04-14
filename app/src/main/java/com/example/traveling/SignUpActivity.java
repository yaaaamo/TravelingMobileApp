package com.example.traveling;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private EditText fullnameField, emailField, passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        fullnameField = findViewById(R.id.fullname);
        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.pass);

        Button registerBtn = findViewById(R.id.submit);

        registerBtn.setOnClickListener(v -> {

            String fullName = fullnameField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // ✅ Validation
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("SIGNUP", "Button clicked");

            // 🔐 STEP 1: Firebase Auth
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {

                        Log.d("SIGNUP", "Auth callback triggered");

                        if (task.isSuccessful()) {

                            String uid = mAuth.getCurrentUser().getUid();

                            // 👤 STEP 2: Firestore user data
                            Map<String, Object> user = new HashMap<>();
                            user.put("full_name", fullName);
                            user.put("email", email);

                            db.collection("users")
                                    .document(uid)
                                    .set(user)
                                    .addOnSuccessListener(unused -> {

                                        Log.d("SIGNUP", "Firestore success");

                                        Toast.makeText(SignUpActivity.this,
                                                "Account created!",
                                                Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {

                                        Log.e("SIGNUP", "Firestore error", e);

                                        Toast.makeText(SignUpActivity.this,
                                                "Firestore error: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });

                        } else {

                            Log.e("SIGNUP", "Auth failed", task.getException());

                            Toast.makeText(SignUpActivity.this,
                                    "Sign up failed: " +
                                            (task.getException() != null ? task.getException().getMessage() : "unknown error"),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}