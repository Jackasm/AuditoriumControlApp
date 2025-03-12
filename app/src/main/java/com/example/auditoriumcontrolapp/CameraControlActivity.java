package com.example.auditoriumcontrolapp;

import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import java.util.ArrayList;

public class CameraControlActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraControlActivity"; // Тег для логгирования
    private String cameraIp; // IP-адрес камеры
    private TextureView textureView; // TextureView для отображения видео
    private LibVLC libVLC; // Объект LibVLC
    private MediaPlayer mediaPlayer; // Объект MediaPlayer

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_control);

        // Получаем IP-адрес камеры из Intent
        cameraIp = getIntent().getStringExtra("camera_ip");
        if (cameraIp == null || cameraIp.isEmpty()) {
            Toast.makeText(this, "IP-адрес камеры не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Находим TextureView
        textureView = findViewById(R.id.video_texture_view);
        textureView.setSurfaceTextureListener(this); // Устанавливаем слушатель

        // Инициализация LibVLC с дополнительными параметрами
        ArrayList<String> options = new ArrayList<>();
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        options.add("--verbose"); // Включаем логирование
        options.add("--subsdec-encoding=UTF-8"); // Указываем кодировку субтитров
        options.add("--freetype-font=/system/fonts/DroidSans.ttf"); // Указываем другой шрифт
        options.add("--audio-time-stretch"); // Включаем растяжение аудио
        options.add("--network-caching=300"); // Увеличиваем кэширование сети
        options.add("--video-filter=transform"); // Включаем фильтр трансформации
        options.add("--transform-type=90"); // Поворачиваем видео на 90 градусов
        libVLC = new LibVLC(this, options);



        // Инициализация MediaPlayer
        mediaPlayer = new MediaPlayer(libVLC);

        // Устанавливаем слушатель событий MediaPlayer
        mediaPlayer.setEventListener(event -> {
            Log.d(TAG, "Событие MediaPlayer: " + event.type);
            if (event.type == MediaPlayer.Event.Opening) {
                Log.d(TAG, "Поток открывается");
            } else if (event.type == MediaPlayer.Event.Playing) {
                Log.d(TAG, "Поток воспроизводится");
            } else if (event.type == MediaPlayer.Event.EncounteredError) {
                Log.e(TAG, "Ошибка воспроизведения");
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "SurfaceTexture доступен: " + width + "x" + height);

        // Привязываем SurfaceTexture к MediaPlayer
        mediaPlayer.getVLCVout().setVideoSurface(new Surface(surface), null);
        mediaPlayer.getVLCVout().attachViews();

        // Начинаем воспроизведение
        String rtspUrl = "rtsp://" + cameraIp + ":554"; // Используем переданный IP-адрес и порт 554
        Media media = new Media(libVLC, Uri.parse(rtspUrl));
        media.setHWDecoderEnabled(false, false); // Отключаем аппаратное декодирование
        mediaPlayer.setMedia(media);
        media.release(); // Освобождаем ресурсы Media

        try {
            mediaPlayer.play();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка воспроизведения: " + e.getMessage());
            Toast.makeText(this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "Размер поверхности изменен: " + width + "x" + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "SurfaceTexture уничтожен");

        // Освобождаем ресурсы
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (libVLC != null) {
            libVLC.release();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Обработка обновления поверхности
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Активность уничтожена");

        // Освобождаем ресурсы
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (libVLC != null) {
            libVLC.release();
        }
    }
}