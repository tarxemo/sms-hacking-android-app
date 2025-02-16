package com.example.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (NetworkUtil.isOnline(context)) {
            Log.d("NetworkChangeReceiver", "Network is online, syncing messages...");
            new SendSMSTask(context).execute(); // Trigger sync when online
        } else {
            Log.d("NetworkChangeReceiver", "Network is offline.");
        }
    }

}
