package by.ingman.sevenlis.ice_v3.services;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;

import by.ingman.sevenlis.ice_v3.local.DBLocal;

public class LocationTrackerIntentServiceTask extends IntentService {
    private Context ctx;
    
    public LocationTrackerIntentServiceTask() {
        super(LocationTrackerIntentServiceTask.class.getSimpleName());
        ctx = this;
    }
    
    private void saveLocationInDataBase(final Location location) {
        if (location != null) {
            new DBLocal(ctx).saveLocation(location.getLatitude(), location.getLongitude(), location.getTime());
        }
    }
    
    private Location getLastKnownLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return new Location("fused");
        }
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return new Location(locationManager.getBestProvider(new Criteria(), true));
        }
        Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(new Criteria(), true));
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (location == null) {
            location = locationManager.getLastKnownLocation("fused");
        }
        return location;
    }
    
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        saveLocationInDataBase(getLastKnownLocation());
    }
}
