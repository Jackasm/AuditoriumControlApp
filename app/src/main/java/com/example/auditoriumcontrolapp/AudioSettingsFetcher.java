package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class AudioSettingsFetcher implements SettingsFetcher {
    private final Context context;
    private final String audioProcessorIp;
    private final int audioProcessorPort;
    private final NetworkManager networkManager;
    private static final String TAG = "AudioSettingsFetcher";

    // Добавляем поля для UI элементов
    private final SeekBar[] seekBars;
    private final TextView[] muteButtons;

    public AudioSettingsFetcher(Context context, String ip, int port,
                                SeekBar[] seekBars, TextView[] muteButtons) {
        this.context = context;
        this.audioProcessorIp = ip;
        this.audioProcessorPort = port;
        this.networkManager = new NetworkManager();
        this.seekBars = seekBars;
        this.muteButtons = muteButtons;
    }

    @Override
    public void fetchSettings(Runnable onComplete) {
        Log.d(TAG, "Начало получения настроек звукового процессора");

        int[][] audioSettingsIds = {
                {9, 10},  // PC: громкость, MUTE
                {11, 12}, // VIA: громкость, MUTE
                {7, 8},   // Микрофон 1: громкость, MUTE
                {5, 6},   // Микрофон 2: громкость, MUTE
                {3, 4}    // Микрофон 3: громкость, MUTE
        };

        fetchAudioSettingsSequentially(audioProcessorIp, audioProcessorPort,
                audioSettingsIds, 0, 0);

        setupVolumeSeekBars(audioProcessorIp, audioProcessorPort);
        setupMuteButtons(audioProcessorIp, audioProcessorPort);

        if (onComplete != null) onComplete.run();
    }

    private void fetchAudioSettingsSequentially(String ip, int port,
                                                int[][] audioSettingsIds,
                                                int index, int attempt) {
        if (index >= audioSettingsIds.length) {
            Log.d(TAG, "Завершение получения настроек звукового процессора");
            return;
        }

        if (attempt >= 5) {
            Log.e(TAG, "Не удалось получить настройки для устройства " + index);
            fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index + 1, 0);
            return;
        }

        final int deviceIndex = index;
        int volumeId = audioSettingsIds[index][0];
        int muteId = audioSettingsIds[index][1];

        // Получение громкости
        Log.d(TAG, "Отправка команды для получения громкости устройства " + deviceIndex + ": GS " + volumeId);
        networkManager.sendCommand(ip, port, "GS " + volumeId + "\r", response -> {
            if (response == null || response.trim().isEmpty()) {
                Log.w(TAG, "Пустой ответ для громкости устройства " + deviceIndex + ", повторная попытка " + (attempt + 1));
                new Handler().postDelayed(() -> fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index, attempt + 1), 50);
                return;
            }

            try {
                Log.d(TAG, "Получен ответ для громкости устройства " + deviceIndex + ": " + response);
                int volume = parseAudioResponse(response);
                updateVolumeSeekBar(deviceIndex, volume);

                // Получение MUTE
                Log.d(TAG, "Отправка команды для получения MUTE устройства " + deviceIndex + ": GS " + muteId);
                networkManager.sendCommand(ip, port, "GS " + muteId + "\r", muteResponse -> {
                    try {
                        Log.d(TAG, "Получен ответ для MUTE устройства " + deviceIndex + ": " + muteResponse);
                        boolean isMuted = parseMuteResponse(muteResponse);
                        updateMuteButton(deviceIndex, isMuted);

                        // Переходим к следующему устройству
                        fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index + 1, 0);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Ошибка обработки MUTE для устройства " + deviceIndex + ": " + e.getMessage());
                        new Handler().postDelayed(() -> fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index, attempt + 1), 50);
                    }
                });
            } catch (NumberFormatException e) {
                Log.e(TAG, "Ошибка обработки громкости для устройства " + deviceIndex + ": " + e.getMessage());
                new Handler().postDelayed(() -> fetchAudioSettingsSequentially(ip, port, audioSettingsIds, index, attempt + 1), 50);
            }
        });
    }

    private void updateVolumeSeekBar(int deviceIndex, int progress) {
        if (deviceIndex < 0 || deviceIndex >= seekBars.length) {
            Log.e(TAG, "Неверный индекс устройства: " + deviceIndex);
            return;
        }

        SeekBar seekBar = seekBars[deviceIndex];
        if (seekBar != null) {
            if (progress < 0) progress = 0; // Ограничиваем минимальное значение
            if (progress > 100) progress = 100; // Ограничиваем максимальное значение
            seekBar.setProgress(progress);
        } else {
            Log.e(TAG, "SeekBar не найден для устройства " + deviceIndex);
        }
    }

    private void updateMuteButton(int deviceIndex, boolean isMuted) {
        if (deviceIndex < 0 || deviceIndex >= muteButtons.length) {
            Log.e(TAG, "Неверный индекс устройства: " + deviceIndex);
            return;
        }

        TextView muteButton = muteButtons[deviceIndex];
        if (muteButton != null) {
            muteButton.setTextColor(isMuted ? Color.RED : Color.GREEN);
        } else {
            Log.e(TAG, "Кнопка MUTE не найдена для устройства " + deviceIndex);
        }
    }

    private void setupVolumeSeekBars(String audioProcessorIp, int audioProcessorPort) {
        for (int i = 0; i < seekBars.length; i++) {
            final int deviceIndex = i;
            seekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser) return;

                    String command = "CSQ " + getAudioDeviceId(deviceIndex, true) + " " + progress + "\r";
                    Log.d(TAG, "Отправка команды управления громкостью: " + command);

                    networkManager.sendCommand(audioProcessorIp, audioProcessorPort, command,  response -> {
                        if (response != null && !response.equals("Ошибка соединения") && !response.equals("Ошибка: пустой ответ")) {
                            showToast("Громкость устройства " + deviceIndex + " успешно обновлена");
                        } else {
                            showToast("Не удалось обновить громкость устройства " + deviceIndex);
                        }
                    });
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    private void setupMuteButtons(String audioProcessorIp, int audioProcessorPort) {
        for (int i = 0; i < muteButtons.length; i++) {
            final int deviceIndex = i;
            muteButtons[i].setOnClickListener(view -> {
                int muteStatus = isMuteEnabled(deviceIndex) ? 0 : 65535; // Меняем статус MUTE
                String command = "CSQ " + getAudioDeviceId(deviceIndex, false) + " " + muteStatus + "\r";
                Log.d(TAG, "Отправка команды управления MUTE: " + command);

                networkManager.sendCommand(audioProcessorIp, audioProcessorPort, command,  response -> {
                    if (response != null && !response.equals("Ошибка соединения") && !response.equals("Ошибка: пустой ответ")) {
                        updateMuteButton(deviceIndex, muteStatus == 65535);
                        showToast("Состояние MUTE устройства " + deviceIndex + " успешно обновлено");
                    } else {
                        showToast("Не удалось обновить состояние MUTE устройства " + deviceIndex);
                    }
                });
            });
        }
    }

    private boolean isMuteEnabled(int deviceIndex) {
        if (deviceIndex < 0 || deviceIndex >= muteButtons.length) return false;
        TextView muteButton = muteButtons[deviceIndex];
        if (muteButton == null) return false;
        return muteButton.getCurrentTextColor() == Color.RED; // Красный цвет означает, что MUTE включен
    }

    private int getAudioDeviceId(int deviceIndex, boolean isVolume) {
        switch (deviceIndex) {
            case 0: return isVolume ? 9 : 10; // PC
            case 1: return isVolume ? 11 : 12; // VIA
            case 2: return isVolume ? 7 : 8; // Микрофон 1
            case 3: return isVolume ? 5 : 6; // Микрофон 2
            case 4: return isVolume ? 3 : 4; // Микрофон 3
            default: throw new IllegalArgumentException("Invalid device index");
        }
    }

    private void showToast(String message) {
        if (context != null) {
            //Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Context is null. Unable to show toast: " + message);
        }
    }

    private int parseAudioResponse(String response) {
        return Integer.parseInt(response.trim());
    }

    private boolean parseMuteResponse(String response) {
        return Integer.parseInt(response.trim()) == 65535;
    }
}