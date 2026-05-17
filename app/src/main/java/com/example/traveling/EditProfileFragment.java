package com.example.traveling;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileFragment extends Fragment {

    private Uri selectedImageUri = null;
    private ImageView avatarPreview;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(avatarPreview);
                }
            });

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_edit_profile, container, false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        avatarPreview = view.findViewById(R.id.edit_avatar_preview);
        EditText editUsername = view.findViewById(R.id.edit_username);
        EditText editCurrentPassword = view.findViewById(R.id.edit_current_password);
        EditText editNewPassword = view.findViewById(R.id.edit_new_password);
        EditText editConfirmPassword = view.findViewById(R.id.edit_confirm_password);
        Button btnSaveUsername = view.findViewById(R.id.btn_save_username);
        Button btnSavePassword = view.findViewById(R.id.btn_save_password);
        Button btnPickPhoto = view.findViewById(R.id.btn_pick_photo);
        Button btnSavePhoto = view.findViewById(R.id.btn_save_photo);
        ImageButton btnBack = view.findViewById(R.id.btn_back_edit);

        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );


        if (user != null && !user.isAnonymous()) {
            db.collection("Users").document(user.getUid())
                    .get()
                    .addOnSuccessListener(doc -> {
                        String currentName = doc.getString("fullname");
                        if (currentName != null) {
                            editUsername.setText(currentName);
                        }

                        // Mevcut profil fotoğrafını göster
                        String photoUrl = doc.getString("profilePicture");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .circleCrop()
                                    .into(avatarPreview);
                        }
                    });
        }


        btnPickPhoto.setOnClickListener(v ->
                pickImageLauncher.launch("image/*")
        );


        btnSavePhoto.setOnClickListener(v -> {
            if (user == null || user.isAnonymous()) {
                Toast.makeText(requireContext(),
                        "Connectez-vous pour modifier votre photo.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedImageUri == null) {
                Toast.makeText(requireContext(),
                        "Please select a photo first.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            btnSavePhoto.setEnabled(false);
            btnSavePhoto.setText("Uploading...");

            StorageReference storageRef = FirebaseStorage.getInstance()
                    .getReference()
                    .child("profile_pictures/" + user.getUid() + ".jpg");

            storageRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            storageRef.getDownloadUrl()
                                    .addOnSuccessListener(uri -> {
                                        // Firestore'a kaydet
                                        db.collection("Users")
                                                .document(user.getUid())
                                                .update("profilePicture", uri.toString())
                                                .addOnSuccessListener(unused -> {
                                                    btnSavePhoto.setEnabled(true);
                                                    btnSavePhoto.setText("📷 Save Photo");
                                                    Toast.makeText(requireContext(),
                                                            "✅ Photo updated!",
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    }))
                    .addOnFailureListener(e -> {
                        btnSavePhoto.setEnabled(true);
                        btnSavePhoto.setText("📷 Save Photo");
                        Toast.makeText(requireContext(),
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });


        btnSaveUsername.setOnClickListener(v -> {
            if (user == null || user.isAnonymous()) {
                Toast.makeText(requireContext(),
                        "Connectez-vous pour modifier votre profil.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String newUsername = editUsername.getText().toString().trim();
            if (newUsername.isEmpty()) {
                editUsername.setError("Username cannot be empty");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("fullname", newUsername);

            db.collection("Users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(unused ->
                            Toast.makeText(requireContext(),
                                    "✅ Username updated!",
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
        });


        btnSavePassword.setOnClickListener(v -> {
            if (user == null || user.isAnonymous()) {
                Toast.makeText(requireContext(),
                        "Connectez-vous pour modifier votre mot de passe.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String currentPassword = editCurrentPassword.getText().toString().trim();
            String newPassword = editNewPassword.getText().toString().trim();
            String confirmPassword = editConfirmPassword.getText().toString().trim();

            if (currentPassword.isEmpty()) {
                editCurrentPassword.setError("Enter your current password");
                return;
            }

            if (newPassword.isEmpty()) {
                editNewPassword.setError("Enter a new password");
                return;
            }

            if (newPassword.length() < 6) {
                editNewPassword.setError("Password must be at least 6 characters");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                editConfirmPassword.setError("Passwords do not match");
                return;
            }

            String email = user.getEmail();
            if (email == null) return;

            btnSavePassword.setEnabled(false);
            btnSavePassword.setText("Updating...");

            AuthCredential credential = EmailAuthProvider
                    .getCredential(email, currentPassword);

            user.reauthenticate(credential)
                    .addOnSuccessListener(unused ->
                            user.updatePassword(newPassword)
                                    .addOnSuccessListener(unused2 -> {
                                        btnSavePassword.setEnabled(true);
                                        btnSavePassword.setText("💾 Save Password");
                                        editCurrentPassword.setText("");
                                        editNewPassword.setText("");
                                        editConfirmPassword.setText("");
                                        Toast.makeText(requireContext(),
                                                "✅ Password updated!",
                                                Toast.LENGTH_SHORT).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        btnSavePassword.setEnabled(true);
                                        btnSavePassword.setText("💾 Save Password");
                                        Toast.makeText(requireContext(),
                                                "Error: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }))
                    .addOnFailureListener(e -> {
                        btnSavePassword.setEnabled(true);
                        btnSavePassword.setText("💾 Save Password");
                        editCurrentPassword.setError("Wrong password");
                        Toast.makeText(requireContext(),
                                "Current password is incorrect.",
                                Toast.LENGTH_SHORT).show();
                    });
        });

        return view;
    }
}