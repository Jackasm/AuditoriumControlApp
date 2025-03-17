package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        // Предварительная инициализация настроек для всех аудиторий
        initializeDefaultSettings();

        // Настройка кнопок для каждой аудитории
        setupAuditoriumButton("Аудитория 308");
        setupAuditoriumButton("Аудитория 203");
        setupAuditoriumButton("Аудитория 206");
        setupAuditoriumButton("Аудитория 503");

        // Настройка кнопки "Настройки"
        TextView buttonSettings = findViewById(R.id.button_settings);
        if (buttonSettings != null) {
            buttonSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
            });
        }
    }

    /**
     * Предварительная инициализация настроек для всех аудиторий.
     */
    private void initializeDefaultSettings() {
        SharedPreferences sharedPreferences = getSharedPreferences("AuditoriumSettings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Аудитория 1
        editor.putString("Аудитория 308_video_processor_ip", "172.17.60.204");
        editor.putString("Аудитория 308_audio_processor_ip", "172.17.60.206");
        editor.putString("Аудитория 308_camera_1_ip", "172.17.60.208");
        editor.putString("Аудитория 308_camera_2_ip", "172.17.60.209");

        // Аудитория 2
        editor.putString("Аудитория 203_video_processor_ip", "172.17.60.104");
        editor.putString("Аудитория 203_audio_processor_ip", "172.17.60.106");
        editor.putString("Аудитория 203_camera_1_ip", "172.17.60.108");
        editor.putString("Аудитория 203_camera_2_ip", "172.17.60.109");

        // Аудитория 3
        editor.putString("Аудитория 206_video_processor_ip", "172.17.60.154");
        editor.putString("Аудитория 206_audio_processor_ip", "172.17.60.156");
        editor.putString("Аудитория 206_camera_1_ip", "172.17.60.158");
        editor.putString("Аудитория 206_camera_2_ip", "172.17.60.159");

        // Аудитория 4
        editor.putString("Аудитория 503_video_processor_ip", "172.17.60.7");
        editor.putString("Аудитория 503_audio_processor_ip", "172.17.60.6");
        editor.putString("Аудитория 503_camera_1_ip", "172.17.60.8");
        editor.putString("Аудитория 503_camera_2_ip", "172.17.60.9");

        editor.apply(); // Сохраняем настройки
    }

    /**
     * Настройка кнопки для конкретной аудитории.
     *
     * @param auditoriumName Название аудитории
     */
    private void setupAuditoriumButton(String auditoriumName) {
        TextView button = findViewById(getAuditoriumButtonId(auditoriumName));
        if (button != null) {
            button.setOnClickListener(v -> {
                String[] deviceIps = getDeviceIpsForAuditorium(auditoriumName);
                if (deviceIps == null || deviceIps.length != 4) {
                    Toast.makeText(this, "Настройки для " + auditoriumName + " не найдены", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Запускаем ControlActivity с передачей данных
                Intent intent = new Intent(this, ControlActivity.class);
                intent.putExtra("auditorium_name", auditoriumName);
                startActivity(intent);
            });
        }
    }

    /**
     * Возвращает ID кнопки для конкретной аудитории.
     *
     * @param auditoriumName Название аудитории
     * @return ID кнопки или -1, если название не найдено
     */
    private int getAuditoriumButtonId(String auditoriumName) {
        switch (auditoriumName) {
            case "Аудитория 308":
                return R.id.text_auditorium_308;
            case "Аудитория 203":
                return R.id.text_auditorium_203;
            case "Аудитория 206":
                return R.id.text_auditorium_206;
            case "Аудитория 503":
                return R.id.text_auditorium_503;
            default:
                return -1; // Недопустимое значение
        }
    }

    /**
     * Получает IP-адреса устройств для конкретной аудитории из SharedPreferences.
     *
     * @param auditoriumName Название аудитории
     * @return Массив IP-адресов [videoprocessor, audioprocessor, camera1, camera2]
     */
    private String[] getDeviceIpsForAuditorium(String auditoriumName) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuditoriumSettings", Context.MODE_PRIVATE);

        String videoProcessorIp = sharedPreferences.getString(auditoriumName + "_video_processor_ip", null);
        String audioProcessorIp = sharedPreferences.getString(auditoriumName + "_audio_processor_ip", null);
        String camera1Ip = sharedPreferences.getString(auditoriumName + "_camera_1_ip", null);
        String camera2Ip = sharedPreferences.getString(auditoriumName + "_camera_2_ip", null);

        if (videoProcessorIp == null || audioProcessorIp == null || camera1Ip == null || camera2Ip == null) {
            return null; // Настройки отсутствуют или некорректны
        }

        return new String[]{videoProcessorIp, audioProcessorIp, camera1Ip, camera2Ip};
    }


}