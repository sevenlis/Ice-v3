package by.ingman.sevenlis.ice_v3.intents;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import by.ingman.sevenlis.ice_v3.services.LocationTrackerIntentServiceTask;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class LocationTrackerTaskIntents {
    private static Intent locationTrackerIntent = null;
    private static PendingIntent locationTrackerPendingIntent = null;
    private static AlarmManager alarmManager = null;
    
    public static Intent getLocationTrackerTaskIntent(Context ctx) {
        if (locationTrackerIntent == null) {
            locationTrackerIntent = new Intent(ctx, LocationTrackerIntentServiceTask.class);
        }
        return locationTrackerIntent;
    }
    
    private static PendingIntent getLocationTrackerTaskPendingIntent(Context ctx) {
        if (locationTrackerPendingIntent == null) {
            locationTrackerPendingIntent = PendingIntent.getService(ctx, 0, getLocationTrackerTaskIntent(ctx), PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return locationTrackerPendingIntent;
    }
    
    private static AlarmManager getAlarmManager(Context ctx) {
        if (alarmManager == null) {
            alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        }
        return alarmManager;
    }
    
    public void startLocationTrackerServiceTaskAlarm(Context ctx) {
        getAlarmManager(ctx).cancel(getLocationTrackerTaskPendingIntent(ctx));
        long updateFreq = SettingsUtils.Settings.getLocationTrackingInterval(ctx) * 1000;
        long timeToRefresh = SystemClock.elapsedRealtime() + updateFreq;
        getAlarmManager(ctx).setInexactRepeating(AlarmManager.ELAPSED_REALTIME, timeToRefresh, updateFreq, getLocationTrackerTaskPendingIntent(ctx));
    }
    
    public void stopLocationTrackerServiceTaskAlarm(Context ctx) {
        getAlarmManager(ctx).cancel(getLocationTrackerTaskPendingIntent(ctx));
    }
}
