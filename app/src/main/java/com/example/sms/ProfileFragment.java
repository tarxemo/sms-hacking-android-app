package com.example.sms;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        
        DeviceAuthManager authManager = new DeviceAuthManager(requireContext());
        TextView tvUsername = view.findViewById(R.id.tv_username);
        tvUsername.setText(authManager.getDeviceName()); // Display device name as username for now

        view.findViewById(R.id.tv_logout).setOnClickListener(v -> {
            authManager.setSessionUnlocked(false);
            // Optionally clear token if you want a full logout, but usually just lock session
            // authManager.saveToken(null); 
            requireActivity().startActivity(new android.content.Intent(requireContext(), LoginActivity.class));
            requireActivity().finish();
        });

        return view;
    }
}
