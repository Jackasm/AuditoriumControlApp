package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ControlActivity extends AppCompatActivity {

    private static final String TAG = "ControlActivity"; // Тег для логгирования
    private NetworkManager networkManager;
    private boolean isUserSelection = false; // Флаг для отслеживания пользовательского выбора

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        networkManager = new NetworkManager();

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

        // Получаем настройки видеопроцессора и звукового процессора последовательно
        fetchVideoSettings(videoProcessorIp, videoProcessorPort, () -> {
            fetchAudioSettings(audioProcessorIp, audioProcessorPort);
        });
    }

    private void fetchVideoSettings(String videoProcessorIp, int videoProcessorPort, Runnable onComplete) {
        // Логгируем начало получения настроек видеопроцессора
        Log.d(TAG, "Начало получения настроек видеопроцессора");

        // Получаем список доступных видеовходов
        checkInputsAvailability(videoProcessorIp, videoProcessorPort, availableInputs -> {
            // Логгируем доступные видеовходы
            Log.d(TAG, "Доступные видеовходы: " + Arrays.toString(availableInputs));

            // Заполняем спиннеры доступными видеовходами
            updateSpinners(availableInputs);

            // Получаем текущие значения видеовходов и устанавливаем их в спиннеры
            fetchVideoOutputSettings(videoProcessorIp, videoProcessorPort, 0, () -> {
                fetchVideoOutputSettings(videoProcessorIp, videoProcessorPort, 1, () -> {
                    // Только после этого добавляем слушатели
                    setupSpinners(videoProcessorIp, videoProcessorPort);
                    onComplete.run();
                });
            });
        });
    }
    private void setupSpinners(String videoProcessorIp, int videoProcessorPort) {
        Spinner spinnerPanel = findViewById(R.id.spinner_video_output_panel);
        Spinner spinnerCamera = findViewById(R.id.spinner_video_output_camera);

        // Добавляем слушатели для спиннеров
        setupSpinnerListener(spinnerPanel, videoProcessorIp, videoProcessorPort, 0);
        setupSpinnerListener(spinnerCamera, videoProcessorIp, videoProcessorPort, 1);
    }
    private void setupSpinnerListener(Spinner spinner, String ip, int port, int outputIndex) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Игнорируем вызов, если выбор сделан программно
                if (!isUserSelection) {
                    return;
                }

                // Обработка выбора пользователя
                String selectedInput = (String) parent.getItemAtPosition(position);
                int inputCode = getInputCode(selectedInput);

                if (inputCode != -1) {
                    String command = outputIndex + ",0,1," + inputCode + "PRinp";
                    sendChangeVideoOutputCommand(ip, port, command, inputCode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Ничего не выбрано
            }
        });
    }
    private int getInputCode(String inputName) {
        switch (inputName) {
            case "Сигнал не выбран": return 0;
            case "VIA": return 4;
            case "PC": return 5;
            case "Камера 1": return 7;
            case "Камера 2": return 8;
            default: return -1;
        }
    }
    private void sendChangeVideoOutputCommand(String ipAddress, int port, String command, int inputCode) {
        attemptSendCommand(ipAddress, port, command, inputCode, 0);
    }
    private void attemptSendCommand(String ipAddress, int port, String command, int inputCode, int attempt) {
        if (attempt >= 5) {
            showToast("Не удалось переключить сигнал");
            return;
        }

        networkManager.sendCommand(ipAddress, port, command, response -> {
            if (response == null || response.equals("Ошибка соединения") || response.equals("Ошибка: пустой ответ")) {
                new Handler().postDelayed(() -> attemptSendCommand(ipAddress, port, command, inputCode, attempt + 1), 500);
                return;
            }

            try {
                String[] parts = response.split(",");
                if (parts.length >= 4 && parts[0].startsWith("PRinp")) {
                    int currentOutput = Integer.parseInt(parts[0].replace("PRinp", ""));
                    int currentInput = Integer.parseInt(parts[3]);

                    if (currentInput != inputCode) {
                        showToast("Переключить не удалось");
                        return;
                    }

                    String confirmCommand = currentOutput + ",1PUscu";
                    networkManager.sendCommand(ipAddress, port, confirmCommand, confirmResponse -> {
                        if (confirmResponse != null && confirmResponse.startsWith("PUscu" + currentOutput)) {
                            showToast("Переключение успешно");
                        } else {
                            showToast("Не удалось получить подтверждение");
                        }
                    });
                } else {
                    new Handler().postDelayed(() -> attemptSendCommand(ipAddress, port, command, inputCode, attempt + 1), 500);
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                new Handler().postDelayed(() -> attemptSendCommand(ipAddress, port, command, inputCode, attempt + 1), 500);
                showToast("Ошибка при обработке ответа: " + e.getMessage());
            }
        });
    }

    private void fetchVideoOutputSettings(String ip, int port, int outputIndex, Runnable onComplete) {
        String command = outputIndex + ",0,1,PRinp";
        // Логгируем отправку команды для видеовыхода
        Log.d(TAG, "Отправка команды для видеовыхода " + outputIndex + ": " + command);

        networkManager.sendCommand(ip, port, command, response -> {
            // Логгируем полученный ответ
            Log.d(TAG, "Получен ответ для видеовыхода " + outputIndex + ": " + response);

            parseAndSetVideoSettingsForOutput(response, outputIndex);
            onComplete.run();
        });
    }

    private void fetchAudioSettings(String audioProcessorIp, int audioProcessorPort) {
        // Логгируем начало получения настроек звукового процессора
        Log.d(TAG, "Начало получения настроек звукового процессора");

        // Массив ID для запросов громкости и MUTE
        int[][] audioSettingsIds = {
                {9, 10},  // PC: громкость, MUTE
                {11, 12}, // VIA: громкость, MUTE
                {7, 8},   // Микрофон 1: громкость, MUTE
                {5, 6},   // Микрофон 2: громкость, MUTE
                {3, 4}    // Микрофон 3: громкость, MUTE
        };

        // Начинаем последовательную отправку команд
        fetchAudioSettingsSequentially(audioProcessorIp, audioProcessorPort, audioSettingsIds, 0, 0);
    }

    private void fetchAudioSettingsSequentially(String ip, int port, int[][] audioSettingsIds, int index, int attempt) {
        if (index >= audioSettingsIds.length) {
            Log.d(TAG, "Завершение получения настроек звукового процессора");
            return;
        }

        if (attempt >= 5) {
            Log.e(TAG, "Не удалось получить настройки для устройства " + index);
            fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index + 1, 0); // Переходим к следующему устройству
            return;
        }

        final int deviceIndex = index; // Финальная копия для использования в лямбде
        int volumeId = audioSettingsIds[index][0];
        int muteId = audioSettingsIds[index][1];

        // Отправляем команду для получения громкости
        Log.d(TAG, "Отправка команды для получения громкости устройства " + deviceIndex + ": GS " + volumeId);
        networkManager.sendCommand(ip, port, "GS " + volumeId + "\r", response -> {
            if (response == null || response.trim().isEmpty()) { // Проверяем пустой ответ
                Log.w(TAG, "Пустой ответ для громкости устройства " + deviceIndex + ", повторная попытка " + (attempt + 1));
                new Handler().postDelayed(() -> fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index, attempt + 1), 500);
                return;
            }

            try {
                // Логгируем полученный ответ
                Log.d(TAG, "Получен ответ для громкости устройства " + deviceIndex + ": " + response);

                int volume = parseAudioResponse(response);
                updateVolumeSeekBar(deviceIndex, volume);

                // Отправляем команду для получения MUTE
                Log.d(TAG, "Отправка команды для получения MUTE устройства " + deviceIndex + ": GS " + muteId);
                networkManager.sendCommand(ip, port, "GS " + muteId + "\r", muteResponse -> {
                    if (muteResponse == null || muteResponse.trim().isEmpty()) { // Проверяем пустой ответ
                        Log.w(TAG, "Пустой ответ для MUTE устройства " + deviceIndex + ", повторная попытка " + (attempt + 1));
                        new Handler().postDelayed(() -> fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index, attempt + 1), 500);
                        return;
                    }

                    try {
                        // Логгируем полученный ответ
                        Log.d(TAG, "Получен ответ для MUTE устройства " + deviceIndex + ": " + muteResponse);

                        boolean isMuted = parseMuteResponse(muteResponse);
                        updateMuteButton(deviceIndex, isMuted);

                        // Переходим к следующему устройству
                        fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index + 1, 0);

                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Ошибка обработки MUTE для устройства " + deviceIndex + ": " + e.getMessage());
                        new Handler().postDelayed(() -> fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index, attempt + 1), 500);
                    }
                });

            } catch (NumberFormatException e) {
                Log.e(TAG, "Ошибка обработки громкости для устройства " + deviceIndex + ": " + e.getMessage());
                new Handler().postDelayed(() -> fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index, attempt + 1), 500);
            }
        });
    }

    private void checkInputsAvailability(String ip, int port, OnInputsAvailableListener listener) {
        int[][] inputsToCheck = {{4, 3}, {3, 3}, {6, 2}, {7, 2}};
        String[] availableInputs = new String[4];
        int[] index = {0};

        // Начинаем последовательную проверку доступности входов
        checkNextInputSequentially(ip, port, inputsToCheck, availableInputs, index, () -> {
            // Фильтруем null значения и передаем результат
            String[] filteredInputs = Arrays.stream(availableInputs)
                    .filter(input -> input != null && !input.isEmpty())
                    .toArray(String[]::new);
            listener.onInputsAvailable(filteredInputs);
        }, 0);
    }

    private void checkNextInputSequentially(String ip, int port, int[][] inputsToCheck, String[] availableInputs, int[] index, Runnable onComplete, int attempt) {
        if (attempt >= inputsToCheck.length) {
            // Все команды отправлены и обработаны
            onComplete.run();
            return;
        }

        int inputCode = inputsToCheck[attempt][0];
        int yValue = inputsToCheck[attempt][1];

        // Логгируем отправку команды для проверки доступности входа
        Log.d(TAG, "Отправка команды для проверки доступности входа: " + inputCode + "," + yValue + ",ISsva");

        checkVideoInputAvailability(ip, port, inputCode, yValue, response -> {
            // Логгируем полученный ответ
            Log.d(TAG, "Получен ответ для проверки доступности входа: " + response);

            processInputAvailabilityResponse(response, availableInputs, index[0]++);

            // Переходим к следующему входу
            checkNextInputSequentially(ip, port, inputsToCheck, availableInputs, index, onComplete, attempt + 1);
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
                int availability = Integer.parseInt(parts[2]);

                if (availability == 1) {
                    targetArray[index] = switch (inputCode) {
                        case 4 -> "PC";
                        case 3 -> "VIA";
                        case 6 -> "Камера 1";
                        case 7 -> "Камера 2";
                        default -> null;
                    };
                }
            } else {
                showToast("Некорректный формат ответа ISsva");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            showToast("Ошибка при обработке ответа ISsva: " + e.getMessage());
        }
    }

    private void updateSpinners(String[] availableInputs) {
        Spinner spinnerPanel = findViewById(R.id.spinner_video_output_panel);
        Spinner spinnerCamera = findViewById(R.id.spinner_video_output_camera);

        // Добавляем "Сигнал не выбран" в начало списка
        List<String> items = new ArrayList<>();
        items.add("Сигнал не выбран");
        items.addAll(Arrays.asList(availableInputs));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerPanel.setAdapter(adapter);
        spinnerCamera.setAdapter(adapter);
    }

    private void parseAndSetVideoSettingsForOutput(String response, int outputIndex) {
        try {
            String[] parts = response.split(",");
            if (parts.length >= 4 && parts[0].startsWith("PRinp")) {
                int currentInput = Integer.parseInt(parts[3]);
                Spinner targetSpinner = outputIndex == 0 ? findViewById(R.id.spinner_video_output_panel) : findViewById(R.id.spinner_video_output_camera);

                // Временно отключаем слушатель
                AdapterView.OnItemSelectedListener listener = targetSpinner.getOnItemSelectedListener();
                targetSpinner.setOnItemSelectedListener(null);

                // Устанавливаем значение программно
                isUserSelection = false; // Указываем, что выбор программный
                setSpinnerValue(targetSpinner, currentInput);
                isUserSelection = true; // Восстанавливаем флаг

                // Восстанавливаем слушатель
                targetSpinner.setOnItemSelectedListener(listener);
            } else {
                showToast("Некорректный формат ответа видеопроцессора");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            showToast("Ошибка при обработке ответа видеопроцессора: " + e.getMessage());
        }
    }

    private void setSpinnerValue(Spinner spinner, int currentInput) {
        if (spinner == null || currentInput < 0 || currentInput > 8) return;

        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i) instanceof String) {
                String inputName = (String) spinner.getItemAtPosition(i);
                if (("VIA".equals(inputName) && currentInput == 4) ||
                        ("PC".equals(inputName) && currentInput == 5) ||
                        ("Камера 1".equals(inputName) && currentInput == 7) ||
                        ("Камера 2".equals(inputName) && currentInput == 8)) {
                    spinner.setSelection(i, false);
                    return;
                }
            }
        }

        if (currentInput == 0) spinner.setSelection(0);
    }

    private int parseAudioResponse(String response) {
        try {
            return Integer.parseInt(response.trim());
        } catch (NumberFormatException e) {
            return 0; // В случае ошибки возвращаем 0
        }
    }

    private boolean parseMuteResponse(String response) {
        try {
            int value = Integer.parseInt(response.trim());
            return value == 65535; // 65535 означает, что MUTE активирован
        } catch (NumberFormatException e) {
            return false; // В случае ошибки возвращаем false
        }
    }

    private void updateVolumeSeekBar(int deviceIndex, int volume) {
        int seekBarId = getSeekBarId(deviceIndex);
        SeekBar seekBar = findViewById(seekBarId);
        if (seekBar != null) {
            // Преобразуем значение громкости (0-65535) в диапазон 0-100
            int progress = (int) ((volume / 65535.0) * 100);
            seekBar.setProgress(progress);
        }
    }

    private void updateMuteButton(int deviceIndex, boolean isMuted) {
        int buttonId = getMuteButtonId(deviceIndex);
        Button muteButton = findViewById(buttonId);
        if (muteButton != null) {
            muteButton.setBackgroundTintList(ColorStateList.valueOf(
                    isMuted ? Color.RED : Color.GREEN
            ));
        }
    }

    private int getSeekBarId(int deviceIndex) {
        switch (deviceIndex) {
            case 0: return R.id.seekBar_volume_pc;
            case 1: return R.id.seekBar_volume_via;
            case 2: return R.id.seekBar_volume_microphone_1;
            case 3: return R.id.seekBar_volume_microphone_2;
            case 4: return R.id.seekBar_volume_microphone_3;
            default: throw new IllegalArgumentException("Invalid device index");
        }
    }

    private int getMuteButtonId(int deviceIndex) {
        switch (deviceIndex) {
            case 0: return R.id.button_mute_pc;
            case 1: return R.id.button_mute_via;
            case 2: return R.id.button_mute_microphone_1;
            case 3: return R.id.button_mute_microphone_2;
            case 4: return R.id.button_mute_microphone_3;
            default: throw new IllegalArgumentException("Invalid device index");
        }
    }

    private String getDeviceIpForAuditorium(String auditoriumName, String deviceType) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuditoriumSettings", Context.MODE_PRIVATE);
        return sharedPreferences.getString(auditoriumName + "_" + deviceType + "_ip", null);
    }

    private int getDevicePortForAuditorium(String auditoriumName, String deviceType) {
        SharedPreferences sharedPreferences = getSharedPreferences("AuditoriumSettings", Context.MODE_PRIVATE);
        return sharedPreferences.getInt(auditoriumName + "_" + deviceType + "_port", deviceType.equals("video_processor") ? 10500 : 48631);
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