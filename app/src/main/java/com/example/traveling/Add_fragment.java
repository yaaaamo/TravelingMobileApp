package com.example.traveling;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class Add_fragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view= inflater.inflate(R.layout.add_fragment, container, false);
        Button addphoto = view.findViewById(R.id.addphoto);
        addphoto.setOnClickListener(l->{
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new AddPhoto())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}