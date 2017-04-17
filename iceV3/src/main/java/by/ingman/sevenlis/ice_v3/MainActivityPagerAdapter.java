package by.ingman.sevenlis.ice_v3;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Date;

import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

class MainActivityPagerAdapter extends FragmentStatePagerAdapter {
    private ArrayList<MainActivityPageFragment> fragmentArrayList;
    private ArrayList<Date> dateArrayList;
    
    MainActivityPagerAdapter(FragmentManager fragmentManager, ArrayList<MainActivityPageFragment> fragmentArrayList, ArrayList<Date> dateArrayList) {
        super(fragmentManager);
        this.fragmentArrayList = fragmentArrayList;
        this.dateArrayList = dateArrayList;
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
        return FormatsUtils.getDateFormatted(this.dateArrayList.get(position));
    }
}
