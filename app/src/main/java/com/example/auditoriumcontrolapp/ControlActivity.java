package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ControlActivity extends AppCompatActivity {

    private String ipAddress;
    private int port;
    private NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        networkManager = new NetworkManager();

        // Получаем название аудитории из Intent
        String auditoriumName = getIntent().getStringExtra("auditorium_name");
        if (auditoriumName == null || auditoriumName.isEmpty()) {
            Toast.makeText(this, "Название аудитории не передано", Toast.LENGTH_SHORT).show();
            finish(); // Закрываем активность
            return;
        }

        setTitle(auditoriumName);

        // Загружаем текущие настройки для всех устройств
        fetchCurrentSettings(auditoriumName);
    }

    private void fetchCurrentSettings(String auditoriumName) {
        // Получаем IP-адрес и порт видеопроцессора
        String videoProcessorIp = getDeviceIpForAuditorium(auditoriumName, "video_processor");
        int videoProcessorPort = getDevicePortForAuditorium(auditoriumName, "video_processor");

        if (videoProcessorIp == null) {
            Toast.makeText(this, "IP-адрес видеопроцессора не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        // Отправляем команду для видеовыхода "Панель" (X = 0)
        String commandForOutput0 = "0,0,1,PRinp";
        networkManager.sendCommand(videoProcessorIp, videoProcessorPort, commandForOutput0, response -> {
            if (response != null && !response.equals("Ошибка соединения") && !response.equals("Ошибка: пустой ответ")) {
                parseAndSetVideoSettingsForOutput(response, 0); // Обработка ответа для видеовыхода "Панель"
            } else {
                Toast.makeText(this, "Не удалось получить настройки для видеовыхода 'Панель'", Toast.LENGTH_SHORT).show();
            }
        });

        // Отправляем команду для видеовыхода "Камера" (X = 1)
        String commandForOutput1 = "1,0,1,PRinp";
        networkManager.sendCommand(videoProcessorIp, videoProcessorPort, commandForOutput1, response -> {
            if (response != null && !response.equals("Ошибка соединения") && !response.equals("Ошибка: пустой ответ")) {
                parseAndSetVideoSettingsForOutput(response, 1); // Обработка ответа для видеовыхода "Камера"
            } else {
                Toast.makeText(this, "Не удалось получить настройки для видеовыхода 'Камера'", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String getDeviceIpForAuditorium(String auditoriumName, String deviceType) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuditoriumSettings", Context.MODE_PRIVATE);

        switch (deviceType) {
            case "video_processor":
                return sharedPreferences.getString(auditoriumName + "_video_processor_ip", null);
            case "audio_processor":
                return sharedPreferences.getString(auditoriumName + "_audio_processor_ip", null);
            case "camera_1":
                return sharedPreferences.getString(auditoriumName + "_camera_1_ip", null);
            case "camera_2":
                return sharedPreferences.getString(auditoriumName + "_camera_2_ip", null);
            default:
                return null;
        }
    }
    private int getDevicePortForAuditorium(String auditoriumName, String deviceType) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuditoriumSettings", Context.MODE_PRIVATE);

        switch (deviceType) {
            case "video_processor":
                return sharedPreferences.getInt(auditoriumName + "_video_processor_port", 10500);
            case "audio_processor":
                return sharedPreferences.getInt(auditoriumName + "_audio_processor_port", 48631);
            case "camera_1":
            case "camera_2":
                return sharedPreferences.getInt(auditoriumName + "_camera_1_port", 5678); // Порты камер одинаковые
            default:
                return -1; // Недопустимый тип устройства
        }
    }
    private void parseAndSetVideoSettingsForOutput(String response, int outputIndex) {
        try {
            String[] parts = response.split(",");
            if (parts.length >= 4 && parts[0].startsWith("PRinp")) {
                // Извлекаем номер видеовыхода (X) и видеовхода (Y)

                int currentInput = Integer.parseInt(parts[3]); // Номер видеовхода

                // Находим соответствующий спинер
                Spinner targetSpinner;
                if (outputIndex == 0) { // Видеовыход "Панель"
                    targetSpinner = findViewById(R.id.spinner_video_output_panel);
                } else if (outputIndex == 1) { // Видеовыход "Камера"
                    targetSpinner = findViewById(R.id.spinner_video_output_camera);
                } else {
                    return; // Недопустимый индекс видеовыхода
                }

                // Устанавливаем значение в спинер
                setSpinnerValue(targetSpinner, currentInput);
            } else {
                Toast.makeText(this, "Некорректный формат ответа видеопроцессора", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Toast.makeText(this, "Ошибка при обработке ответа видеопроцессора: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void setSpinnerValue(Spinner spinner, int currentInput) {
        if (spinner == null || currentInput < 0 || currentInput > 8) {
            return; // Пропускаем, если спинер не найден или вход некорректный
        }

        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i) instanceof String) {
                String inputName = (String) spinner.getItemAtPosition(i);
                if (("PC".equals(inputName) && currentInput == 4) ||
                        ("VIA".equals(inputName) && currentInput == 5) ||
                        ("Камера 1".equals(inputName) && currentInput == 7) ||
                        ("Камера 2".equals(inputName) && currentInput == 8)) {
                    spinner.setSelection(i);
                    return;
                }
            }
        }

        // Если currentInput == 0, устанавливаем "Сигнал не выбран"
        if (currentInput == 0) {
            spinner.setSelection(0); // Предполагаем, что "Сигнал не выбран" находится на позиции 0
        }
    }
}