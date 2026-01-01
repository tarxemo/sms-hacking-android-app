package com.example.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;

        if (action.equals(Intent.ACTION_BOOT_COMPLETED) ||
                action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
                action.equals("android.intent.action.QUICKBOOT_POWERON") ||
                action.equals("com.htc.intent.action.QUICKBOOT_POWERON")) {

            Log.d("BootReceiver", "Device rebooted (Action: " + action + "), checking authentication...");
            
            DeviceAuthManager authManager = new DeviceAuthManager(context);
            if (authManager.getToken() != null) {
                Log.d("BootReceiver", "User is authenticated, starting background service.");
                Intent serviceIntent = new Intent(context, SMSBackgroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
                
                // Trigger immediate sync on boot
                Log.d("BootReceiver", "Triggering immediate boot sync.");
                new SendSMSTask(context).execute();
                
                // Reschedule Alarm Watchdog
                scheduleAlarm(context);
                // Ensure WorkManager is active
                scheduleWork(context);
                
            } else {
                Log.d("BootReceiver", "User not authenticated, service will start after first login.");
            }
        }
    }

    private void scheduleAlarm(Context context) {
        Intent intent = new Intent(context, SyncAlarmReceiver.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long interval = 30 * 60 * 1000;
            alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + interval,
                    interval,
                    pendingIntent
            );
        }
    }

    private void scheduleWork(Context context) {
        androidx.work.PeriodicWorkRequest syncRequest =
                new androidx.work.PeriodicWorkRequest.Builder(SyncWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                        .setConstraints(new androidx.work.Constraints.Builder()
                                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                                .build())
                        .build();

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SMS_SYNC_WORK",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
    }
}
