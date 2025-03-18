package com.example.auditoriumcontrolapp;



import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VideoSettingsFetcher implements SettingsFetcher {
    private final Context context;
    private static final String TAG = "VideoSettingsFetcher";
    private final String videoProcessorIp;
    private final int videoProcessorPort;
    private final NetworkManager networkManager;
    private final Spinner spinnerPanel;
    private final Spinner spinnerCamera;
    private boolean isUserSelection = false; // Флаг для отслеживания пользовательского выбора

    public VideoSettingsFetcher(Context context, String ip, int port, Spinner spinnerPanel, Spinner spinnerCamera) {
        this.context = context;
        this.videoProcessorIp = ip;
        this.videoProcessorPort = port;
        this.networkManager = new NetworkManager();
        this.spinnerPanel = spinnerPanel;
        this.spinnerCamera = spinnerCamera;
    }

    @Override
    public void fetchSettings(Runnable onComplete) {
        Log.d("VideoSettingsFetcher", "Начало получения настроек видеопроцессора");
        String [] availableInputsDefault = new String[0];

        updateSpinners(availableInputsDefault);
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

                });
            });
        });
    }

    // Остальные методы из оригинального fetchVideoSettings()
    // ...
    private void checkInputsAvailability(String ip, int port, ControlActivity.OnInputsAvailableListener listener) {
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
    private void updateSpinners(String[] availableInputs) {
        // Добавляем "Сигнал не выбран" в начало списка
        List<String> items = new ArrayList<>();
        items.add("Сигнал не выбран");
        items.addAll(Arrays.asList(availableInputs));

        // Загружаем шрифт
        Typeface bohemaPink = null;
        if (context != null) {
            bohemaPink = context.getResources().getFont(R.font.bohema_pink);
        }

        // Создаем кастомный адаптер
        CustomSpinnerAdapter adapter = new CustomSpinnerAdapter(
                context, // Контекст
                android.R.layout.simple_spinner_item, // Стандартный макет для элемента
                items.toArray(new String[0]), // Данные для Spinner
                bohemaPink, // Шрифт
                Color.BLUE // Цвет текста (синий)
        );

        // Устанавливаем макет для выпадающего списка
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Применяем адаптер к Spinner
        if (spinnerPanel != null) spinnerPanel.setAdapter(adapter);
        if (spinnerCamera != null) spinnerCamera.setAdapter(adapter);
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
    private void setupSpinners(String videoProcessorIp, int videoProcessorPort) {
        if (spinnerPanel != null && spinnerCamera != null) {
            setupSpinnerListener(spinnerPanel, videoProcessorIp, videoProcessorPort, 0);
            setupSpinnerListener(spinnerCamera, videoProcessorIp, videoProcessorPort, 1);
        }
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
    private void parseAndSetVideoSettingsForOutput(String response, int outputIndex) {
        try {
            String[] parts = response.split(",");
            if (parts.length >= 4 && parts[0].startsWith("PRinp")) {
                int currentInput = Integer.parseInt(parts[3]);
                Spinner targetSpinner = (outputIndex == 0) ? spinnerPanel : spinnerCamera;

                if (targetSpinner != null) {
                    // Временно отключаем слушатель
                    AdapterView.OnItemSelectedListener listener = targetSpinner.getOnItemSelectedListener();
                    targetSpinner.setOnItemSelectedListener(null);

                    // Устанавливаем значение программно
                    isUserSelection = false; // Указываем, что выбор программный
                    setSpinnerValue(targetSpinner, currentInput);
                    isUserSelection = true; // Восстанавливаем флаг

                    // Восстанавливаем слушатель
                    targetSpinner.setOnItemSelectedListener(listener);
                }
            } else {
                showToast("Некорректный формат ответа видеопроцессора");
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            showToast("Ошибка при обработке ответа видеопроцессора: " + e.getMessage());
        }
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
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
    private void checkVideoInputAvailability(String ipAddress, int port, int inputCode, int yValue, NetworkManager.OnResponseListener listener) {
        String command = inputCode + "," + yValue + ",ISsva";
        networkManager.sendCommand(ipAddress, port, command, listener);
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
}
