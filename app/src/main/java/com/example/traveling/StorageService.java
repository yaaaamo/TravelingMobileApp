package com.example.traveling;
import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.util.Log;

public class StorageService {
    // Upload a file to Firebase Storage
    public void uploadFile(Uri fileUri) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference ref = storage.getReference()
                .child("uploads/" + fileUri.getLastPathSegment());

        ref.putFile(fileUri)
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred())
                            / snapshot.getTotalByteCount();
                    Log.d("Upload", "Progress: " + progress + "%");
                })
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL after successful upload
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d("Upload", "URL: " + downloadUrl);
                    });
                })
                .addOnFailureListener(e ->
                        Log.e("Upload", "Failed", e));
    }

}
