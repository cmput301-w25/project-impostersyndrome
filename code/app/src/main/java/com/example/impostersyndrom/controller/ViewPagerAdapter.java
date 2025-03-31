package com.example.impostersyndrom.controller;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.impostersyndrom.view.FollowingFragment;
import com.example.impostersyndrom.view.PendingRequestsFragment;

/**
 * Adapter for managing fragments in a ViewPager2, displaying pending requests and following lists.
 *
 * @author [Your Name]
 */
public class ViewPagerAdapter extends FragmentStateAdapter {

    /**
     * Constructs a new ViewPagerAdapter.
     *
     * @param fragmentActivity The FragmentActivity hosting the ViewPager2
     */
    public ViewPagerAdapter(@NonNull androidx.fragment.app.FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    /**
     * Creates a fragment for the given position.
     *
     * @param position The position within the adapter
     * @return A Fragment instance (PendingRequestsFragment or FollowingFragment)
     */
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new PendingRequestsFragment();
            case 1:
                return new FollowingFragment();
            default:
                return new PendingRequestsFragment();
        }
    }

    /**
     * Returns the total number of fragments managed by this adapter.
     *
     * @return The number of items (2: Pending Requests and Following)
     */
    @Override
    public int getItemCount() {
        return 2;  // Two fragments
    }
}