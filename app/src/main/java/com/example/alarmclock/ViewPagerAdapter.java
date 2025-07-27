package com.example.alarmclock;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new AlarmFragment();
            case 1:
                return new StopwatchFragment();
            case 2:
                return new TimerFragment();
            default:
                return new AlarmFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}