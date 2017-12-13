package by.ingman.sevenlis.ice_v3.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;

import by.ingman.sevenlis.ice_v3.R;
import by.ingman.sevenlis.ice_v3.local.DBLocal;
import by.ingman.sevenlis.ice_v3.utils.FormatsUtils;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private Context ctx;
    private GoogleMap mMap;
    private LocationManager locationManager;
    private DatePickerDialog.OnDateSetListener onDateSetListener;
    private Calendar curDateCalendar;
    private MenuItem menuItemDate;
    private DBLocal dbLocal;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        
        ctx = this;
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync((OnMapReadyCallback) ctx);
        
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
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initMap();
        showRoute(curDateCalendar);
    }
    
    private void showRoute(Calendar dateCalendar) {
        ArrayList<LatLng> positions = dbLocal.getRoutePositions(dateCalendar.getTimeInMillis());
        PolylineOptions polylineOptions = new PolylineOptions();
        for (LatLng latLng : positions) {
            polylineOptions.add(latLng);
        }
        mMap.clear();
        mMap.addPolyline(polylineOptions);
        
        LatLng initLatLng = dbLocal.getStartRoutePosition(curDateCalendar.getTimeInMillis());
        mMap.addMarker(new MarkerOptions().position(initLatLng));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initLatLng, 15));
    }
    
    private void initMap() {
        UiSettings settings = mMap.getUiSettings();
        settings.setAllGesturesEnabled(true);
        settings.setZoomControlsEnabled(true);
        settings.setZoomGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(true);
        
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Toast.makeText(ctx, "onMapClick: " + latLng.latitude + "," + latLng.longitude, Toast.LENGTH_SHORT).show();
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng));
            }
        });
        
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                Toast.makeText(ctx, "onMapLongClick: " + latLng.latitude + "," + latLng.longitude, Toast.LENGTH_LONG).show();
                
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
//
//        LatLng initLatLng;
//        Location lastLocation = getLastKnownLocation();
//        if (lastLocation == null) {
//            initLatLng = dbLocal.getStartRoutePosition(curDateCalendar.getTimeInMillis());
//        } else {
//            initLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
//        }
//        mMap.clear();
//        mMap.addMarker(new MarkerOptions().position(initLatLng));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initLatLng, 15));
    }
}
