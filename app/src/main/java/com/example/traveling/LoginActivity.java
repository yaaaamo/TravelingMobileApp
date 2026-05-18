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
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText emailField, passwordField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        emailField = findViewById(R.id.email);
        passwordField = findViewById(R.id.password);
        findViewById(R.id.loginBtn).setOnClickListener(v -> {
            String email = emailField.getText().toString();
            String password = passwordField.getText().toString();
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            // analystics
                            // That way, analytics events can be associated with that signed-in user
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

                            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(this);
                            if (user != null) {
                                analytics.setUserId(user.getUid());
                            }

                            FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
                            crashlytics.setUserId(user.getUid());
                            crashlytics.setCustomKey("login_method", "email");

                            startActivity(new Intent(this, MainActivity.class));

                            finish();
                        } else {
                            Toast.makeText(this, "Auth failed", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        findViewById(R.id.signuppass).setOnClickListener(v ->{
            Intent intent = new Intent(this, SignUpActivity.class );
            startActivity(intent);
        });
        Button ano = findViewById(R.id.guestBtn);
        ano.setOnClickListener(l->{
            mAuth.signInAnonymously()
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d("anonymous", "signInAnonymously:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w("anonymous", "signInAnonymously:failure", task.getException());
                            }
                        }
                    });
        });


    }

}