package com.example.auditoriumcontrolapp;

import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkManager {

    // Интерфейс для обратного вызова при получении ответа
    public interface OnResponseListener {
        void onResponse(String response);
    }

    /**
     * Отправляет команду на устройство по указанному IP-адресу и порту.
     *
     * @param ipAddress IP-адрес устройства
     * @param port      Порт устройства
     * @param command   Команда для отправки
     * @param listener  Обратный вызов для получения ответа
     */
    public void sendCommand(String ipAddress, int port, String command, OnResponseListener listener) {
        new Thread(() -> {
            try (Socket socket = new Socket(ipAddress, port);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Отправляем команду
                out.println(command);

                // Читаем ответ
                String responseLine = in.readLine(); // Читаем только первую строку
                if (responseLine == null) {
                    responseLine = "Ошибка: пустой ответ";
                }
                String finalResponse = responseLine;

                // Передаем ответ обратно в UI поток
                if (listener != null) {
                    runOnUiThread(() -> listener.onResponse(finalResponse));
                }

            } catch (IOException e) {
                e.printStackTrace();
                // Если произошла ошибка соединения, передаем сообщение об ошибке
                if (listener != null) {
                    runOnUiThread(() -> listener.onResponse("Ошибка соединения"));
                }
            }
        }).start();
    }

    /**
     * Выполняет код в UI-потоке.
     *
     * @param runnable Код для выполнения
     */
    private void runOnUiThread(Runnable runnable) {
        if (ContextHolder.getContext() == null) {
            // Если контекст отсутствует, выводим ошибку
            System.err.println("Context is null");
            return;
        }

        if (ContextHolder.getContext() instanceof AppCompatActivity) {
            ((AppCompatActivity) ContextHolder.getContext()).runOnUiThread(runnable);
        } else {
            // Если контекст не является AppCompatActivity, используйте Application контекст
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }
}