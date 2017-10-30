package by.ingman.sevenlis.ice_v3;

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

import by.ingman.sevenlis.ice_v3.classes.CustomPagerTabStrip;
import by.ingman.sevenlis.ice_v3.classes.ExchangeDataIntents;
import by.ingman.sevenlis.ice_v3.remote.sql.CheckApkUpdate;
import by.ingman.sevenlis.ice_v3.remote.sql.ExchangeDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.NotificationsUtil;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class MainActivity extends AppCompatActivity {
    private Context ctx;
    
    private static final int REQUEST_CODE_NEW_ORDER = 0;
    
    private static ExchangeDataIntents exchangeDataIntents;
    private static CheckApkUpdate chkApkUpdate;
    
    private NotificationsUtil notifUtils;
    private ViewPager viewPager;
    private CustomPagerTabStrip customPagerTabStrip;
    private MainActivityPageFragment currentFragment;
    private static ArrayList<MainActivityPageFragment> fragmentArrayList = new ArrayList<>();
    
    
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
            } break;
            case R.id.settings: {
                Intent intent = new Intent(ctx, SettingsActivity.class);
                startActivity(intent);
            } break;
            case R.id.update_data: {
                Intent intent = new Intent(ctx, UpdateDataActivity.class);
                startActivity(intent);
            } break;
            case R.id.stopExchangeData: {
                stopExchangeDataService();
            } break;
            case R.id.startExchangeData: {
                startExchangeDataService();
            }  break;
            case R.id.refresh_list: {
                refreshCurrentFragment();
            } break;
            case R.id.return_today: {
                currentFragment = findMainActivityFragment(Calendar.getInstance());
                viewPager.setCurrentItem(fragmentArrayList.indexOf(currentFragment));
            } break;
            case R.id.about_app: {
                Intent intent = new Intent(ctx, AboutActivity.class);
                startActivity(intent);
            } break;
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
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        ctx = getApplicationContext();
        notifUtils = new NotificationsUtil(ctx);
        exchangeDataIntents = new ExchangeDataIntents();
        chkApkUpdate = new CheckApkUpdate();
        
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setDisplayShowTitleEnabled(true);
            getActionBar().setTitle(R.string.app_name);
            getActionBar().setSubtitle("Журнал заявок");
        }
    
        Calendar now = Calendar.getInstance();
        FormatsUtils.roundDayToStart(now);
        
        currentFragment = new MainActivityPageFragment();
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
    
        registerReceiver(broadcastReceiver, new IntentFilter(ExchangeDataService.CHANNEL_ORDERS_UPDATES));
        
        startService(new Intent(ctx, CheckApkUpdate.class));
        
        startExchangeDataService();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("currentDateMillis",currentFragment.getOrderDateCal().getTimeInMillis());
    }
    
    private void startExchangeDataService() {
        startService(ExchangeDataIntents.getExchangeDataServiceIntent(ctx));
        exchangeDataIntents.startExchangeDataServiceAlarm(ctx);
    }
    
    private void stopExchangeDataService() {
        exchangeDataIntents.stopExchangeDataServiceAlarm(ctx);
    }
    
    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        if (SettingsUtils.Settings.getExchangeShutdownOnExit(ctx)) {
            stopExchangeDataService();
            notifUtils.dismissAllUpdateNotifications();
        }
        chkApkUpdate.cancelUpdateAvailableNotification();
        super.onDestroy();
    }
    
    private void refreshCurrentFragment() {
        if (currentFragment == null) return;
        currentFragment.refreshOrdersList(true);
    }
    
}