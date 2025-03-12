package com.example.auditoriumcontrolapp;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkManager {

    private final Handler mainHandler;
    private static final String TAG = "NetworkManager"; // Тег для логгирования

    public NetworkManager() {
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface OnResponseListener {
        void onResponse(String response);
    }

    public void sendCommand(String ipAddress, int port, String command, OnResponseListener listener) {
        // Логгируем отправленную команду
        Log.d(TAG, "Отправка команды: " + command + " на " + ipAddress + ":" + port);

        new Thread(() -> {
            try (Socket socket = new Socket(ipAddress, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Отправляем команду
                out.println(command);

                // Читаем ответ
                String response = in.readLine(); // Читаем одну строку
                if (response == null) {
                    response = ""; // Если ответ пустой, устанавливаем пустую строку
                }
                response = response.trim(); // Убираем лишние пробелы
                Log.d(TAG, "Получен ответ: " + response + " от " + ipAddress + ":" + port);
                String trimmedResponse = response;
// Передаем ответ в слушатель
                if (listener != null) {
                    mainHandler.post(() -> listener.onResponse(trimmedResponse));
                }

            } catch (IOException e) {
                e.printStackTrace();
                // Логгируем ошибку соединения
                Log.e(TAG, "Ошибка соединения: " + e.getMessage());

                // Если произошла ошибка соединения, передаем сообщение об ошибке
                if (listener != null) {
                    mainHandler.post(() -> listener.onResponse("Ошибка соединения: " + e.getMessage()));
                }
            }
        }).start();
    }
}