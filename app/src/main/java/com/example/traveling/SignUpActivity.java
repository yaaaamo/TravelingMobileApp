package com.example.traveling;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {
    // Get Firestore instance
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        EditText fullname = findViewById(R.id.fullname);
        EditText mail = findViewById(R.id.email);
        EditText pass = findViewById(R.id.pass);
        Button registerBtn = findViewById(R.id.submit);

        // CREATE: Add a new document
        Map<String, Object> user = new HashMap<>();
        user.put("full_name", "John Doe");
        user.put("email", "john@example.com");

        db.collection("users")
                .add(user)
                .addOnSuccessListener(docRef ->
                        Log.d("DB", "Added: " + docRef.getId()))
                .addOnFailureListener(e ->
                        Log.w("DB", "Error", e));
        // READ: Get a document by ID
        db.collection("users")
                .document(mail.toString())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("full_name");
                        String email = doc.getString("email");
                        Log.d("DB", name + " - " + email);
                    }
                });

// READ: Get all documents in collection
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot) {
                        Log.d("DB", doc.getId());
                    }
                });


    }

}
