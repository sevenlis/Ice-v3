package by.ingman.sevenlis.ice_v3.activities;

import android.Manifest;
import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.CompoundButtonCompat;

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
    private CheckBox checkBoxFollow;
    private DBLocal dbLocal;
    private final BroadcastReceiver locationChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(LocationTrackingService.LOCATION_CHANGE_CHANNEL_ACTION)) {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    double lat = extras.getDouble(LocationTrackingService.LOCATION_CHANGE_LATITUDE_KEY);
                    double lon = extras.getDouble(LocationTrackingService.LOCATION_CHANGE_LONGITUDE_KEY);
                    if (checkBoxFollow.isChecked()) {
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
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
        
        curDateCalendar = Calendar.getInstance();
        onDateSetListener = (datePicker, year, monthOfYear, dayOfMonth) -> {
            if (curDateCalendar == null) {
                curDateCalendar = Calendar.getInstance();
            }
            curDateCalendar.set(year, monthOfYear, dayOfMonth);
            menuItemDate.setTitle(FormatsUtils.getDateFormatted(curDateCalendar.getTime()));
            showRoute(curDateCalendar);
        };
        dbLocal = new DBLocal(ctx);
        polylineOptions = new PolylineOptions();
        registerReceiver(locationChangeReceiver,new IntentFilter(LocationTrackingService.LOCATION_CHANGE_CHANNEL_ACTION));
        
        checkBoxFollow = new CheckBox(ctx);
        checkBoxFollow.setText(R.string.map_follow_my_location);
        checkBoxFollow.setTextColor(getResources().getColor(R.color.white));
        int[][] states = {{android.R.attr.state_checked}, {}};
        int[] colors = {getResources().getColor(R.color.white), getResources().getColor(R.color.white)};
        CompoundButtonCompat.setButtonTintList(checkBoxFollow, new ColorStateList(states, colors));
        checkBoxFollow.setChecked(false);
        checkBoxFollow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(!isChecked);
            }
            if (isChecked) {
                trackPolyline.remove();
                polylineOptions = new PolylineOptions();
                //polylineOptions.add(mMap.getCameraPosition().target);
                trackPolyline = mMap.addPolyline(polylineOptions);
            } else {
                showRoute(curDateCalendar);
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.maps_menu, menu);
        menuItemDate = menu.findItem(R.id.menu_item_date);
        menuItemDate.setTitle(FormatsUtils.getDateFormatted(curDateCalendar.getTime()));
        menuItemDate.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemDate.setOnMenuItemClickListener(item -> {
            DatePickerDialog dateDialog = new DatePickerDialog(ctx, onDateSetListener,
                    curDateCalendar.get(Calendar.YEAR),
                    curDateCalendar.get(Calendar.MONTH),
                    curDateCalendar.get(Calendar.DAY_OF_MONTH));
            dateDialog.show();
            return false;
        });
        
        MenuItem menuItemFollow = menu.findItem(R.id.menu_item_follow);
        menuItemFollow.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menuItemFollow.setActionView(checkBoxFollow);

        ActionBar.LayoutParams params = new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, FormatsUtils.getPixelsForDp(ctx,10), 0);
        checkBoxFollow.setLayoutParams(params);

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
    
        mMap.setOnMapClickListener(latLng -> {
            Toast.makeText(ctx, "onMapClick: " + latLng.latitude + "," + latLng.longitude, Toast.LENGTH_SHORT).show();
            marker.remove();
            marker = mMap.addMarker(new MarkerOptions().position(latLng));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15),1000,null);
        });
        
        mMap.setOnMapLongClickListener(latLng -> Toast.makeText(ctx, "onMapLongClick: " + latLng.latitude + "," + latLng.longitude, Toast.LENGTH_SHORT).show());
        
        mMap.setOnCameraMoveListener(() -> {
            //Toast.makeText(ctx, "onCameraMove: " + mMap.getCameraPosition().target.latitude + "," + mMap.getCameraPosition().target.longitude, Toast.LENGTH_SHORT).show();
        });
    
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        
        mMap.clear();
    }
}
