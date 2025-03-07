package com.example.auditoriumcontrolapp;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ControlActivity extends AppCompatActivity {


    private NetworkManager networkManager;
    // Флаги для игнорирования первого выбора
    private boolean isSpinnerPanelInitialized = false;
    private boolean isSpinnerCameraInitialized = false;

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

        String videoProcessorIp = getDeviceIpForAuditorium(auditoriumName, "video_processor");
        int videoProcessorPort = getDevicePortForAuditorium(auditoriumName, "video_processor");

        if (videoProcessorIp == null) {
            Toast.makeText(this, "IP-адрес видеопроцессора не найден", Toast.LENGTH_SHORT).show();
            return;
        }

        // Находим спиннеры
        Spinner spinnerPanel = findViewById(R.id.spinner_video_output_panel);
        Spinner spinnerCamera = findViewById(R.id.spinner_video_output_camera);

        // Добавляем слушатель для спиннера "Панель"
        spinnerPanel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Игнорируем первый выбор при загрузке активности
                if (!isSpinnerPanelInitialized) {
                    isSpinnerPanelInitialized = true; // Устанавливаем флаг
                    return;
                }

                String selectedInput = (String) parent.getItemAtPosition(position);
                int inputCode = getInputCode(selectedInput);

                if (inputCode != -1) {
                    // Отправляем команду для изменения видеовыхода "Панель"
                    String command = "0,0,1," + inputCode + "PRinp";
                    sendChangeVideoOutputCommand(videoProcessorIp, videoProcessorPort, command, inputCode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не выбрано
            }
        });

        // Добавляем слушатель для спиннера "Камера"
        spinnerCamera.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Игнорируем первый выбор при загрузке активности
                if (!isSpinnerCameraInitialized) {
                    isSpinnerCameraInitialized = true; // Устанавливаем флаг
                    return;
                }

                String selectedInput = (String) parent.getItemAtPosition(position);
                int inputCode = getInputCode(selectedInput);

                if (inputCode != -1) {
                    // Отправляем команду для изменения видеовыхода "Камера"
                    String command = "1,0,1," + inputCode + "PRinp";
                    sendChangeVideoOutputCommand(videoProcessorIp, videoProcessorPort, command, inputCode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не выбрано
            }
        });
        // Загружаем текущие настройки для всех устройств
        fetchCurrentSettings(auditoriumName);
        int i = 0;
    }
    private int getInputCode(String inputName) {
        switch (inputName) {
            case "Сигнал не выбран":
                return 0;
            case "VIA":
                return 4;
            case "PC":
                return 5;
            case "Камера 1":
                return 7;
            case "Камера 2":
                return 8;
            default:
                return -1; // Недопустимое значение
        }
    }
    private void sendChangeVideoOutputCommand(String ipAddress, int port, String command, int inputCode) {
        networkManager.sendCommand(ipAddress, port, command, response -> {
            if (response != null && !response.equals("Ошибка соединения") && !response.equals("Ошибка: пустой ответ")) {
                try {
                    String[] parts = response.split(",");
                    if (parts.length >= 4 && parts[0].startsWith("PUscu")){
                        int currentOutput = Integer.parseInt(parts[0].replace("PRinp", "")); // Номер видеовыхода
                        int currentInput = Integer.parseInt(parts[3]); // Новый номер видеовхода

                        // Проверяем, что номер видеовыхода совпадает с запросом
                        if (currentInput != inputCode) {
                            Toast.makeText(this, "Переключить не удалось", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Если переключение успешно, отправляем команду для подтверждения
                        String confirmCommand = "0,1PUscu";
                        networkManager.sendCommand(ipAddress, port, confirmCommand, confirmResponse -> {
                            if (confirmResponse != null && confirmResponse.startsWith("PUscu"))
                                return;
                             else {
                                Toast.makeText(this, "Не удалось получить подтверждение", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                       // Toast.makeText(this, "Некорректный формат ответа видеопроцессора", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    Toast.makeText(this, "Ошибка при обработке ответа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Не удалось отправить команду", Toast.LENGTH_SHORT).show();
            }
        });
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
        networkManager.sendCommand(videoProcessorIp, videoProcessorPort, commandForOutput0, cameraresponse -> {
            if (cameraresponse != null && !cameraresponse.equals("Ошибка соединения") && !cameraresponse.equals("Ошибка: пустой ответ")) {
                parseAndSetVideoSettingsForOutput(cameraresponse, 0); // Обработка ответа для видеовыхода "Панель"
            } else {
                Toast.makeText(this, "Не удалось получить настройки для видеовыхода 'Панель'", Toast.LENGTH_SHORT).show();
            }

            // Добавляем задержку перед отправкой второй команды
            new Handler().postDelayed(() -> {
                // Отправляем команду для видеовыхода "Камера" (X = 1)
                String commandForOutput1 = "1,0,1,PRinp";
                networkManager.sendCommand(videoProcessorIp, videoProcessorPort, commandForOutput1, response -> {
                    if (response != null && !response.equals("Ошибка соединения") && !response.equals("Ошибка: пустой ответ")) {
                        parseAndSetVideoSettingsForOutput(response, 1); // Обработка ответа для видеовыхода "Камера"
                    } else {
                        Toast.makeText(this, "Не удалось получить настройки для видеовыхода 'Камера'", Toast.LENGTH_SHORT).show();
                    }
                });
            }, 50); // Задержка 1 секунда (1000 мс)
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
                if (("VIA".equals(inputName) && currentInput == 4) ||
                        ("PC".equals(inputName) && currentInput == 5) ||
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