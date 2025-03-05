package com.example.auditoriumcontrolapp;

import android.app.Application;

public class ContextHolder extends Application {

    private static ContextHolder instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static ContextHolder getInstance() {
        return instance;
    }

    public static android.content.Context getContext() {
        return instance.getApplicationContext();
    }
}