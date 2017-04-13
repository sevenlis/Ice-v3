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
import java.util.Date;

import by.ingman.sevenlis.ice_v3.classes.CustomPagerTabStrip;
import by.ingman.sevenlis.ice_v3.classes.ExchangeDataIntents;
import by.ingman.sevenlis.ice_v3.remote.sql.CheckApkUpdate;
import by.ingman.sevenlis.ice_v3.remote.sql.ExchangeDataService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.NotificationsUtil;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class MainActivity extends AppCompatActivity {
    private Context ctx;
    
    private final int REQUEST_CODE_NEW_ORDER = 0;
    
    private static ExchangeDataIntents exchangeDataIntents;
    private static CheckApkUpdate chkApkUpdate;
    
    NotificationsUtil notifUtils;
    ArrayList<Date> dateArrayList = new ArrayList<>();
    ViewPager viewPager;
    CustomPagerTabStrip customPagerTabStrip;
    Calendar mainOrderDateCal = Calendar.getInstance();
    
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ExchangeDataService.CHANNEL)) {
                MainActivityPageFragment mainActivityPageFragment = (MainActivityPageFragment) getSupportFragmentManager().findFragmentById(R.id.main_pager);
                if (mainActivityPageFragment == null) return;
                mainActivityPageFragment.refreshOrdersList(false, mainOrderDateCal);
            }
        }
    };
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.setGroupVisible(R.id.exchangeServiceMenuGroup, false);
        /*MenuItem menuItem = menu.findItem(R.id.add_order);
        menuItem.setIcon(android.R.drawable.ic_input_add)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);*/
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_order: {
                Calendar newOrderDateCal = mainOrderDateCal;
                
                Calendar nowStart = Calendar.getInstance();
                FormatsUtils.roundDayToStart(nowStart);
                
                if (nowStart.getTimeInMillis() > mainOrderDateCal.getTimeInMillis()) {
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
                FormatsUtils.roundDayToStart(mainOrderDateCal);
                viewPager.setCurrentItem(dateArrayList.indexOf(mainOrderDateCal.getTime()));
                MainActivityPageFragment mainActivityPageFragment = (MainActivityPageFragment) getSupportFragmentManager().findFragmentById(R.id.main_pager);
                mainActivityPageFragment.refreshOrdersList(true, mainOrderDateCal);
            }
            break;
            case R.id.return_today: {
                mainOrderDateCal = Calendar.getInstance();
                FormatsUtils.roundDayToStart(mainOrderDateCal);
                viewPager.setCurrentItem(dateArrayList.indexOf(mainOrderDateCal.getTime()));
            }
            break;
            case R.id.about_app: {
                Intent intent = new Intent(ctx, AboutActivity.class);
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
                    Long orderDateMillis = data.getExtras().getLong("orderDateMillis");
                    mainOrderDateCal.setTimeInMillis(orderDateMillis);
                }
                FormatsUtils.roundDayToStart(mainOrderDateCal);
                viewPager.setCurrentItem(dateArrayList.indexOf(mainOrderDateCal.getTime()));
                MainActivityPageFragment mainActivityPageFragment = (MainActivityPageFragment) getSupportFragmentManager().findFragmentById(R.id.main_pager);
                if (mainActivityPageFragment == null) return;
                mainActivityPageFragment.refreshOrdersList(true, mainOrderDateCal);
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
        
        mainOrderDateCal = Calendar.getInstance();
        if (savedInstanceState != null) {
            long dateMillis = savedInstanceState.getLong("orderDateLong");
            mainOrderDateCal.setTimeInMillis(dateMillis);
        }
        FormatsUtils.roundDayToStart(mainOrderDateCal);
        
        for (int i = -SettingsUtils.Settings.getOrderLogDepth(ctx); i <= SettingsUtils.Settings.getOrderDaysAhead(ctx); i++) {
            Calendar nDate = Calendar.getInstance();
            nDate.add(Calendar.DATE, i);
            FormatsUtils.roundDayToStart(nDate);
            dateArrayList.add(nDate.getTime());
        }
        
        viewPager = (ViewPager) findViewById(R.id.main_pager);
        viewPager.setAdapter(new MainActivityPagerAdapter(ctx, getSupportFragmentManager(), dateArrayList));
        viewPager.setCurrentItem(dateArrayList.indexOf(mainOrderDateCal.getTime()));
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    
            }
    
            @Override
            public void onPageSelected(int position) {
                mainOrderDateCal.setTime(dateArrayList.get(position));
                FormatsUtils.roundDayToStart(mainOrderDateCal);
                if (customPagerTabStrip != null) {
                    customPagerTabStrip.setTextColor(customPagerTabStrip.getDateColor(mainOrderDateCal));
                    customPagerTabStrip.setTabIndicatorColor(customPagerTabStrip.getDateColor(mainOrderDateCal));
                }
            }
    
            @Override
            public void onPageScrollStateChanged(int state) {
                
            }
        });
    
        customPagerTabStrip = (CustomPagerTabStrip) findViewById(R.id.pager_title_strip);
        if (customPagerTabStrip != null) {
            customPagerTabStrip.setTextColor(customPagerTabStrip.getDateColor(mainOrderDateCal));
            customPagerTabStrip.setTabIndicatorColor(customPagerTabStrip.getDateColor(mainOrderDateCal));
        }
    
        registerReceiver(broadcastReceiver, new IntentFilter(ExchangeDataService.CHANNEL));
        
        startService(new Intent(ctx, CheckApkUpdate.class));
        
        startExchangeDataService();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("orderDateLong", mainOrderDateCal.getTimeInMillis());
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
}