package by.ingman.sevenlis.ice_v3.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Calendar;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.activities.fragments.MainActivityPageFragment;
import by.ingman.sevenlis.ice_v3.adapters.MainActivityPagerAdapter;
import by.ingman.sevenlis.ice_v3.classes.CustomPagerTabStrip;
import by.ingman.sevenlis.ice_v3.intents.ExchangeDataIntents;
import by.ingman.sevenlis.ice_v3.intents.LocationTrackerTaskIntents;
import by.ingman.sevenlis.ice_v3.remote.CheckApkUpdate;
import by.ingman.sevenlis.ice_v3.services.ExchangeDataService;
import by.ingman.sevenlis.ice_v3.services.LocationTrackingService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.NotificationsUtil;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_NEW_ORDER = 0;
    private static ExchangeDataIntents exchangeDataIntents;
    private static LocationTrackerTaskIntents locationTrackerTaskIntents;
    private static CheckApkUpdate chkApkUpdate;
    private static ArrayList<MainActivityPageFragment> fragmentArrayList = new ArrayList<>();
    private Context ctx;
    private NotificationsUtil notifUtils;
    private ViewPager viewPager;
    private CustomPagerTabStrip customPagerTabStrip;
    private MainActivityPageFragment currentFragment;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(ExchangeDataService.CHANNEL_ORDERS_UPDATES)) {
                refreshCurrentFragment();
            }
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.setGroupVisible(R.id.exchangeServiceMenuGroup, false);
        return super.onCreateOptionsMenu(menu);
    }
    
    private MainActivityPageFragment findMainActivityFragment(Calendar orderDateCal) {
        FormatsUtils.roundDayToStart(orderDateCal);
        
        MainActivityPageFragment fragment = currentFragment;
        for (MainActivityPageFragment fr : fragmentArrayList) {
            if (fr.getOrderDateCal().getTimeInMillis() == orderDateCal.getTimeInMillis()) {
                fragment = fr;
                break;
            }
        }
        return fragment;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_order: {
                Calendar newOrderDateCal = currentFragment.getOrderDateCal();
                
                Calendar nowStart = Calendar.getInstance();
                FormatsUtils.roundDayToStart(nowStart);
                
                if (nowStart.getTimeInMillis() > newOrderDateCal.getTimeInMillis()) {
                    Calendar tomorrow = Calendar.getInstance();
                    tomorrow.add(Calendar.DATE, 1);
                    newOrderDateCal.setTime(tomorrow.getTime());
                }
                Intent intent = new Intent(ctx, OrderActivity.class);
                intent.putExtra("longDate", newOrderDateCal.getTimeInMillis());
                startActivityForResult(intent, REQUEST_CODE_NEW_ORDER);
            }
            break;
            case R.id.settings: {
                Intent intent = new Intent(ctx, SettingsActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.update_data: {
                Intent intent = new Intent(ctx, UpdateDataActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.stopExchangeData: {
                stopExchangeDataService();
            }
            break;
            case R.id.startExchangeData: {
                startExchangeDataService();
            }
            break;
            case R.id.refresh_list: {
                refreshCurrentFragment();
            }
            break;
            case R.id.return_today: {
                currentFragment = findMainActivityFragment(Calendar.getInstance());
                viewPager.setCurrentItem(fragmentArrayList.indexOf(currentFragment));
            }
            break;
            case R.id.about_app: {
                Intent intent = new Intent(ctx, AboutActivity.class);
                startActivity(intent);
            }
            break;
            case R.id.show_location: {
                Intent intent = new Intent(ctx, MapsActivity.class);
                startActivity(intent);
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_NEW_ORDER) {
                if (data.getExtras() != null) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(data.getExtras().getLong("orderDateMillis"));
                    FormatsUtils.roundDayToStart(cal);
                    currentFragment = findMainActivityFragment(cal);
                    viewPager.setCurrentItem(fragmentArrayList.indexOf(currentFragment));
                }
                refreshCurrentFragment();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    
    private MainActivityPageFragment getCurrentFragment() {
        if (currentFragment == null) currentFragment = new MainActivityPageFragment();
        return currentFragment;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ctx = this.getApplicationContext();
        notifUtils = new NotificationsUtil(ctx);
        exchangeDataIntents = new ExchangeDataIntents();
        locationTrackerTaskIntents = new LocationTrackerTaskIntents();
        chkApkUpdate = new CheckApkUpdate();
        
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setTitle(R.string.app_name);
            getActionBar().setSubtitle("Журнал заявок");
        }
        
        Calendar now = Calendar.getInstance();
        FormatsUtils.roundDayToStart(now);
        
        currentFragment = getCurrentFragment();
        currentFragment.setOrderDateCal(now);
        
        MainActivity.fragmentArrayList.clear();
        for (int i = -SettingsUtils.Settings.getOrderLogDepth(ctx); i <= SettingsUtils.Settings.getOrderDaysAhead(ctx); i++) {
            Calendar nDate = Calendar.getInstance();
            nDate.add(Calendar.DATE, i);
            FormatsUtils.roundDayToStart(nDate);
            
            MainActivityPageFragment fragment = new MainActivityPageFragment();
            fragment.setOrderDateCal(nDate);
            fragmentArrayList.add(fragment);
            
            if (nDate.getTimeInMillis() == now.getTimeInMillis()) {
                currentFragment = fragment;
            }
        }
        
        if (savedInstanceState != null) {
            long dateMillis = savedInstanceState.getLong("orderDateLong");
            now.setTimeInMillis(dateMillis);
            currentFragment = findMainActivityFragment(now);
        }
        
        MainActivityPagerAdapter mainActivityPagerAdapter = new MainActivityPagerAdapter(getSupportFragmentManager(), fragmentArrayList);
        
        viewPager = (ViewPager) findViewById(R.id.main_pager);
        viewPager.setAdapter(mainActivityPagerAdapter);
        viewPager.setCurrentItem(fragmentArrayList.indexOf(currentFragment));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            
            }
            
            @Override
            public void onPageSelected(int position) {
                currentFragment = fragmentArrayList.get(position);
                if (customPagerTabStrip != null) {
                    customPagerTabStrip.setTextColor(customPagerTabStrip.getDateColor(currentFragment.getOrderDateCal()));
                    customPagerTabStrip.setTabIndicatorColor(customPagerTabStrip.getDateColor(currentFragment.getOrderDateCal()));
                }
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {
                
            }
        });
        
        customPagerTabStrip = (CustomPagerTabStrip) findViewById(R.id.pager_title_strip);
        if (customPagerTabStrip != null) {
            customPagerTabStrip.setTextColor(customPagerTabStrip.getDateColor(currentFragment.getOrderDateCal()));
            customPagerTabStrip.setTabIndicatorColor(customPagerTabStrip.getDateColor(currentFragment.getOrderDateCal()));
        }
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("currentDateMillis", currentFragment.getOrderDateCal().getTimeInMillis());
    }
    
    private void startExchangeDataService() {
        startService(ExchangeDataIntents.getExchangeDataServiceIntent(ctx));
        exchangeDataIntents.startExchangeDataServiceAlarm(ctx);
    }
    
    private void stopExchangeDataService() {
        exchangeDataIntents.stopExchangeDataServiceAlarm(ctx);
    }
    
    private void startLocationTrackerService() {
        Intent intent;
        String locationTrackingType = SettingsUtils.Settings.getLocationTrackingType(ctx);
        switch (locationTrackingType) {
            case SettingsUtils.LOCATION_TRACKING_TYPE_ALWAYS:
                intent = new Intent(ctx, LocationTrackingService.class);
                break;
            case SettingsUtils.LOCATION_TRACKING_TYPE_PERIOD:
                intent = LocationTrackerTaskIntents.getLocationTrackerTaskIntent(ctx);
                locationTrackerTaskIntents.startLocationTrackerServiceTaskAlarm(ctx);
                break;
            default:
                return;
        }
        startService(intent);
    }
    
    private void stopLocationTrackerService() {
        Intent intent;
        String locationTrackingType = SettingsUtils.Settings.getLocationTrackingType(ctx);
        switch (locationTrackingType) {
            case SettingsUtils.LOCATION_TRACKING_TYPE_ALWAYS:
                intent = new Intent(ctx, LocationTrackingService.class);
                break;
            case SettingsUtils.LOCATION_TRACKING_TYPE_PERIOD:
                intent = LocationTrackerTaskIntents.getLocationTrackerTaskIntent(ctx);
                locationTrackerTaskIntents.stopLocationTrackerServiceTaskAlarm(ctx);
                break;
            default:
                return;
        }
        stopService(intent);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    
        registerReceiver(broadcastReceiver, new IntentFilter(ExchangeDataService.CHANNEL_ORDERS_UPDATES));
    
        startService(new Intent(ctx, CheckApkUpdate.class));
    
        stopExchangeDataService();
        startExchangeDataService();
    
        stopLocationTrackerService();
        startLocationTrackerService();
    }
    
    @Override
    protected void onStop() {
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        if (SettingsUtils.Settings.getExchangeShutdownOnExit(ctx)) {
            stopExchangeDataService();
            notifUtils.dismissAllUpdateNotifications();
        }
        if (SettingsUtils.Settings.getLocationTrackingShutdownOnExit(ctx)) {
            stopLocationTrackerService();
        }
        chkApkUpdate.cancelUpdateAvailableNotification();
        super.onDestroy();
    }
    
    private void refreshCurrentFragment() {
        getCurrentFragment().refreshOrdersList();
    }
}