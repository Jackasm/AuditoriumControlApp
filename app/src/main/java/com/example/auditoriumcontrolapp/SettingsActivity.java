package com.example.auditoriumcontrolapp;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.Arrays;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setTitle("Настройки");

        ViewPager2 viewPager = findViewById(R.id.view_pager);

        // Список названий аудиторий
        List<String> auditoriumNames = Arrays.asList(
                "Аудитория 308",
                "Аудитория 203",
                "Аудитория 206",
                "Аудитория 503"
        );

        // Настройка адаптера
        AuditoriumPagerAdapter adapter = new AuditoriumPagerAdapter(this, auditoriumNames);
        viewPager.setAdapter(adapter);

        // Кнопка сохранения
        View buttonSaveSettings = findViewById(R.id.button_save_settings);
        buttonSaveSettings.setOnClickListener(v -> saveAllSettings());
    }

    private void saveAllSettings() {
        ViewPager2 viewPager = findViewById(R.id.view_pager);
        for (int i = 0; i < viewPager.getChildCount(); i++) {
            AuditoriumSettingsFragment fragment = (AuditoriumSettingsFragment) getSupportFragmentManager()
                    .findFragmentByTag("f" + i);
            if (fragment != null) {
                fragment.saveSettings();
            }
        }
        finish(); // Закрываем экран настроек
    }
}