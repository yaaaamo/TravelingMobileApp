package com.example.traveling;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private static final String TAG = "SignUpActivity";

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
            String fullname = fullnameField.getText().toString().trim();
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            // Basic validation
            if (fullname.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task ->  {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                Log.d("SIGNUP", "createUserWithEmail:success");
                                if(user!= null) {
                                    String uid = user.getUid();
                                    Map<String, Object> userData = new HashMap<>();
                                    userData.put("fullname", fullname);
                                    userData.put("email", email);
                                    userData.put("uid", uid);
                                    db.collection("Users").document(uid)
                                            .set(userData)
                                            .addOnSuccessListener(unused -> {
                                                        Log.d(TAG, "User data saved successfully");

                                                    });
                                }
                                Intent intent = new Intent(this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                Log.w("SINGUP", "createUserWithEmail:failure", task.getException());
                                Toast.makeText(SignUpActivity.this,
                                        "Registration failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            }
                    });
        });
    }
}