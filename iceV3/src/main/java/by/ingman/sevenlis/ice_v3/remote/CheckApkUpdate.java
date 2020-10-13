package by.ingman.sevenlis.ice_v3.remote;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.TaskStackBuilder;

import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.activities.MainActivity;
import by.ingman.sevenlis.ice_v3.activities.UpdateDataActivity;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class CheckApkUpdate extends IntentService {
    private static final int NOTIFY_ID = 398;
    private static final String NOTIF_CHANNEL_ID = "APK_UPDATE_CHANNEL_ID";
    private static NotificationManager notificationManager;
    private CharSequence channelName = "ICE-V3 APK update notification channel";
    public CheckApkUpdate() {
        super(CheckApkUpdate.class.getSimpleName());
    }
    
    @Override
    public void onCreate() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(NOTIF_CHANNEL_ID,channelName,importance);
            channel.setVibrationPattern(new long[]{0});
            channel.enableVibration(false);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
        super.onCreate();
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        File versionInfoFile = initVersionInfoFile();
        if (!isConnected() || versionInfoFile == null) return;
        
        try {
            getVersionInfoFileRemote(versionInfoFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        int curVersionCode = getVersionCodeLocal();
        int newVersionCode = readVersionCodeFromFile(versionInfoFile);
        double curVersionName = Double.parseDouble(getVersionNameLocal());
        double newVersionName = Double.parseDouble(readVersionNameFromFile(versionInfoFile));
        
        if (curVersionCode < newVersionCode || curVersionName < newVersionName) {
            sendUpdateAvailableNotification(newVersionCode, newVersionName);
        }
        stopSelf();
    }
    
    private void sendUpdateAvailableNotification(int newVersionCode, double newVersionName) {
        Notification.Builder mBuilder;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            mBuilder = new Notification.Builder(this,NOTIF_CHANNEL_ID);
            mBuilder.setChannelId(NOTIF_CHANNEL_ID);
        } else {
            mBuilder = new Notification.Builder(this);
        }
        mBuilder.setTicker("Доступно обновление")
                .setSmallIcon(R.drawable.ic_info_white)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ice_pict))
                .setContentTitle("Доступно обновление")
                .setContentText("Доступно обновление до версии " + FormatsUtils.getNumberFormatted(newVersionCode, 0).trim() + " (" + FormatsUtils.getNumberFormatted(newVersionName, 3).trim() + ")");
        Intent resultIntent = new Intent(this, UpdateDataActivity.class);
        
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        
        notificationManager.notify(NOTIFY_ID, mBuilder.build());
    }
    
    public void cancelUpdateAvailableNotification() {
        notificationManager.cancel(NOTIFY_ID);
    }
    
    private int readVersionCodeFromFile(File versionInfoFile) {
        int versionCode = 0;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(versionInfoFile));
            int count = 0;
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                count++;
                if (count == 1) {
                    versionCode = Integer.parseInt(str);
                    break;
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
    
    private String readVersionNameFromFile(File versionInfoFile) {
        String versionName = "0.000";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(versionInfoFile));
            int count = 0;
            String str;
            while ((str = bufferedReader.readLine()) != null) {
                count++;
                if (count == 2) {
                    versionName = str;
                    break;
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return versionName;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
        return ni != null && ni.isConnected();
    }
    
    public int getVersionCodeLocal() {
        int versionCode = 0;
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }
    
    public String getVersionNameLocal() {
        String versionName = "0.000";
        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }
    
    @Nullable
    private File initVersionInfoFile() {
        File appFolder = new File(String.format("%s%s%s", getApplicationContext().getFilesDir().getAbsolutePath(), File.separator, SettingsUtils.APP_FOLDER));
        if (!appFolder.exists()) {
            if (!appFolder.mkdir()) {
                Toast.makeText(getApplicationContext(), "Error creating folder " + appFolder.getPath(), Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        File mFile = new File(appFolder.getPath() + "/version.info");
        if (!mFile.exists()) {
            try {
                if (mFile.createNewFile()) {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(mFile));
                    writer.write(String.valueOf(getVersionCodeLocal()));
                    writer.newLine();
                    writer.write(getVersionNameLocal());
                    writer.flush();
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Error creating file " + mFile.getPath(), Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return mFile;
    }
    
    private void getVersionInfoFileRemote(File versionInfoFile) throws IOException {
        FTPClient ftpClient = FTPClientConnector.getFtpClient();
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        ftpClient.retrieveFile("/htdocs/iceV3/version.info",new FileOutputStream(versionInfoFile));
        FTPClientConnector.disconnectClient();
    }
    
}
