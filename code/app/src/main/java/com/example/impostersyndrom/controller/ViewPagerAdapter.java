package com.example.impostersyndrom.controller;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.impostersyndrom.view.FollowingFragment;
import com.example.impostersyndrom.view.PendingRequestsFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {
    public ViewPagerAdapter(@NonNull androidx.fragment.app.FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) { // ⬅ FIXED to return Fragment
        switch (position) {
            case 0:
                return new PendingRequestsFragment();
            case 1:
                return new FollowingFragment();
            default:
                return new PendingRequestsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;  // Two fragments
    }
}
