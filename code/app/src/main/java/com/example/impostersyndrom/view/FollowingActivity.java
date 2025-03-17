package com.example.impostersyndrom.view;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.impostersyndrom.R;
import com.example.impostersyndrom.controller.ViewPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class FollowingActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_following);

        auth = FirebaseAuth.getInstance();

        // Initialize TabLayout and ViewPager
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ViewPager2 viewPager = findViewById(R.id.viewPager);

        // Set up adapter for ViewPager2
        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        // Attach TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Pending Requests" : "Following");
        }).attach();

        // Set up bottom navigation
        setupBottomNavigation();
    }

    /**
     * Handles bottom navigation button actions.
     */
    private void setupBottomNavigation() {
        ImageButton homeButton = findViewById(R.id.homeButton);
        ImageButton searchButton = findViewById(R.id.searchButton);
        ImageButton addMoodButton = findViewById(R.id.addMoodButton);
        ImageButton heartButton = findViewById(R.id.heartButton);
        ImageButton profileButton = findViewById(R.id.profileButton);

        homeButton.setOnClickListener(v -> {
            startActivity(new Intent(FollowingActivity.this, MainActivity.class));
            finish();
        });

        searchButton.setOnClickListener(v -> {
            startActivity(new Intent(FollowingActivity.this, SearchActivity.class));
            finish();
        });

        addMoodButton.setOnClickListener(v -> {
            startActivity(new Intent(FollowingActivity.this, EmojiSelectionActivity.class));
        });

        heartButton.setOnClickListener(v -> {
            // Stay on the same page (FollowingActivity)
        });

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(FollowingActivity.this, ProfileActivity.class));
        });


    }
}
