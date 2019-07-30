package com.abcar.driver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.annotation.NonNull;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Locale;


public class Tracking extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private MapView mapView;
    private AlertDialog alert1;
    private Boolean isOn;
    private BroadcastReceiver mMessageReceiver;
    TextView platTracking, campaignTracking, addressText;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    Double lastLatitude, lastLongitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            Boolean is_on_boot = extras.getBoolean("IS_ON_BOOT");
            if(is_on_boot == true){
                Intent serviceIntent = new Intent(Tracking.this, TrackingService.class);
                ContextCompat.startForegroundService(Tracking.this, serviceIntent);
                finish();
            }
        }
        setContentView(R.layout.activity_tracking);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        final Button button = (Button) findViewById(R.id.button_tracking);
        button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        platTracking = (TextView)findViewById(R.id.platTrackingView);
        addressText = (TextView)findViewById(R.id.addressText);
        campaignTracking = (TextView)findViewById(R.id.campaignTrackingView);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();
        boolean logged_in = sharedPref.getBoolean("logged_in", false);
        boolean gps_on = sharedPref.getBoolean("gps_on", false);
        if(logged_in != true){
            Intent i = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(i);
        }
        else{
            platTracking.setText(sharedPref.getString("plat", "null"));
            campaignTracking.setText(sharedPref.getString("campaign", "null"));
            lastLatitude = Double.valueOf(sharedPref.getString("lastLatitude", "0"));
            lastLongitude = Double.valueOf(sharedPref.getString("lastLongitude", "0"));
            setAddress();
        }
        isOn = gps_on;
        final AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        if (isOn == true) {
            builder1.setMessage("Turn OFF Tracking?");
            button.setText("Tracking ON");
            button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            Intent serviceIntent = new Intent(Tracking.this, TrackingService.class);
            ContextCompat.startForegroundService(Tracking.this, serviceIntent);
        } else {
            builder1.setMessage("Turn ON Tracking?");
            button.setText("Tracking OFF");
            button.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            Intent serviceIntent = new Intent(Tracking.this, TrackingService.class);
            stopService(serviceIntent);
        }
        builder1.setCancelable(true);
        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (isOn != true) {
                            isOn = true;
                            builder1.setMessage("Turn OFF Tracking?");
                            button.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                            button.setText("Tracking ON");
                            Intent serviceIntent = new Intent(Tracking.this, TrackingService.class);
                            ContextCompat.startForegroundService(Tracking.this, serviceIntent);
                        } else {
                            isOn = false;
                            builder1.setMessage("Turn ON Tracking?");
                            button.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                            button.setText("Tracking OFF");
                            Intent serviceIntent = new Intent(Tracking.this, TrackingService.class);
                            stopService(serviceIntent);
                        }
                        editor.putBoolean("gps_on", isOn);
                        editor.commit();
                        dialog.cancel();
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                alert1 = builder1.create();
                alert1.show();
            }
        });
        if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 101);

        }
        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                lastLatitude = Double.valueOf(intent.getStringExtra("lastLatitude"));
                lastLongitude = Double.valueOf(intent.getStringExtra("lastLongitude"));
                setAddress();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("GPSLocationUpdates"));
    }
    private void setAddress(){
        if(lastLatitude != 0 && lastLongitude != 0){
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lastLatitude, lastLongitude, 1);
                addressText.setText(addresses.get(0).getAddressLine(0)+", "+
                        addresses.get(0).getAddressLine(1)+", "+addresses.get(0).getAddressLine(2));

                LatLng jak = new LatLng(lastLatitude, lastLongitude);
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(jak).title("Latest Location"));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(jak));
            }catch(Exception e)
            {

            }
        }
        else {

        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng jak = new LatLng(-6.1634976, 106.8119999);
        mMap.addMarker(new MarkerOptions().position(jak).title("Latest Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(jak));
        mMap.setMinZoomPreference(15);

    }
    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        try {
            unregisterReceiver(mMessageReceiver);
        }catch (Exception e){

        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

}
