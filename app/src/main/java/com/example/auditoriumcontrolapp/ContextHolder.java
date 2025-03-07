package com.example.auditoriumcontrolapp;

import android.app.Application;
import android.content.Context;

public class ContextHolder extends Application {

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this; // Инициализируем контекст
    }

    public static Context getContext() {
        return context; // Возвращаем глобальный контекст
    }
}