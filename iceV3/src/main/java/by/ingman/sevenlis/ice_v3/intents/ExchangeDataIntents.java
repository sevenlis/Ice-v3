package by.ingman.sevenlis.ice_v3.intents;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import by.ingman.sevenlis.ice_v3.services.ExchangeDataService;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class ExchangeDataIntents {
    private static PendingIntent exchangeDataServicePendingIntent = null;
    private static Intent exchangeDataServiceIntent = null;
    private static AlarmManager alarmManager = null;
    
    public static Intent getExchangeDataServiceIntent(Context ctx) {
        if (exchangeDataServiceIntent == null) {
            exchangeDataServiceIntent = new Intent(ctx, ExchangeDataService.class);
        }
        return exchangeDataServiceIntent;
    }
    
    private static PendingIntent getExchangeDataServicePendingIntent(Context ctx) {
        if (exchangeDataServicePendingIntent == null) {
            exchangeDataServicePendingIntent = PendingIntent.getService(ctx, 0, getExchangeDataServiceIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return exchangeDataServicePendingIntent;
    }
    
    private static AlarmManager getAlarmManager(Context ctx) {
        if (alarmManager == null) {
            alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        }
        return alarmManager;
    }
    
    public void startExchangeDataServiceAlarm(Context ctx) {
        getAlarmManager(ctx).cancel(getExchangeDataServicePendingIntent(ctx));
        long updateFreq = SettingsUtils.Settings.getDataUpdateInterval(ctx) * 1000;
        long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq;
        getAlarmManager(ctx).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, timeToRefresh, updateFreq, getExchangeDataServicePendingIntent(ctx));
    }
    
    public void stopExchangeDataServiceAlarm(Context ctx) {
        getAlarmManager(ctx).cancel(getExchangeDataServicePendingIntent(ctx));
    }
    
}
