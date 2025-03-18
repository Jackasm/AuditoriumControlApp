package com.example.auditoriumcontrolapp;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class AuditoriumPagerAdapter extends FragmentStateAdapter {

    private final List<String> auditoriumNames;

    public AuditoriumPagerAdapter(FragmentActivity fa, List<String> auditoriumNames) {
        super(fa);
        this.auditoriumNames = auditoriumNames;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return AuditoriumSettingsFragment.newInstance(auditoriumNames.get(position));
    }

    @Override
    public int getItemCount() {
        return auditoriumNames.size();
    }
}