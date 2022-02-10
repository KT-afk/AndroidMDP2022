package com.example.androidmdp2022;

import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class ViewPagerAdapter extends FragmentStateAdapter {

    // TODO: to be deleted after building the Arena
    //private static final String[] TAB_TITLES = new String[]{ "Arena"};

    // TODO: original tab
    private static final String[] TAB_TITLES = new String[]{ "Arena", "Bluetooth"};

    public ViewPagerAdapter(@NonNull @NotNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    //private final Context mContext;


    @NotNull
    @Override
    public Fragment createFragment(int position) {
        //return null;
        Fragment fragment = null;

        switch (position)
        {
            case 0:
                fragment = ArenaFragment.newInstance("", "");
                //return new ArenaFragment();
                break;
            case 1:
                fragment = BluetoothFragment.newInstance("", "");
                //return new BluetoothFragment();
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
