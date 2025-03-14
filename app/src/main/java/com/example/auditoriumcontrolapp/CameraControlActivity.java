package com.example.auditoriumcontrolapp;



import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.net.Uri;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class CameraControlActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraControlActivity"; // Тег для логгирования
    private String cameraIp; // IP-адрес камеры
    private int cameraPort; // Порт камеры
    private NetworkManager networkManager; // Менеджер для отправки команд
    private TextureView textureView; // TextureView для отображения видео
    private LibVLC libVLC; // Объект LibVLC
    private MediaPlayer mediaPlayer; // Объект MediaPlayer
    private static final byte[] STOP = { (byte) 0x81, 0x01, 0x06, 0x01, 0x00, 0x00, 0x03, 0x03, (byte) 0xFF };
    private static final byte[] UP = { (byte) 0x81, 0x01, 0x06, 0x01, 0x05, 0x05, 0x03, 0x01, (byte) 0xFF };
    private static final byte[] DOWN = { (byte) 0x81, 0x01, 0x06, 0x01, 0x05, 0x05, 0x03, 0x02, (byte) 0xFF };
    private static final byte[] LEFT = { (byte) 0x81, 0x01, 0x06, 0x01, 0x05, 0x05, 0x01, 0x03, (byte) 0xFF };
    private static final byte[] RIGHT = { (byte) 0x81, 0x01, 0x06, 0x01, 0x05, 0x05, 0x02, 0x03, (byte) 0xFF };
    private static final byte[] ZOOM_IN = { (byte) 0x81, 0x01, 0x04, 0x07, 0x02, (byte) 0xFF };
    private static final byte[] ZOOM_OUT = { (byte) 0x81, 0x01, 0x04, 0x07, 0x03, (byte) 0xFF };
    private static final byte[] ZOOM_STOP = { (byte) 0x81, 0x01, 0x04, 0x07, 0x00, (byte) 0xFF };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_control);
        cameraPort = getIntent().getIntExtra("camera_port", 4567);
        // Получаем IP-адрес камеры из Intent
        cameraIp = getIntent().getStringExtra("camera_ip");
        if (cameraIp == null || cameraIp.isEmpty()) {
            Toast.makeText(this, "IP-адрес камеры не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // Инициализируем NetworkManager
        networkManager = new NetworkManager();
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
        options.add("--network-caching=0"); // Увеличиваем кэширование сети

        //options.add("--video-filter=transform"); // Включаем фильтр трансформации
        //options.add("--transform-type=90:hflip:vflip"); // Поворачиваем видео на 90 градусов
        options.add("--video-filter=rotate"); // Включаем фильтр поворота
        options.add("--rotate-angle=90"); // Поворачиваем видео на 90 градусов
        //options.add("--no-autoscale");
        //options.add("--transform-type=hflip:vflip");




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

        setupButtons();
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
        //mediaPlayer.setAspectRatio("");
        mediaPlayer.setScale(1.8f); // Увеличить картинку в 2 раза
        try {
            mediaPlayer.play();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка воспроизведения: " + e.getMessage());
            Toast.makeText(this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
        }
        // Применяем сдвиг картинки
        Matrix matrix = new Matrix();
        int videoWidth = 1920;
        int videoHeight = 1080;
        Log.d(TAG, "Размеры видео: " + videoWidth + "x" + videoHeight);
        float scaleX = (float) textureView.getWidth() / videoWidth;
        float scaleY = (float) textureView.getHeight() / videoHeight;
        //matrix.setScale(scaleY , scaleX * 2);
        //matrix.postRotate(90); // Поворачиваем видео на 90 градусов
        textureView.setTransform(matrix);
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
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "Активность приостановлена");

        // Останавливаем воспроизведение и освобождаем ресурсы
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "Активность остановлена");

        // Останавливаем воспроизведение и освобождаем ресурсы
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
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
    private void setupButtons() {
        // Кнопка "Вверх"
        findViewById(R.id.button_up).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand(UP); // Отправляем команду "Вверх"
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand(STOP); // Отправляем команду "Стоп"
            }
            return true;
        });

        // Кнопка "Вниз"
        findViewById(R.id.button_down).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand(DOWN); // Отправляем команду "Вниз"
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand(STOP); // Отправляем команду "Стоп"
            }
            return true;
        });

        // Кнопка "Влево"
        findViewById(R.id.button_left).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand(LEFT); // Отправляем команду "Влево"
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand(STOP); // Отправляем команду "Стоп"
            }
            return true;
        });

        // Кнопка "Вправо"
        findViewById(R.id.button_right).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand(RIGHT); // Отправляем команду "Вправо"
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand(STOP); // Отправляем команду "Стоп"
            }
            return true;
        });

        // Кнопка "Приблизить"
        findViewById(R.id.button_zoom_in).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand(ZOOM_IN); // Отправляем команду "Приблизить"
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand(ZOOM_STOP); // Отправляем команду "Стоп"
            }
            return true;
        });

        // Кнопка "Отдалить"
        findViewById(R.id.button_zoom_out).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand(ZOOM_OUT); // Отправляем команду "Отдалить"
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand(ZOOM_STOP); // Отправляем команду "Стоп"
            }
            return true;
        });
    }
    private void sendCommand(byte[] command) {
        String commandString = new String(command, StandardCharsets.UTF_8);
        networkManager.sendCommand(cameraIp, cameraPort, commandString, response -> {
            Log.d(TAG, "Ответ от камеры: " + response);
        });
    }
}