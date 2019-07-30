package com.abcar.driver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class TrackingService extends Service implements LocationListener {
    public static final String CHANNEL_ID = "mainNotificationChannel";
    private LocationManager locationManager;
    private String postUrl, currentLat, currentLong,connMessage, plat, campaign;
    private HttpURLConnection conn;
    private OutputStream os;
    private URL mUrl;
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    Double lastLatitude, lastLongitude;
    public TrackingService() {
    }
    @Override
    public void onCreate() {
        super.onCreate();
        this.registerReceiver(this.mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
//        this.registerReceiver(this.mBatChargingReceiver,
//                new IntentFilter(Intent.ACTION_POWER_CONNECTED));
    }
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            int level = intent.getIntExtra("level", 0);
            int scale = intent.getIntExtra("scale", 0);
            float batteryPct = level / (float)scale;
            float limit = (float) 0.15;
            if(batteryPct <= limit ){
                Intent serviceIntent = new Intent(getApplicationContext(), TrackingService.class);
                stopService(serviceIntent);
            }
        }
    };
//    private BroadcastReceiver mBatChargingReceiver = new BroadcastReceiver(){
//        @Override
//        public void onReceive(Context arg0, Intent intent) {
//            int status = intent.getIntExtra("status", -1);
//            if( status >= 0 ){
//                Intent serviceIntent = new Intent(getApplicationContext(), TrackingService.class);
//                stopService(serviceIntent);
//            }
//        }
//    };
    public void onDestroy(){
        // unregister receiver
        this.unregisterReceiver(this.mBatInfoReceiver);
//        this.unregisterReceiver(this.mBatChargingReceiver);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        postUrl = "http://dev.abplusscar.com/gps/save/";
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();
        boolean logged_in =sharedPref.getBoolean("logged_in", false);
        String notifMessage;
        if(logged_in == true){
            notifMessage = "GPS Tracking is ON";
            plat = sharedPref.getString("plat", "XXXXXXXXX");
            campaign = sharedPref.getString("campaign", "XXXXXXXXX");
            lastLatitude = Double.valueOf(sharedPref.getString("lastLatitude", "0"));
            lastLongitude = Double.valueOf(sharedPref.getString("lastLongitude", "0"));
        }
        else {
            notifMessage = "Login Error";
        }

        Intent notificationIntent = new Intent(this, Tracking.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Main Notification Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            serviceChannel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(notifMessage)
                .setSmallIcon(R.drawable.logo)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.InboxStyle())
                .build();
//        String input = intent.getStringExtra("inputExtra");

        startForeground(1, notification);
        //do heavy work on a background thread
        //stopSelf();

        if(logged_in == true){
            getLocation();
        }
        else {
        }
        return START_STICKY;
    }

    void getLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, this);
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
        }
        catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Boolean isOn = sharedPref.getBoolean("gps_on", false);
        if (isOn != true) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.removeUpdates(this);
            locationManager = null;
            stopSelf();
        }

        String locationText;
        locationText ="Latitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude();
//        locationView.setText(locationText);
        try {
//            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
//            addressView.setText(addresses.get(0).getAddressLine(0)+", "+
//                    addresses.get(0).getAddressLine(1)+", "+addresses.get(0).getAddressLine(2));
            currentLat = Double.toString(location.getLatitude());
            currentLong = Double.toString(location.getLongitude());
            float speed = location.getSpeed();
            Log.i("SPEED", String.valueOf(speed));
            editor.putString("lastLatitude",currentLat);
            editor.putString("lastLongitude",currentLong);
            editor.commit();

            Intent intent = new Intent("GPSLocationUpdates");
            intent.putExtra("lastLatitude",currentLat);
            intent.putExtra("lastLongitude",currentLong);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            new SaveSettingTask().execute(mUrl);
        }catch(Exception e)
        {

        }

    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private class SaveSettingTask extends AsyncTask<URL, Integer, Long> {

        @Override
        protected Long doInBackground(URL... urls) {
            try {
                String baseUrl;
                baseUrl = postUrl + plat.replace(" ", "").toUpperCase() +"/?";
                baseUrl += "lat="+currentLat;
                baseUrl += "&lng="+currentLong;
                baseUrl += "&lastlat="+lastLatitude;
                baseUrl += "&lastlng="+lastLongitude;
                baseUrl += "&cmp="+campaign.replace(" ", "_").toLowerCase();
                Log.v("murls", baseUrl);
                mUrl = new URL(baseUrl);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            try {
                conn = (HttpURLConnection) mUrl.openConnection();
                conn.setReadTimeout(10000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                os = conn.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                os.close();
                Log.v("responseCode",conn.getResponseMessage());
                connMessage = conn.getResponseMessage();
                conn.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
