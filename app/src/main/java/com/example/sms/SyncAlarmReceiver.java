package com.example.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class SyncAlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SyncAlarmReceiver", "Alarm-based heartbeat triggered.");
        DeviceAuthManager authManager = new DeviceAuthManager(context);
        if (authManager.getToken() != null) {
            Intent serviceIntent = new Intent(context, SMSBackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
