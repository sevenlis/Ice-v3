package by.ingman.sevenlis.ice_v3;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.Date;

import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

class MainActivityPagerAdapter extends FragmentStatePagerAdapter {
    private Context ctx;
    private ArrayList<Date> dateArrayList;
    
    MainActivityPagerAdapter(Context context, FragmentManager fragmentManager, ArrayList<Date> dateArrayList) {
        super(fragmentManager);
        this.ctx = context;
        this.dateArrayList = dateArrayList;
    }
    
    @Override
    public Fragment getItem(int position) {
        return MainActivityPageFragment.newInstance(ctx, dateArrayList.get(position));
        /*Fragment fragment = MainActivityPageFragment.newInstance(ctx, dateArrayList.get(position));
        Bundle args = new Bundle();
        args.putLong("longDate", dateArrayList.get(position).getTime());
        fragment.setArguments(args);
        return fragment;*/
    }
    
    @Override
    public int getCount() {
        return dateArrayList.size();
    }
    
    @Override
    public CharSequence getPageTitle(int position) {
        return FormatsUtils.getDateFormatted(dateArrayList.get(position));
    }
}
