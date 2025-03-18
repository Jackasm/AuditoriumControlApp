package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ControlActivity extends AppCompatActivity {

    private static final String TAG = "ControlActivity"; // Тег для логгирования




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);



        String auditoriumName = getIntent().getStringExtra("auditorium_name");
        if (auditoriumName == null || auditoriumName.isEmpty()) {
            showToastAndFinish("Название аудитории не передано");
            return;
        }

        setTitle(auditoriumName);

        String videoProcessorIp = getDeviceIpForAuditorium(auditoriumName, "video_processor");
        int videoProcessorPort = getDevicePortForAuditorium(auditoriumName, "video_processor");

        String audioProcessorIp = getDeviceIpForAuditorium(auditoriumName, "audio_processor");
        int audioProcessorPort = getDevicePortForAuditorium(auditoriumName, "audio_processor");

        if (videoProcessorIp == null || audioProcessorIp == null) {
            showToast("IP-адрес видеопроцессора или звукового процессора не найден");
            return;
        }

        // Логгируем начало получения настроек
        Log.d(TAG, "Начало получения настроек для аудитории: " + auditoriumName);
        Spinner spinnerPanel = findViewById(R.id.spinner_video_output_panel);
        Spinner spinnerCamera = findViewById(R.id.spinner_video_output_camera);
        VideoSettingsFetcher videoSettingsFetcher = new VideoSettingsFetcher(
                this,
                videoProcessorIp,
                videoProcessorPort,
                spinnerPanel,
                spinnerCamera
        );
        SeekBar[] seekBars = {
                findViewById(R.id.seekBar_volume_pc),
                findViewById(R.id.seekBar_volume_via),
                findViewById(R.id.seekBar_volume_microphone_1),
                findViewById(R.id.seekBar_volume_microphone_2),
                findViewById(R.id.seekBar_volume_microphone_3)
        };

        TextView[] muteButtons = {
                findViewById(R.id.text_mute_pc),
                findViewById(R.id.text_mute_via),
                findViewById(R.id.text_mute_microphone_1),
                findViewById(R.id.text_mute_microphone_2),
                findViewById(R.id.text_mute_microphone_3)
        };
        AudioSettingsFetcher audioSettingsFetcher = new AudioSettingsFetcher(
                this,
                audioProcessorIp,
                audioProcessorPort,
                seekBars,
                muteButtons
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        executor.submit(() -> videoSettingsFetcher.fetchSettings(null));
        executor.submit(() -> audioSettingsFetcher.fetchSettings(null));
        executor.shutdown();


        // Настройка кнопок для управления камерами
        setupCameraButtons(auditoriumName);
    }


    private void setupCameraButtons(String auditoriumName) {
        TextView buttonCamera1 = findViewById(R.id.text_camera_1);
        TextView buttonCamera2 = findViewById(R.id.text_camera_2);

        buttonCamera1.setOnClickListener(view -> {
            String cameraIp = getDeviceIpForAuditorium(auditoriumName, "camera_1");
            int cameraPort = getDevicePortForAuditorium(auditoriumName, "camera_1");

            if (cameraIp == null) {
                showToast("IP-адрес Камеры 1 не найден");
                return;
            }

            // Запускаем новую активность для управления Камерой 1
            Intent intent = new Intent(this, CameraControlActivity.class);
            intent.putExtra("camera_ip", cameraIp);
            intent.putExtra("camera_port", cameraPort);
            startActivity(intent);
        });

        buttonCamera2.setOnClickListener(view -> {
            String cameraIp = getDeviceIpForAuditorium(auditoriumName, "camera_2");
            int cameraPort = getDevicePortForAuditorium(auditoriumName, "camera_2");

            if (cameraIp == null) {
                showToast("IP-адрес Камеры 2 не найден");
                return;
            }

            // Запускаем новую активность для управления Камерой 2
            Intent intent = new Intent(this, CameraControlActivity.class);
            intent.putExtra("camera_ip", cameraIp);
            intent.putExtra("camera_port", cameraPort);
            startActivity(intent);
        });
    }
    private String getDeviceIpForAuditorium(String auditoriumName, String deviceType) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuditoriumSettings", Context.MODE_PRIVATE);
        return sharedPreferences.getString(auditoriumName + "_" + deviceType + "_ip", null);
    }

    private int getDevicePortForAuditorium(String auditoriumName, String deviceType) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuditoriumSettings", Context.MODE_PRIVATE);

        // Определяем ключ для порта в зависимости от типа устройства
        String portKey;
        switch (deviceType) {
            case "video_processor":
                portKey = auditoriumName + "_video_processor_port";
                break;
            case "audio_processor":
                portKey = auditoriumName + "_audio_processor_port";
                break;
            case "camera_1":
                portKey = auditoriumName + "_camera_1_port";
                break;
            case "camera_2":
                portKey = auditoriumName + "_camera_2_port";
                break;
            default:
                // Если тип устройства неизвестен, возвращаем порт по умолчанию
                return 48631;
        }

        // Получаем порт из SharedPreferences
        return sharedPreferences.getInt(portKey, getDefaultPortForDevice(deviceType));
    }

    // Метод для получения порта по умолчанию в зависимости от типа устройства
    private int getDefaultPortForDevice(String deviceType) {
        switch (deviceType) {
            case "video_processor":
                return 10500;
            case "audio_processor":
                return 48631;
            case "camera_1":
            case "camera_2":
                return 5678;
            default:
                return 48631; // Порт по умолчанию для неизвестных устройств
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showToastAndFinish(String message) {
        showToast(message);
        finish();
    }

    interface OnInputsAvailableListener {
        void onInputsAvailable(String[] availableInputs);
    }
}