package com.example.sms;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SendSMSTask extends AsyncTask<Void, Void, Void> {

    private static final String API_URL = "https://api.tarxemo.com/api/sms/sync/";
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
                    smsObject.put("sms_type", cursor.getString(cursor.getColumnIndexOrThrow("type")));
                    smsArray.put(smsObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } while (cursor.moveToNext());

            cursor.close();

            JSONObject payload = new JSONObject();
            try {
                payload.put("device_id", getDeviceId(context));
                payload.put("sms_list", smsArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (sendDataToServer(payload)) {
                deleteSyncedMessages();
            }
        }
        return null;
    }

    private String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    /**
     * Sends SMS data to the Django API.
     * @param payload JSON Object containing device_id and sms_list.
     * @return true if successful, false otherwise.
     */
    public boolean sendDataToServer(JSONObject payload) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            
            DeviceAuthManager authManager = new DeviceAuthManager(context);
            String token = authManager.getToken();
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes());
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
