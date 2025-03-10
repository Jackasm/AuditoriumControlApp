package com.example.auditoriumcontrolapp;

import static java.lang.Thread.sleep;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private void sendChangeVideoOutputCommand(String ipAddress, int port, String command, final int inputCode) {
        attemptSendCommand(ipAddress, port, command, inputCode, 0); // Начинаем с 0 попытки
    }

    /**
     * Попытка отправки команды для изменения видеовыхода.
     *
     * @param ipAddress   IP-адрес видеопроцессора
     * @param port        Порт видеопроцессора
     * @param command     Команда для изменения видеовыхода
     * @param inputCode   Код выбранного видеовхода
     * @param attempt     Текущая попытка (максимум 5)
     */
    private void attemptSendCommand(String ipAddress, int port, String command, final int inputCode, int attempt) {
        if (attempt >= 5) {
            // Если превышено количество попыток, показываем сообщение об ошибке
            Toast.makeText(this, "Не удалось переключить сигнал", Toast.LENGTH_SHORT).show();
            return;
        }

        networkManager.sendCommand(ipAddress, port, command, response -> {
            if (response == null || response.equals("Ошибка соединения") || response.equals("Ошибка: пустой ответ")) {
                // При ошибке увеличиваем счетчик попыток и повторяем запрос
                new Handler().postDelayed(() -> attemptSendCommand(ipAddress, port, command, inputCode, attempt + 1), 500);
                return;
            }

            try {
                String[] parts = response.split(",");
                if (parts.length >= 4 && parts[0].startsWith("PRinp")) {
                    int currentOutput = Integer.parseInt(parts[0].replace("PRinp", "")); // Номер видеовыхода
                    int currentInput = Integer.parseInt(parts[3]); // Новый номер видеовхода

                    // Проверяем, что номер видеовхода совпадает с запросом
                    if (currentInput != inputCode) {
                        Toast.makeText(this, "Переключить не удалось", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Если переключение успешно, отправляем команду для подтверждения
                    String confirmCommand = currentOutput + ",1PUscu";
                    networkManager.sendCommand(ipAddress, port, confirmCommand, confirmResponse -> {
                        if (confirmResponse != null && confirmResponse.startsWith("PUscu" + currentOutput)) {
                            Toast.makeText(this, "Переключение успешно", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Не удалось получить подтверждение", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    // Если ответ не содержит PRinp, увеличиваем счетчик попыток и повторяем запрос
                    new Handler().postDelayed(() -> attemptSendCommand(ipAddress, port, command, inputCode, attempt + 1), 500);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // При ошибке парсинга увеличиваем счетчик попыток и повторяем запрос
                new Handler().postDelayed(() -> attemptSendCommand(ipAddress, port, command, inputCode, attempt + 1), 500);
                Toast.makeText(this, "Ошибка при обработке ответа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        // Создаем массивы для доступных входов
        final String[] availableInputs = new String[4]; // Для Панели


        // Массивы для индексов
        final int[] availableInputsIndex = {0};


        // Проверяем доступность PC
        checkNextInputAvailability(videoProcessorIp, videoProcessorPort, 4, 3, availableInputs, availableInputsIndex, () -> {
            // Проверяем доступность VIA
            checkNextInputAvailability(videoProcessorIp, videoProcessorPort, 3, 3, availableInputs, availableInputsIndex, () -> {
                // Проверяем доступность Камеры 1
                checkNextInputAvailability(videoProcessorIp, videoProcessorPort, 6, 2, availableInputs, availableInputsIndex, () -> {
                    // Проверяем доступность Камеры 2
                    checkNextInputAvailability(videoProcessorIp, videoProcessorPort, 7, 2, availableInputs, availableInputsIndex, () -> {
                        // После проверки всех входов обновляем спиннеры
                        updateSpinners(availableInputs);
                    }, 0); // Начинаем с 0 попытки
                }, 0); // Начинаем с 0 попыток
            }, 0); // Начинаем с 0 попыток
        }, 0); // Начинаем с 0 попыток

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
    private void checkVideoInputAvailability(String ipAddress, int port, int inputCode, int yValue, NetworkManager.OnResponseListener listener) {
        String command = inputCode + "," + yValue + ",ISsva";
        networkManager.sendCommand(ipAddress, port, command, listener);
    }
    private void processInputAvailabilityResponse(String response, String[] targetArray, int index) {
        try {
            String[] parts = response.split(",");
            if (parts.length >= 3 && parts[0].startsWith("ISsva")) {
                int inputCode = Integer.parseInt(parts[0].replace("ISsva", ""));
                int yValue = Integer.parseInt(parts[1]);
                int availability = Integer.parseInt(parts[2]);

                // Если вход доступен (N = 1), добавляем его в массив
                if (availability == 1) {
                    switch (inputCode) {
                        case 4:
                            targetArray[index] = "PC";
                            break;
                        case 3:
                            targetArray[index] = "VIA";
                            break;
                        case 6:
                            targetArray[index] = "Камера 1";
                            break;
                        case 7:
                            targetArray[index] = "Камера 2";
                            break;
                        default:
                            targetArray[index] = null; // Недопустимый вход
                    }
                }
            } else {
                Toast.makeText(this, "Некорректный формат ответа ISsva", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            Toast.makeText(this, "Ошибка при обработке ответа ISsva: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void checkNextInputAvailability(String ipAddress, int port, int inputCode, int yValue, String[] targetArray, int[] indexArray, Runnable nextStep, int attempt) {
        if (attempt >= 5) {
            // Если превышено количество попыток, показываем сообщение об ошибке
            Toast.makeText(this, "Не удалось проверить доступность видеовхода", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(nextStep, 50); // Переходим к следующему шагу
            return;
        }

        checkVideoInputAvailability(ipAddress, port, inputCode, yValue, response -> {
            if (response == null || response.equals("Ошибка соединения") || response.equals("Ошибка: пустой ответ")) {
                // При ошибке увеличиваем счетчик попыток и повторяем запрос
                new Handler().postDelayed(() -> checkNextInputAvailability(ipAddress, port, inputCode, yValue, targetArray, indexArray, nextStep, attempt + 1), 50);
                return;
            }

            try {
                String[] parts = response.split(",");
                if (parts.length >= 3 && parts[0].startsWith("ISsva")) {
                    // Если ответ содержит ISsva, обрабатываем его
                    processInputAvailabilityResponse(response, targetArray, indexArray[0]++);
                    new Handler().postDelayed(nextStep, 50); // Переходим к следующему шагу
                } else {
                    // Если ответ не содержит ISsva, увеличиваем счетчик попыток и повторяем запрос
                    new Handler().postDelayed(() -> checkNextInputAvailability(ipAddress, port, inputCode, yValue, targetArray, indexArray, nextStep, attempt + 1), 50);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // При ошибке парсинга увеличиваем счетчик попыток и повторяем запрос
                Toast.makeText(this, "Ошибка при обработке ответа ISsva: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                new Handler().postDelayed(() -> checkNextInputAvailability(ipAddress, port, inputCode, yValue, targetArray, indexArray, nextStep, attempt + 1), 50);
            }
        });
    }
    private void updateSpinners(String[] availableInputsPanel) {
        // Находим спиннеры
        Spinner spinnerPanel = findViewById(R.id.spinner_video_output_panel);
        Spinner spinnerCamera = findViewById(R.id.spinner_video_output_camera);

        // Фильтруем доступные входы
        String[] filteredPanelInputs = filterAvailableInputs(availableInputsPanel);
        String[] filteredCameraInputs = filterAvailableInputs(availableInputsPanel);

        // Устанавливаем новые адаптеры для спиннеров
        ArrayAdapter<String> panelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredPanelInputs);
        panelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPanel.setAdapter(panelAdapter);

        ArrayAdapter<String> cameraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, filteredCameraInputs);
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCamera.setAdapter(cameraAdapter);

        // Добавляем "Сигнал не выбран" как первый элемент
        addDefaultOption(spinnerPanel, "Сигнал не выбран");
        addDefaultOption(spinnerCamera, "Сигнал не выбран");
    }
    private String[] filterAvailableInputs(String[] inputs) {
        List<String> filteredList = new ArrayList<>();

        for (String input : inputs) {
            if (input != null && !input.isEmpty()) {
                filteredList.add(input);
            }
        }

        return filteredList.toArray(new String[0]); // Преобразуем список в массив
    }
    private void addDefaultOption(Spinner spinner, String defaultOption) {
        List<String> items = new ArrayList<>();
        items.add(defaultOption); // Добавляем "Сигнал не выбран"
        for (int i = 0; i < spinner.getCount(); i++) {
            String item = (String) spinner.getItemAtPosition(i);
            if (item != null && !item.isEmpty()) {
                items.add(item);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items.toArray(new String[0]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
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