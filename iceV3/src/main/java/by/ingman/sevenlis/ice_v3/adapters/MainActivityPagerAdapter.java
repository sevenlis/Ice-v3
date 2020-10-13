package by.ingman.sevenlis.ice_v3.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

import by.ingman.sevenlis.ice_v3.activities.fragments.MainActivityPageFragment;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class MainActivityPagerAdapter extends FragmentStatePagerAdapter {
    private final ArrayList<MainActivityPageFragment> fragmentArrayList;
    
    public MainActivityPagerAdapter(FragmentManager fragmentManager, ArrayList<MainActivityPageFragment> fragmentArrayList) {
        super(fragmentManager,BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        this.fragmentArrayList = fragmentArrayList;
    }
    
    @NonNull
    @Override
    public Fragment getItem(int position) {
        return this.fragmentArrayList.get(position);
    }
    
    @Override
    public int getCount() {
        return this.fragmentArrayList.size();
    }
    
    @Override
    public CharSequence getPageTitle(int position) {
        MainActivityPageFragment fragment = (MainActivityPageFragment) getItem(position);
        return FormatsUtils.getDateFormatted(fragment.getOrderDateCal().getTime());
    }
}
