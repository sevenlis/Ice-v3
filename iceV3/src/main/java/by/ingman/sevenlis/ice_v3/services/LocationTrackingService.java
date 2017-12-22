package by.ingman.sevenlis.ice_v3.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import java.util.Set;

import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.SettingsUtils;

public class LocationTrackingService extends Service {
    public static final String LOCATION_CHANGE_CHANNEL_ACTION = "by.ingman.sevenlis.ice_v3." + LocationTrackingService.class.getSimpleName() + ".broadcastLocationChangeChannel";
    public static final String LOCATION_CHANGE_LATITUDE_KEY = LOCATION_CHANGE_CHANNEL_ACTION + ".Latitude";
    public static final String LOCATION_CHANGE_LONGITUDE_KEY = LOCATION_CHANGE_CHANNEL_ACTION + ".Longitude";
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Context ctx;
    
    private boolean getLocationPermissions() {
        return ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    
    @Override
    @SuppressLint("MissingPermission")
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (getLocationPermissions()) {
            Set<String> providers = SettingsUtils.Settings.getLocationTrackingProviders(ctx);
            
            if (providers.contains(SettingsUtils.LOCATION_TRACKING_PROVIDER_GPS) &
                    ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener);
            }
            if (providers.contains(SettingsUtils.LOCATION_TRACKING_PROVIDER_NETWORK) &
                    ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener);
            }
            if (providers.contains(SettingsUtils.LOCATION_TRACKING_PROVIDER_PASSIVE)) {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0L, 0f, locationListener);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    
    @Override
    public void onCreate() {
        ctx = this;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                saveLocationInDataBase(location);
                
                Intent intent = new Intent(LOCATION_CHANGE_CHANNEL_ACTION);
                intent.putExtra(LOCATION_CHANGE_LATITUDE_KEY,location.getLatitude());
                intent.putExtra(LOCATION_CHANGE_LONGITUDE_KEY,location.getLongitude());
                sendBroadcast(intent);
            }
            
            @Override
            @SuppressLint("MissingPermission")
            public void onStatusChanged(String provider, int status, Bundle extras) {
                if (getLocationPermissions() && status == LocationProvider.AVAILABLE) {
                    saveLocationInDataBase(locationManager.getLastKnownLocation(provider));
                }
            }
            
            @Override
            @SuppressLint("MissingPermission")
            public void onProviderEnabled(String provider) {
                if (getLocationPermissions()) {
                    saveLocationInDataBase(locationManager.getLastKnownLocation(provider));
                }
            }
            
            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        super.onCreate();
    }
    
    private void saveLocationInDataBase(Location location) {
        if (location != null) {
            new DBLocal(ctx).saveLocation(location.getLatitude(), location.getLongitude(), location.getTime());
        }
    }
    
    @Override
    public void onDestroy() {
        locationManager.removeUpdates(locationListener);
        super.onDestroy();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
