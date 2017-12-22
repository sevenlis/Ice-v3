package by.ingman.sevenlis.ice_v3.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Calendar;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.services.LocationTrackingService;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private Context ctx;
    private GoogleMap mMap;
    private Marker marker;
    private Polyline trackPolyline;
    private PolylineOptions polylineOptions;
    private DatePickerDialog.OnDateSetListener onDateSetListener;
    private Calendar curDateCalendar;
    private MenuItem menuItemDate;
    private MenuItem menuItemFollow;
    private DBLocal dbLocal;
    private BroadcastReceiver locationChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(LocationTrackingService.LOCATION_CHANGE_CHANNEL_ACTION)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    double lat = extras.getDouble(LocationTrackingService.LOCATION_CHANGE_LATITUDE_KEY);
                    double lon = extras.getDouble(LocationTrackingService.LOCATION_CHANGE_LONGITUDE_KEY);
                    if (menuItemFollow.isChecked()) {
                        followLocation(new LatLng(lat, lon));
                    }
                }
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        
        ctx = this;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(OnMapReadyCallback.class.cast(this));
        
        curDateCalendar = Calendar.getInstance();
        onDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                if (curDateCalendar == null) {
                    curDateCalendar = Calendar.getInstance();
                }
                curDateCalendar.set(year, monthOfYear, dayOfMonth);
                menuItemDate.setTitle(FormatsUtils.getDateFormatted(curDateCalendar.getTime()));
                showRoute(curDateCalendar);
            }
        };
        dbLocal = new DBLocal(ctx);
        polylineOptions = new PolylineOptions();
        registerReceiver(locationChangeReceiver,new IntentFilter(LocationTrackingService.LOCATION_CHANGE_CHANNEL_ACTION));
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menuItemDate = menu.add(FormatsUtils.getDateFormatted(curDateCalendar.getTime()));
        menuItemDate.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemDate.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                DatePickerDialog dateDialog = new DatePickerDialog(ctx, onDateSetListener,
                        curDateCalendar.get(Calendar.YEAR),
                        curDateCalendar.get(Calendar.MONTH),
                        curDateCalendar.get(Calendar.DAY_OF_MONTH));
                dateDialog.show();
                return false;
            }
        });
        menuItemFollow = menu.add(R.string.map_follow_my_location);
        menuItemFollow.setCheckable(true).setChecked(false);
        menuItemFollow.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                menuItemFollow.setChecked(!menuItemFollow.isChecked());
                if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    mMap.setMyLocationEnabled(!menuItemFollow.isChecked());
                }
                if (menuItemFollow.isChecked()) {
                    trackPolyline.remove();
                    polylineOptions = new PolylineOptions();
                    //polylineOptions.add(mMap.getCameraPosition().target);
                    trackPolyline = mMap.addPolyline(polylineOptions);
                } else {
                    showRoute(curDateCalendar);
                }
                return false;
            }
        });
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        initMap(googleMap);
        showRoute(curDateCalendar);
    }
    
    @Override
    protected void onDestroy() {
        unregisterReceiver(locationChangeReceiver);
        super.onDestroy();
    }
    
    private void followLocation(LatLng latLng) {
        marker.remove();
        marker = mMap.addMarker(new MarkerOptions().position(latLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,20),1000,null);
        
        polylineOptions.add(latLng);
        trackPolyline.remove();
        trackPolyline = mMap.addPolyline(polylineOptions);
    }
    
    private void showRoute(Calendar dateCalendar) {
        Iterable<LatLng> positions = dbLocal.getRoutePositions(dateCalendar.getTimeInMillis());
        
        polylineOptions = new PolylineOptions();
        polylineOptions.addAll(positions);
        
        trackPolyline.remove();
        trackPolyline = mMap.addPolyline(polylineOptions);
        
        LatLng initLatLng = dbLocal.getStartRoutePosition(curDateCalendar.getTimeInMillis());
        marker.remove();
        marker = mMap.addMarker(new MarkerOptions().position(initLatLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(initLatLng,15),1000,null);
    }
    
    private void initMap(GoogleMap googleMap) {
        this.mMap = googleMap;
        
        UiSettings settings = mMap.getUiSettings();
        settings.setAllGesturesEnabled(true);
        settings.setZoomControlsEnabled(true);
        settings.setZoomGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(true);
    
        polylineOptions = new PolylineOptions();
        trackPolyline = mMap.addPolyline(polylineOptions);
        marker = mMap.addMarker(new MarkerOptions().position(new LatLng(0.0d,0.0d)));
    
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Toast.makeText(ctx, "onMapClick: " + latLng.latitude + "," + latLng.longitude, Toast.LENGTH_SHORT).show();
                marker.remove();
                marker = mMap.addMarker(new MarkerOptions().position(latLng));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15),1000,null);
            }
        });
        
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Toast.makeText(ctx, "onMapLongClick: " + latLng.latitude + "," + latLng.longitude, Toast.LENGTH_SHORT).show();
            }
        });
        
        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                //Toast.makeText(ctx, "onCameraMove: " + mMap.getCameraPosition().target.latitude + "," + mMap.getCameraPosition().target.longitude, Toast.LENGTH_SHORT).show();
            }
        });
    
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        
        mMap.clear();
    }
}
