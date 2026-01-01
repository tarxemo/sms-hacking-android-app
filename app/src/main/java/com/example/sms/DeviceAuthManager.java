package com.example.sms;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

public class DeviceAuthManager {
    private static final String PREF_NAME = "device_auth";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD_HASH = "password_hash";
    private static final String KEY_REGISTERED = "is_registered";
    private static final String KEY_INITIAL_SYNC = "initial_sync_done";
    private static final String KEY_LAST_SYNC_TS = "last_sync_timestamp";
    private static boolean isUnlocked = false;

    private SharedPreferences prefs;
    private Context context;

    public DeviceAuthManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveCredentials(String username, String password) {
        prefs.edit()
                .putString(KEY_USERNAME, username)
                .putString(KEY_PASSWORD_HASH, hashPassword(password))
                .apply();
    }

    public boolean checkOfflineLogin(String username, String password) {
        String savedUsername = prefs.getString(KEY_USERNAME, "");
        String savedHash = prefs.getString(KEY_PASSWORD_HASH, "");
        return username.equals(savedUsername) && !savedHash.isEmpty() && hashPassword(password).equals(savedHash);
    }

    public void setSessionUnlocked(boolean unlocked) {
        isUnlocked = unlocked;
    }

    public boolean isSessionUnlocked() {
        return isUnlocked;
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
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
