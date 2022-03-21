package com.example.androidmdp2022;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import org.jetbrains.annotations.NotNull;

public class ViewPagerAdapter extends FragmentStateAdapter {

    // TODO: original tab
    private static final String[] TAB_TITLES = new String[]{ "Arena", "Bluetooth"};

    public ViewPagerAdapter(@NonNull @NotNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NotNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment = null;

        switch (position)
        {
            case 0:
                fragment = MapFragment.newInstance("", "");
                break;
            case 1:
                fragment = BluetoothFragment.newInstance("", "");
                break;
        }
        return fragment;
    }

    public static String[] getTabTitles() {
        return TAB_TITLES;
    }

    @Override
    public int getItemCount() {
        return TAB_TITLES.length;
    }
}
