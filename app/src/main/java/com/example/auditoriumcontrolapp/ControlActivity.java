package com.example.auditoriumcontrolapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ControlActivity extends AppCompatActivity {

    private NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        networkManager = new NetworkManager();

        // Видеопроцессор
        Spinner spinnerVideoInput = findViewById(R.id.spinner_video_input);
        Spinner spinnerVideoOutput = findViewById(R.id.spinner_video_output);

        spinnerVideoInput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                String command = "SET_VIDEO_INPUT:" + selectedItem;
                sendCommand(command, response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Обработайте случай, когда ничего не выбрано (если необходимо)
            }
        });

        spinnerVideoOutput.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = parent.getItemAtPosition(position).toString();
                String command = "SET_VIDEO_OUTPUT:" + selectedItem;
                sendCommand(command, response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Обработайте случай, когда ничего не выбрано (если необходимо)
            }
        });

        // Громкость и mute
        setupSoundControl(findViewById(R.id.sound_control_device_1), "DEVICE_1");
        setupSoundControl(findViewById(R.id.sound_control_device_2), "DEVICE_2");
        setupSoundControl(findViewById(R.id.sound_control_device_3), "DEVICE_3");
        setupSoundControl(findViewById(R.id.sound_control_device_4), "DEVICE_4");
        setupSoundControl(findViewById(R.id.sound_control_device_5), "DEVICE_5");

        // Управление камерами
        RadioGroup radioGroupCameras = findViewById(R.id.radio_group_cameras);
        Button buttonCameraUp = findViewById(R.id.button_camera_up);
        Button buttonCameraDown = findViewById(R.id.button_camera_down);
        Button buttonCameraLeft = findViewById(R.id.button_camera_left);
        Button buttonCameraRight = findViewById(R.id.button_camera_right);
        Button buttonCameraZoomIn = findViewById(R.id.button_camera_zoom_in);
        Button buttonCameraZoomOut = findViewById(R.id.button_camera_zoom_out);

        radioGroupCameras.setOnCheckedChangeListener((group, checkedId) -> {
            String camera = checkedId == R.id.radio_camera_1 ? "CAMERA_1" : "CAMERA_2";
            sendCommand("SELECT_CAMERA:" + camera, response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show());
        });

        buttonCameraUp.setOnClickListener(v -> sendCommand("CAMERA_MOVE:UP", response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show()));
        buttonCameraDown.setOnClickListener(v -> sendCommand("CAMERA_MOVE:DOWN", response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show()));
        buttonCameraLeft.setOnClickListener(v -> sendCommand("CAMERA_MOVE:LEFT", response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show()));
        buttonCameraRight.setOnClickListener(v -> sendCommand("CAMERA_MOVE:RIGHT", response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show()));
        buttonCameraZoomIn.setOnClickListener(v -> sendCommand("CAMERA_ZOOM:IN", response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show()));
        buttonCameraZoomOut.setOnClickListener(v -> sendCommand("CAMERA_ZOOM:OUT", response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show()));
    }

    private void setupSoundControl(View soundControlItemView, String deviceName) {
        SeekBar seekBarVolume = soundControlItemView.findViewById(R.id.seekBar_volume);
        Button buttonMute = soundControlItemView.findViewById(R.id.button_mute);

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    String command = "SET_VOLUME_" + deviceName + ":" + progress;
                    sendCommand(command, response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        buttonMute.setOnClickListener(v -> {
            String command = "MUTE_" + deviceName + ":ON";
            sendCommand(command, response -> Toast.makeText(ControlActivity.this, response, Toast.LENGTH_SHORT).show());
        });
    }

    private void sendCommand(String command, NetworkManager.OnResponseListener listener) {
        networkManager.sendCommand(command, listener);
    }
}