package com.example.androidmdp2022;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

//import com.example.androidmdp2022.BluetoothFragment;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    // TODO: to be deleted after building the Arena
    //private static final String[] TAB_TITLES = new String[]{ "Arena"};

    // TODO: original tab
    private static final String[] TAB_TITLES = new String[]{ "Arena", "Bluetooth"};
    private final Context mContext;

    public SectionsPagerAdapter(Context context, FragmentManager fm) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        // Return a PlaceholderFragment (defined as a static inner class below).
        //return PlaceholderFragment.newInstance(position + 1);

        Fragment fragment = null;

        switch (position)
        {
            case 0:
                fragment = ArenaFragment.newInstance("", "");
                break;
//            case 1:
//                fragment = BluetoothFragment.newInstance("", "");
//                break;

        }
        return fragment;

    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        //return mContext.getResources().getString(TAB_TITLES[position]);
        String tabTitle = "";

        switch (position)
        {
            case 0:
                tabTitle = TAB_TITLES[position];
                break;
//            case 1:
//                tabTitle = TAB_TITLES[position];
//                break;
        }
        return tabTitle;
    }

    @Override
    public int getCount() {
        // Show total pages.
        return TAB_TITLES.length;
    }
}