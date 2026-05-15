package com.example.traveling;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;


public class Profile extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_profile, container, false);
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseUser user = firebaseAuth.getCurrentUser();
        TextView name = view.findViewById(R.id.nametv);
        TextView email= view.findViewById(R.id.emailtv);
        if (user != null) {
            String uid = user.getUid();

            db.collection("Users").document(uid)
                    .addSnapshotListener((snapshot, error) -> {
                        //removing this for now as it messes with anonymous logic but i have to rewrite it!!!
                        //if (error != null || snapshot == null || !snapshot.exists()) Log.d("anonymous", "error in the first if");

                        String nameStr = snapshot.getString("fullname");
                        String emailStr = snapshot.getString("email");
                        //String imageUrl = snapshot.getString("image");
                        if(user.isAnonymous()){
                            name.setText("Guest ;)");
                            email.setText("Guest@plslogin.com");
                        }
                        else{
                        name.setText(nameStr);
                        email.setText(emailStr);}

                        /*if (getActivity() != null) {
                            Glide.with(getActivity())
                                    .load(imageUrl)
                                    .into(avatartv);
                        }*/
                    });
        }

        Button btnSavedJourneys = view.findViewById(R.id.btn_saved_journeys);
        btnSavedJourneys.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new SavedJourneysFragment())
                        .addToBackStack(null)
                        .commit()
        );
        return view;
    }
}