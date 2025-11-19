package com.example.project;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AssessmentPagerAdapter extends FragmentStateAdapter {

    public AssessmentPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new DepressionFragment();
            case 1:
                return new AnxietyFragment();
            case 2:
                return new StressFragment();
            default:
                return new DepressionFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}