package by.ingman.sevenlis.ice_v3.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;

import by.ingman.sevenlis.ice_v3.activities.MainActivityPageFragment;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class MainActivityPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<MainActivityPageFragment> fragmentArrayList;
    
    public MainActivityPagerAdapter(FragmentManager fragmentManager, ArrayList<MainActivityPageFragment> fragmentArrayList) {
        super(fragmentManager);
        this.fragmentArrayList = fragmentArrayList;
    }
    
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
