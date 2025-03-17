package com.example.auditoriumcontrolapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {

    private Typeface typeface; // Шрифт
    private int textColor; // Цвет текста

    public CustomSpinnerAdapter(@NonNull Context context, int resource, @NonNull String[] objects, Typeface typeface, int textColor) {
        super(context, resource, objects);
        this.typeface = typeface;
        this.textColor = textColor;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        customizeTextView((TextView) view);
        return view;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        customizeTextView((TextView) view);
        return view;
    }

    private void customizeTextView(TextView textView) {
        textView.setTypeface(typeface); // Устанавливаем шрифт
        textView.setTextColor(textColor); // Устанавливаем цвет текста
        textView.setTextSize(28); // Размер текста (опционально)
    }
}