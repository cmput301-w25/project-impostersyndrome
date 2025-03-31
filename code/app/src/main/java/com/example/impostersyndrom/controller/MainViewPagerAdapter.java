package com.example.impostersyndrom.controller;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.impostersyndrom.view.FollowingMoodsFragment;
import com.example.impostersyndrom.view.MainActivity;
import com.example.impostersyndrom.view.MyMoodsFragment;

/**
 * Adapter for managing fragments in a ViewPager2 within the MainActivity.
 *
 * @author Roshan
 *
 */
public class MainViewPagerAdapter extends FragmentStateAdapter {

    /**
     * Constructs a new MainViewPagerAdapter.
     *
     * @param activity The MainActivity hosting the ViewPager2
     */
    public MainViewPagerAdapter(@NonNull MainActivity activity) {
        super(activity);
    }

    /**
     * Creates a fragment for the given position.
     *
     * @param position The position within the adapter
     * @return A Fragment instance corresponding to the position (MyMoodsFragment or FollowingMoodsFragment)
     */
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

    /**
     * Returns the total number of fragments (tabs) managed by this adapter.
     *
     * @return The number of items (2: My Moods and Following)
     */
    @Override
    public int getItemCount() {
        return 2; // Two tabs: My Moods and Following
    }
}