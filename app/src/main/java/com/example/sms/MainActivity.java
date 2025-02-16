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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_READ_SMS = 1;
    private SMSDatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private SMSAdapter smsAdapter;
    private List<SMSData> smsDataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent serviceIntent = new Intent(this, SMSBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }

        recyclerView = findViewById(R.id.recyclerView);
        smsDataList = new ArrayList<>();
        dbHelper = new SMSDatabaseHelper(this);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        smsAdapter = new SMSAdapter(smsDataList);
        recyclerView.setAdapter(smsAdapter);

        // Check for SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS}, PERMISSION_REQUEST_READ_SMS);
        } else {
            readAndStoreSMS();
        }
    }

    private void readAndStoreSMS() {
        // Read and store received SMS
        readAndStoreMessages("content://sms/inbox");

        // Read and store sent SMS
        readAndStoreMessages("content://sms/sent");

        // Display stored SMS in RecyclerView
        displayStoredSMS();
    }

    private void readAndStoreMessages(String uriString) {
        Uri uri = Uri.parse(uriString);
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String sender = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String message = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String formattedTimestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", Long.parseLong(timestamp)).toString();
                String type = "received"; // Modify this if needed (e.g., "sent" for outgoing messages)

                if (NetworkUtil.isOnline(this)) {
                    // If online, send message to the API immediately
                    JSONArray smsArray = new JSONArray();
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

                    // Attempt to send SMS to the API
                    new Thread(() -> {
                        SendSMSTask sendTask = new SendSMSTask(this);
                        if (sendTask.sendDataToServer(smsArray)) {
                            Log.d("SMS", "Message sent successfully: " + message);
                        } else {
                            Log.e("SMS", "Failed to send message, storing locally.");
                            dbHelper.insertSMS(sender, message, formattedTimestamp, type);
                        }
                    }).start();
                } else {
                    // If offline, store message in SQLite
                    dbHelper.insertSMS(sender, message, formattedTimestamp, type);
                }

                // Add message to RecyclerView
                smsDataList.add(new SMSData(sender, message, formattedTimestamp));
            }
            cursor.close();
        }
    }


    private void displayStoredSMS() {
        smsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_READ_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readAndStoreSMS();
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
