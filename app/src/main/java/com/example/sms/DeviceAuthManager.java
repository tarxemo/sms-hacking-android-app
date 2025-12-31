package com.example.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

public class DeviceAuthManager {
    private static final String PREF_NAME = "device_auth";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_REGISTERED = "is_registered";
    private static final String KEY_INITIAL_SYNC = "initial_sync_done";
    private static final String KEY_LAST_SYNC_TS = "last_sync_timestamp";

    private SharedPreferences prefs;
    private Context context;

    public DeviceAuthManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getDeviceId() {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public String getDeviceName() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void setRegistered(boolean registered) {
        prefs.edit().putBoolean(KEY_REGISTERED, registered).apply();
    }

    public boolean isRegistered() {
        return prefs.getBoolean(KEY_REGISTERED, false);
    }

    public void setInitialSyncDone(boolean done) {
        prefs.edit().putBoolean(KEY_INITIAL_SYNC, done).apply();
    }

    public boolean isInitialSyncDone() {
        return prefs.getBoolean(KEY_INITIAL_SYNC, false);
    }

    public void setLastSyncTimestamp(long timestamp) {
        prefs.edit().putLong(KEY_LAST_SYNC_TS, timestamp).apply();
    }

    public long getLastSyncTimestamp() {
        return prefs.getLong(KEY_LAST_SYNC_TS, 0);
    }
}
