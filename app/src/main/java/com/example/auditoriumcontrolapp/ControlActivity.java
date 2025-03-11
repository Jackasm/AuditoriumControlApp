package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ControlActivity extends AppCompatActivity {

    private NetworkManager networkManager;
    private boolean isSpinnerPanelInitialized = false;
    private boolean isSpinnerCameraInitialized = false;

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

        if (videoProcessorIp == null) {
            showToast("IP-адрес видеопроцессора не найден");
            return;
        }

        setupSpinners(videoProcessorIp, videoProcessorPort);
        fetchCurrentSettings(auditoriumName);
    }

    private void setupSpinners(String videoProcessorIp, int videoProcessorPort) {
        Spinner spinnerPanel = findViewById(R.id.spinner_video_output_panel);
        Spinner spinnerCamera = findViewById(R.id.spinner_video_output_camera);

        setupSpinnerListener(spinnerPanel, videoProcessorIp, videoProcessorPort, 0, () -> isSpinnerPanelInitialized = true);
        setupSpinnerListener(spinnerCamera, videoProcessorIp, videoProcessorPort, 1, () -> isSpinnerCameraInitialized = true);
    }

    private void setupSpinnerListener(Spinner spinner, String ip, int port, int outputIndex, Runnable onInitialized) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Проверяем флаг инициализации
                if ((outputIndex == 0 && !isSpinnerPanelInitialized) || (outputIndex == 1 && !isSpinnerCameraInitialized)) {
                    onInitialized.run(); // Устанавливаем флаг инициализации
                    return;
                }

                String selectedInput = (String) parent.getItemAtPosition(position);
                int inputCode = getInputCode(selectedInput);

                if (inputCode != -1) {
                    String command = outputIndex + ",0,1," + inputCode + "PRinp";
                    sendChangeVideoOutputCommand(ip, port, command, inputCode);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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

    private void fetchCurrentSettings(String auditoriumName) {
        String videoProcessorIp = getDeviceIpForAuditorium(auditoriumName, "video_processor");
        int videoProcessorPort = getDevicePortForAuditorium(auditoriumName, "video_processor");

        if (videoProcessorIp == null) {
            showToast("IP-адрес видеопроцессора не найден");
            return;
        }

        // Создаем массив для доступных входов
        String[] availableInputs = new String[4];

        // Проверяем доступность входов и обновляем спиннеры
        checkInputsAvailability(videoProcessorIp, videoProcessorPort, availableInputs, () -> {
            updateSpinners(availableInputs); // Передаем массив availableInputs
        });

        sendCommandWithDelay(videoProcessorIp, videoProcessorPort, "0,0,1,PRinp", 0, response -> {
            parseAndSetVideoSettingsForOutput(response, 0);
            sendCommandWithDelay(videoProcessorIp, videoProcessorPort, "1,0,1,PRinp", 50, response1 -> parseAndSetVideoSettingsForOutput(response1, 1));
        });
    }

    private void checkInputsAvailability(String ip, int port, String[] availableInputs, Runnable onComplete) {
        int[][] inputsToCheck = {{4, 3}, {3, 3}, {6, 2}, {7, 2}};
        int[] index = {0};

        checkNextInput(ip, port, inputsToCheck, availableInputs, index, onComplete, 0);
    }

    private void checkNextInput(String ip, int port, int[][] inputsToCheck, String[] availableInputs, int[] index, Runnable onComplete, int attempt) {
        if (attempt >= inputsToCheck.length) {
            onComplete.run(); // Вызываем onComplete после проверки всех входов
            return;
        }

        int inputCode = inputsToCheck[attempt][0];
        int yValue = inputsToCheck[attempt][1];

        checkVideoInputAvailability(ip, port, inputCode, yValue, response -> {
            processInputAvailabilityResponse(response, availableInputs, index[0]++);
            checkNextInput(ip, port, inputsToCheck, availableInputs, index, onComplete, attempt + 1);
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

        List<String> panelItems = Stream.concat(Stream.of("Сигнал не выбран"), Arrays.stream(availableInputs).filter(input -> input != null && !input.isEmpty()))
                .collect(Collectors.toList());

        ArrayAdapter<String> panelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, panelItems);
        panelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPanel.setAdapter(panelAdapter);

        ArrayAdapter<String> cameraAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, panelItems);
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCamera.setAdapter(cameraAdapter);
    }

    private void sendCommandWithDelay(String ip, int port, String command, int delay, NetworkManager.OnResponseListener listener) {
        new Handler().postDelayed(() -> networkManager.sendCommand(ip, port, command, listener), delay);
    }

    private void parseAndSetVideoSettingsForOutput(String response, int outputIndex) {
        try {
            String[] parts = response.split(",");
            if (parts.length >= 4 && parts[0].startsWith("PRinp")) {
                int currentInput = Integer.parseInt(parts[3]);
                Spinner targetSpinner = outputIndex == 0 ? findViewById(R.id.spinner_video_output_panel) : findViewById(R.id.spinner_video_output_camera);
                setSpinnerValue(targetSpinner, currentInput);
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
}