package com.example.sms;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class SendSMSTask extends AsyncTask<Void, Void, Void> {

    private static final String API_URL = "http://172.16.130.67:8000/sms/api/";
    private Context context;
    private SMSDatabaseHelper dbHelper;

    public SendSMSTask(Context context) {
        this.context = context;
        this.dbHelper = new SMSDatabaseHelper(context);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        if (!NetworkUtil.isOnline(context)) {
            Log.d("SendSMSTask", "Device is offline, storing SMS locally.");
            return null;
        }

        Cursor cursor = dbHelper.getAllSMS();
        if (cursor != null && cursor.moveToFirst()) {
            JSONArray smsArray = new JSONArray();
            do {
                JSONObject smsObject = new JSONObject();
                try {
                    smsObject.put("sender", cursor.getString(cursor.getColumnIndexOrThrow("sender")));
                    smsObject.put("message", cursor.getString(cursor.getColumnIndexOrThrow("message")));
                    smsObject.put("timestamp", cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
                    smsObject.put("type", cursor.getString(cursor.getColumnIndexOrThrow("type")));
                    smsArray.put(smsObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());

            cursor.close();

            if (sendDataToServer(smsArray)) {
                deleteSyncedMessages();
            }
        }
        return null;
    }

    /**
     * Sends SMS data to the Django API.
     * @param smsArray JSON Array containing SMS messages.
     * @return true if successful, false otherwise.
     */
    public boolean sendDataToServer(JSONArray smsArray) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(smsArray.toString().getBytes());
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == 201) {
                Log.d("SendSMSTask", "SMS successfully sent to server.");
                return true;
            } else {
                Log.e("SendSMSTask", "Failed to send SMS, response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Deletes messages from SQLite after successful sync.
     */
    private void deleteSyncedMessages() {
        Cursor cursor = dbHelper.getAllSMS();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"));
                dbHelper.deleteSMS(timestamp);
            } while (cursor.moveToNext());
            cursor.close();
        }
    }
}
