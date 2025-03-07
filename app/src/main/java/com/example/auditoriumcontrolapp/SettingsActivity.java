package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AuditoriumSettings";
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        setupAuditoriumSettings("Аудитория 1", findViewById(R.id.auditorium_1_settings));
        setupAuditoriumSettings("Аудитория 2", findViewById(R.id.auditorium_2_settings));
        setupAuditoriumSettings("Аудитория 3", findViewById(R.id.auditorium_3_settings));
        setupAuditoriumSettings("Аудitorium 4", findViewById(R.id.auditorium_4_settings));

        // Кнопка сохранения
        Button buttonSaveSettings = findViewById(R.id.button_save_settings);
        buttonSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void setupAuditoriumSettings(String auditoriumName, View settingsView) {
        TextView textViewAuditoriumName = settingsView.findViewById(R.id.text_auditorium_name);
        EditText editVideoProcessorIp = settingsView.findViewById(R.id.edit_video_processor_ip);
        EditText editVideoProcessorPort = settingsView.findViewById(R.id.edit_video_processor_port);
        EditText editAudioProcessorIp = settingsView.findViewById(R.id.edit_audio_processor_ip);
        EditText editAudioProcessorPort = settingsView.findViewById(R.id.edit_audio_processor_port);
        EditText editCamera1Ip = settingsView.findViewById(R.id.edit_camera_1_ip);
        EditText editCamera1Port = settingsView.findViewById(R.id.edit_camera_1_port);
        EditText editCamera2Ip = settingsView.findViewById(R.id.edit_camera_2_ip);
        EditText editCamera2Port = settingsView.findViewById(R.id.edit_camera_2_port);

        // Устанавливаем текущие значения
        textViewAuditoriumName.setText(auditoriumName);
        editVideoProcessorIp.setText(sharedPreferences.getString(auditoriumName + "_video_processor_ip", "192.168.1.100"));
        editVideoProcessorPort.setText(String.valueOf(sharedPreferences.getInt(auditoriumName + "_video_processor_port", 10500)));
        editAudioProcessorIp.setText(sharedPreferences.getString(auditoriumName + "_audio_processor_ip", "192.168.1.101"));
        editAudioProcessorPort.setText(String.valueOf(sharedPreferences.getInt(auditoriumName + "_audio_processor_port", 5001)));
        editCamera1Ip.setText(sharedPreferences.getString(auditoriumName + "_camera_1_ip", "192.168.1.102"));
        editCamera1Port.setText(String.valueOf(sharedPreferences.getInt(auditoriumName + "_camera_1_port", 5002)));
        editCamera2Ip.setText(sharedPreferences.getString(auditoriumName + "_camera_2_ip", "192.168.1.103"));
        editCamera2Port.setText(String.valueOf(sharedPreferences.getInt(auditoriumName + "_camera_2_port", 5003)));
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        saveAuditoriumSettings("Аудитория 1", findViewById(R.id.auditorium_1_settings));
        saveAuditoriumSettings("Аудитория 2", findViewById(R.id.auditorium_2_settings));
        saveAuditoriumSettings("Аудитория 3", findViewById(R.id.auditorium_3_settings));
        saveAuditoriumSettings("Аудитория 4", findViewById(R.id.auditorium_4_settings));

        editor.apply();
        finish(); // Закрываем экран настроек
    }

    private void saveAuditoriumSettings(String auditoriumName, View settingsView) {
        EditText editVideoProcessorIp = settingsView.findViewById(R.id.edit_video_processor_ip);
        EditText editVideoProcessorPort = settingsView.findViewById(R.id.edit_video_processor_port);
        EditText editAudioProcessorIp = settingsView.findViewById(R.id.edit_audio_processor_ip);
        EditText editAudioProcessorPort = settingsView.findViewById(R.id.edit_audio_processor_port);
        EditText editCamera1Ip = settingsView.findViewById(R.id.edit_camera_1_ip);
        EditText editCamera1Port = settingsView.findViewById(R.id.edit_camera_1_port);
        EditText editCamera2Ip = settingsView.findViewById(R.id.edit_camera_2_ip);
        EditText editCamera2Port = settingsView.findViewById(R.id.edit_camera_2_port);

        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putString(auditoriumName + "_video_processor_ip", editVideoProcessorIp.getText().toString());
        editor.putInt(auditoriumName + "_video_processor_port", Integer.parseInt(editVideoProcessorPort.getText().toString()));
        editor.putString(auditoriumName + "_audio_processor_ip", editAudioProcessorIp.getText().toString());
        editor.putInt(auditoriumName + "_audio_processor_port", Integer.parseInt(editAudioProcessorPort.getText().toString()));
        editor.putString(auditoriumName + "_camera_1_ip", editCamera1Ip.getText().toString());
        editor.putInt(auditoriumName + "_camera_1_port", Integer.parseInt(editCamera1Port.getText().toString()));
        editor.putString(auditoriumName + "_camera_2_ip", editCamera2Ip.getText().toString());
        editor.putInt(auditoriumName + "_camera_2_port", Integer.parseInt(editCamera2Port.getText().toString()));
    }
}