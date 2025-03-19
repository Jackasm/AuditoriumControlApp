package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class AuditoriumSettingsFragment extends Fragment {

    private static final String ARG_AUDITORIUM_NAME = "auditorium_name";
    private static final String PREFS_NAME = "AuditoriumSettings";

    public static AuditoriumSettingsFragment newInstance(String auditoriumName) {
        AuditoriumSettingsFragment fragment = new AuditoriumSettingsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_AUDITORIUM_NAME, auditoriumName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_auditorium_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String auditoriumName = getArguments().getString(ARG_AUDITORIUM_NAME);
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        TextView textViewAuditoriumName = view.findViewById(R.id.text_auditorium_name);
        EditText editVideoProcessorIp = view.findViewById(R.id.edit_video_processor_ip);
        EditText editVideoProcessorPort = view.findViewById(R.id.edit_video_processor_port);
        EditText editAudioProcessorIp = view.findViewById(R.id.edit_audio_processor_ip);
        EditText editAudioProcessorPort = view.findViewById(R.id.edit_audio_processor_port);
        EditText editCamera1Ip = view.findViewById(R.id.edit_camera_1_ip);
        EditText editCamera1Port = view.findViewById(R.id.edit_camera_1_port);
        EditText editCamera2Ip = view.findViewById(R.id.edit_camera_2_ip);
        EditText editCamera2Port = view.findViewById(R.id.edit_camera_2_port);

        // Устанавливаем текущие значения
        textViewAuditoriumName.setText(auditoriumName);
        editVideoProcessorIp.setText(sharedPreferences.getString(auditoriumName + "_video_processor_ip", "192.168.1.100"));
        editVideoProcessorPort.setText(String.valueOf(sharedPreferences.getInt(auditoriumName + "_video_processor_port", 10500)));
        editAudioProcessorIp.setText(sharedPreferences.getString(auditoriumName + "_audio_processor_ip", "192.168.1.101"));
        editAudioProcessorPort.setText(String.valueOf(sharedPreferences.getInt(auditoriumName + "_audio_processor_port", 48631)));
        editCamera1Ip.setText(sharedPreferences.getString(auditoriumName + "_camera_1_ip", "192.168.1.102"));
        editCamera1Port.setText(String.valueOf(sharedPreferences.getInt(auditoriumName + "_camera_1_port", 5678)));
        editCamera2Ip.setText(sharedPreferences.getString(auditoriumName + "_camera_2_ip", "192.168.1.103"));
        editCamera2Port.setText(String.valueOf(sharedPreferences.getInt(auditoriumName + "_camera_2_port", 5678)));
    }

    public void saveSettings() {
        String auditoriumName = getArguments().getString(ARG_AUDITORIUM_NAME);
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        View view = getView();
        if (view != null) {
            EditText editVideoProcessorIp = view.findViewById(R.id.edit_video_processor_ip);
            EditText editVideoProcessorPort = view.findViewById(R.id.edit_video_processor_port);
            EditText editAudioProcessorIp = view.findViewById(R.id.edit_audio_processor_ip);
            EditText editAudioProcessorPort = view.findViewById(R.id.edit_audio_processor_port);
            EditText editCamera1Ip = view.findViewById(R.id.edit_camera_1_ip);
            EditText editCamera1Port = view.findViewById(R.id.edit_camera_1_port);
            EditText editCamera2Ip = view.findViewById(R.id.edit_camera_2_ip);
            EditText editCamera2Port = view.findViewById(R.id.edit_camera_2_port);

            editor.putString(auditoriumName + "_video_processor_ip", editVideoProcessorIp.getText().toString());
            editor.putInt(auditoriumName + "_video_processor_port", Integer.parseInt(editVideoProcessorPort.getText().toString()));
            editor.putString(auditoriumName + "_audio_processor_ip", editAudioProcessorIp.getText().toString());
            editor.putInt(auditoriumName + "_audio_processor_port", Integer.parseInt(editAudioProcessorPort.getText().toString()));
            editor.putString(auditoriumName + "_camera_1_ip", editCamera1Ip.getText().toString());
            editor.putInt(auditoriumName + "_camera_1_port", Integer.parseInt(editCamera1Port.getText().toString()));
            editor.putString(auditoriumName + "_camera_2_ip", editCamera2Ip.getText().toString());
            editor.putInt(auditoriumName + "_camera_2_port", Integer.parseInt(editCamera2Port.getText().toString()));
        }

        editor.apply();
    }
}