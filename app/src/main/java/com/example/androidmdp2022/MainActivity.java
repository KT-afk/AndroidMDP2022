package com.example.androidmdp2022;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import android.hardware.Camera;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class MainActivity extends AppCompatActivity {
    private String[] TAB_TITLE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TabLayout tabLayout = findViewById(R.id.tabs);

        ViewPager2 viewPager2 = findViewById(R.id.view_pager);
        //help to preload and keep the other fragment
        viewPager2.setOffscreenPageLimit(2);
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager2.setAdapter(adapter);
        viewPager2.setUserInputEnabled(false);

        TAB_TITLE = ViewPagerAdapter.getTabTitles();
        tabLayout.setSelectedTabIndicator(R.color.black);

        new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(TAB_TITLE[position]);
            }
        }).attach();

    }
}

