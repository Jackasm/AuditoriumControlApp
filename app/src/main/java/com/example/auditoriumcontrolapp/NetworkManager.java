package com.example.auditoriumcontrolapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NetworkManager {

    private static final String SERVER_IP = "192.168.1.100"; // IP адрес устройства
    private static final int SERVER_PORT = 5000; // Порт устройства

    public interface OnResponseListener {
        void onResponse(String response);
    }

    public void sendCommand(final String command, final OnResponseListener listener) {
        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                // Отправляем команду
                out.println(command);

                // Читаем ответ
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }

                // Передаем ответ обратно в UI поток
                if (listener != null) {
                    runOnUiThread(() -> listener.onResponse(response.toString()));
                }

            } catch (IOException e) {
                e.printStackTrace();
                if (listener != null) {
                    runOnUiThread(() -> listener.onResponse("Ошибка соединения"));
                }
            }
        }).start();
    }

    private void runOnUiThread(Runnable runnable) {
        ControlActivity activity = (ControlActivity) ContextHolder.getContext();
        if (activity != null) {
            activity.runOnUiThread(runnable);
        }
    }
}