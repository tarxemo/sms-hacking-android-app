package com.example.sms;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("SyncWorker", "Periodic sync worker triggered.");
        Context context = getApplicationContext();
        
        DeviceAuthManager authManager = new DeviceAuthManager(context);
        if (authManager.getToken() != null) {
            // Ensure the service is running
            Intent serviceIntent = new Intent(context, SMSBackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            
            // Also trigger a direct sync just in case
            new SendSMSTask(context).execute();
        }
        
        return Result.success();
    }
}
