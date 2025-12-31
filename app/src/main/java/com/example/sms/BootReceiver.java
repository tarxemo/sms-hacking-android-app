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
            } else {
                Log.d("BootReceiver", "User not authenticated, service will start after first login.");
            }
        }
    }
}
