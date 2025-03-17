package com.example.auditoriumcontrolapp;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

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
                /*try {
                    Thread.sleep(5); // Задержка 5 миллисекунд
                } catch (InterruptedException e) {
                    Log.e(TAG, "Ошибка при задержке: " + e.getMessage());
                } */
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
    public void sendCommandBytes(String ipAddress, int port, byte[] command, OnResponseListener listener) {
        Log.d(TAG, "Отправка команды (байты): " + bytesToHex(command) + " на " + ipAddress + ":" + port);

        new Thread(() -> {
            try (Socket socket = new Socket(ipAddress, port);
                 OutputStream out = socket.getOutputStream();
                 DataInputStream in = new DataInputStream(socket.getInputStream())) {

                // Устанавливаем таймаут на чтение
                socket.setSoTimeout(5000); // Таймаут 5 секунд

                // Отправляем команду как набор байт
                out.write(command); // Отправляем байты
                out.flush(); // Убеждаемся, что данные отправлены



                // Читаем ответ
                byte[] buffer = new byte[1024]; // Буфер на 1024 байта
                int bytesRead = in.read(buffer); // Читаем данные
                String response = new String(buffer, 0, bytesRead); // Преобразуем в строку
                Log.d(TAG, "Ответ получен: " + response);

                // Передаем ответ в слушатель
                if (listener != null) {
                    mainHandler.post(() -> listener.onResponse(response));
                }

            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Таймаут при чтении ответа: " + e.getMessage());
                if (listener != null) {
                    mainHandler.post(() -> listener.onResponse("Таймаут при чтении ответа"));
                }
            } catch (IOException e) {
                Log.e(TAG, "Ошибка соединения: " + e.getMessage());
                if (listener != null) {
                    mainHandler.post(() -> listener.onResponse("Ошибка соединения: " + e.getMessage()));
                }
            }
        }).start();
    }
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}