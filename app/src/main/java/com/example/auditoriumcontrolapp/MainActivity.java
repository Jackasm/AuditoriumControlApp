package com.example.auditoriumcontrolapp;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonAuditorium1 = findViewById(R.id.button_auditorium_1);
        Button buttonAuditorium2 = findViewById(R.id.button_auditorium_2);
        Button buttonAuditorium3 = findViewById(R.id.button_auditorium_3);
        Button buttonAuditorium4 = findViewById(R.id.button_auditorium_4);

        buttonAuditorium1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControlActivity("Аудитория 1");
            }
        });

        buttonAuditorium2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControlActivity("Аудитория 2");
            }
        });

        buttonAuditorium3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControlActivity("Аудитория 3");
            }
        });

        buttonAuditorium4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startControlActivity("Аудитория 4");
            }
        });
    }

    private void startControlActivity(String auditoriumName) {
        Intent intent = new Intent(this, ControlActivity.class);
        intent.putExtra("auditorium_name", auditoriumName);
        startActivity(intent);
    }
}