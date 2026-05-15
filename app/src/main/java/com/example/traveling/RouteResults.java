package com.example.traveling;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RouteResults extends Fragment {

    private RouteOptionAdapter adapter;
    private TravelPathViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_route_results, container, false);

        // viewModel ÖNCE
        viewModel = new ViewModelProvider(requireActivity()).get(TravelPathViewModel.class);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_routes);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new RouteOptionAdapter(new ArrayList<>(), option -> {
            List<RouteOption> currentOptions = viewModel.getRouteOptions().getValue();
            if (currentOptions == null) return;
            int planIndex = currentOptions.indexOf(option);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, RouteDetail.newInstance(planIndex))
                    .addToBackStack(null)
                    .commit();
        });
        recyclerView.setAdapter(adapter);

        viewModel.getRouteOptions().observe(getViewLifecycleOwner(), options -> {
            if (options != null) {
                adapter.updateData(options);
            }
        });

        ImageButton btnBack = view.findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        return view;
    }
}