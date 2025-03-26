package com.example.impostersyndrom.controller;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.impostersyndrom.view.FollowingMoodsFragment;
import com.example.impostersyndrom.view.MainActivity;
import com.example.impostersyndrom.view.MyMoodsFragment;

public class MainViewPagerAdapter extends FragmentStateAdapter {
    public MainViewPagerAdapter(@NonNull MainActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new MyMoodsFragment();
            case 1:
                return new FollowingMoodsFragment();
            default:
                return new MyMoodsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2; // Two tabs: My Moods and Following
    }
}