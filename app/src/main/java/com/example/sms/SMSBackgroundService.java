package com.example.sms;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class SMSBackgroundService extends Service {
    private static final String CHANNEL_ID = "SMSServiceChannel";
    private SMSDatabaseHelper dbHelper;
    private Timer timer;
    private Handler handler;

    private SMSObserver smsObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new SMSDatabaseHelper(this);
        handler = new Handler();
        
        smsObserver = new SMSObserver(handler);
        getContentResolver().registerContentObserver(
                Uri.parse("content://sms"),
                true,
                smsObserver
        );

        startForegroundService();
        startSyncTask();
    }

    private class SMSObserver extends android.database.ContentObserver {
        public SMSObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.d("SMSBackgroundService", "SMS database changed, check for new messages...");
            processNewMessages();
        }
    }

    private void processNewMessages() {
        new Thread(() -> {
            DeviceAuthManager authManager = new DeviceAuthManager(this);
            long lastSync = authManager.getLastSyncTimestamp();
            
            Uri uri = Uri.parse("content://sms/");
            String selection = "date > ?";
            String[] selectionArgs = new String[]{String.valueOf(lastSync)};
            
            Cursor cursor = getContentResolver().query(uri, null, selection, selectionArgs, "date ASC");
            long maxTs = lastSync;

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                    String message = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                    long date = cursor.getLong(cursor.getColumnIndexOrThrow("date"));
                    int typeInt = cursor.getInt(cursor.getColumnIndexOrThrow("type"));
                    
                    String type = (typeInt == 1) ? "received" : "sent";
                    String formattedTimestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", date).toString();

                    dbHelper.insertSMS(sender, message, formattedTimestamp, type);
                    if (date > maxTs) {
                        maxTs = date;
                    }
                }
                cursor.close();
            }

            if (maxTs > lastSync) {
                authManager.setLastSyncTimestamp(maxTs);
                Log.d("SMSBackgroundService", "New messages found, triggering sync.");
                new SendSMSTask(this).execute();
            }
        }).start();
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Sync Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SMS Sync Service")
                .setContentText("Syncing SMS in the background")
                .setSmallIcon(R.drawable.ic_sms)
                .build();

        startForeground(1, notification);
    }

    private void startSyncTask() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                handler.post(() -> readAndSendSMS());
            }
        }, 0, 5 * 60 * 1000); // Runs every 5 minutes
    }

    private void readAndSendSMS() {
        Log.d("SMSBackgroundService", "Triggering background sync...");
        new SendSMSTask(this).execute();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
        if (smsObserver != null) {
            getContentResolver().unregisterContentObserver(smsObserver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
