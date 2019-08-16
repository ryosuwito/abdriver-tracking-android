package com.abcar.driver;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private AlertDialog alert1;
    private AlertDialog.Builder builder1;
    private WebView mWebView;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private String token, driverUrl = "http://driver.abplusscar.com/dashboard.html";
    private TextView warning, warning_detail;
    private Boolean is_from_notification;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = getOnNavigationItemSelectedListener();

    private BottomNavigationView.OnNavigationItemSelectedListener getOnNavigationItemSelectedListener() {
        return new BottomNavigationView.OnNavigationItemSelectedListener() {

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.navigation_home:
                Intent i = new Intent(getApplicationContext(), Tracking.class);
                startActivity(i);
                return true;
            case R.id.navigation_reward:
                Intent k = new Intent(getApplicationContext(), BuktiActivity.class);
                startActivity(k);
                return true;
            case R.id.navigation_dashboard:
                return true;
//                case R.id.navigation_notifications:
//                    Intent j = new Intent(getApplicationContext(), NotificationActivity.class);
//                    startActivity(j);
//                    return true;
        }
        return false;
    }
};
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent newIntent = getIntent();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();
        String plat = sharedPref.getString("plat", "XXXXXXXXX");
        String campaign = sharedPref.getString("campaign", "null");
        driverUrl = driverUrl + "?license_no=" + plat;
        is_from_notification = false;
        if (newIntent.hasExtra("jobLink")){
            driverUrl = newIntent.getStringExtra("jobLink");
            is_from_notification = true;
        }
        warning = (TextView)findViewById(R.id.warning);
        warning_detail = (TextView)findViewById(R.id.warning_detail);
        mWebView = (WebView)findViewById(R.id.mainWebview);
        mWebView.setWebViewClient(new WebViewClient());
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        mWebView.loadUrl(driverUrl);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navView.setSelectedItemId(R.id.navigation_dashboard);
        builder1 = new AlertDialog.Builder(this);
        builder1.setTitle("Logout");
        builder1.setMessage("Are You Sure?");
        builder1.setCancelable(true);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = sharedPref.edit();
        token = sharedPref.getString("token", "");
        boolean first_run =sharedPref.getBoolean("first_run", true);
        if(first_run == true){
            editor.putBoolean("first_run", false);
            editor.commit();
            hideWarning();
        }
        else {
            boolean support_startup =sharedPref.getBoolean("support_startup", false);
            if(support_startup){
                hideWarning();
            }
        }
        boolean logged_in =sharedPref.getBoolean("logged_in", false);
        ComponentName receiver = new ComponentName(MainActivity.this, RebootReceiver.class);
        PackageManager pm = MainActivity.this.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        if(logged_in != true){
            Intent i = new Intent(getApplicationContext(),LoginActivity.class);
            startActivity(i);
        }
        else{
            Boolean isOn = sharedPref.getBoolean("gps_on", false);
            if (isOn == true) {
                Intent serviceIntent = new Intent(MainActivity.this, TrackingService.class);
                ContextCompat.startForegroundService(MainActivity.this, serviceIntent);
            } else {
                Intent serviceIntent = new Intent(MainActivity.this, TrackingService.class);
                stopService(serviceIntent);
            }
        }
        warning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideWarning();
            }
        });
        warning_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideWarning();
            }
        });
        builder1.setMessage("Are You Sure Want to Logout?");
        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        logout();
                        Intent serviceIntent = new Intent(MainActivity.this, TrackingService.class);
                        stopService(serviceIntent);
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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
            if (!power.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }
    protected void hideWarning(){
        warning_detail.setVisibility(View.GONE);
        warning.setVisibility(View.GONE);
    }
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
    }
    void logout(){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.edit().clear().commit();
        mWebView.destroy();
        Intent i = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(i);
    }
    @Override
    public void onBackPressed(){
        if(mWebView!=null){
            if(mWebView.canGoBack()) {
                mWebView.goBack();
            }
            else{
                if(is_from_notification){
                    mWebView.loadUrl(driverUrl);
                }
                else{
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//***Change Here***
                    startActivity(intent);
                    finish();
                    System.exit(0);
                }
            }
        }
        else{
            super.onBackPressed();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.logoutMenu:
                alert1 = builder1.create();
                alert1.show();
                return true;
            case R.id.helpMenu:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
