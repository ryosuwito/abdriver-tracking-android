package com.abcar.driver;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

public class RebootReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "mainNotificationChannel";
    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    @Override
    public void onReceive(Context context, Intent intent) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPref.edit();
        editor.putBoolean("gps_on", true);
        editor.putBoolean("support_startup", true);
        editor.commit();
        Toast.makeText(context, "Received Succesfully", Toast.LENGTH_LONG).show();
//

        Log.d("Receiver", "Received Successfully");
        PendingIntent pendingIntent;
        Intent pIntent = new Intent(context, TrackingService.class);
        pendingIntent = PendingIntent.getService(context,
                0, pIntent, 0);
        AlarmManager alarm = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis() + 60000, pendingIntent);
        Toast.makeText(context, "Timer set to " + "60" + " seconds.",
                Toast.LENGTH_LONG).show();
// notificationId is a unique int for each notification that you must define

        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
//            Intent serviceIntent = new Intent(context, TrackingService.class);
//            ContextCompat.startForegroundService(context, serviceIntent);
//            Intent i = new Intent();
//            i.setClass(context,Tracking.class);
//            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            i.putExtra("IS_ON_BOOT", true);
//            context.getApplicationContext().startActivity(i);
//            Intent torService = new Intent(context, TrackingService.class);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                context.startForegroundService(torService);
//            }
//            else
//            {
//                context.startService(torService);
//            }
        }
    }
}
