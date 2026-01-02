package com.example.sms;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

public class AutoStartHelper {

    private static final String PREF_NAME = "AutoStartPrefs";
    private static final String KEY_SHOWN = "auto_start_dialog_shown";

    /**
     * Check if the device manufacturer has auto-start restrictions
     */
    public static boolean isAutoStartPermissionAvailable(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        return manufacturer.contains("xiaomi") ||
                manufacturer.contains("redmi") ||
                manufacturer.contains("oppo") ||
                manufacturer.contains("vivo") ||
                manufacturer.contains("huawei") ||
                manufacturer.contains("honor") ||
                manufacturer.contains("asus") ||
                manufacturer.contains("samsung") ||
                manufacturer.contains("oneplus") ||
                manufacturer.contains("realme") ||
                manufacturer.contains("letv") ||
                manufacturer.contains("nokia") ||
                manufacturer.contains("tecno") ||
                manufacturer.contains("infinix");
    }

    /**
     * Get the intent to open manufacturer-specific auto-start settings
     */
    public static Intent getAutoStartSettingsIntent(Context context) {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Intent intent = null;

        try {
            if (manufacturer.contains("xiaomi") || manufacturer.contains("redmi")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
            } else if (manufacturer.contains("oppo")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"));
            } else if (manufacturer.contains("vivo")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"));
            } else if (manufacturer.contains("huawei") || manufacturer.contains("honor")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
            } else if (manufacturer.contains("samsung")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.samsung.android.lool",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"));
            } else if (manufacturer.contains("oneplus")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.oneplus.security",
                        "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"));
            } else if (manufacturer.contains("asus")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.asus.mobilemanager",
                        "com.asus.mobilemanager.autostart.AutoStartActivity"));
            } else if (manufacturer.contains("nokia")) {
                intent = new Intent();
                intent.setComponent(new ComponentName("com.evenwell.powersaving.g3",
                        "com.evenwell.powersaving.g3.exception.PowerSaverExceptionActivity"));
            } else if (manufacturer.contains("tecno") || manufacturer.contains("infinix")) {
                // Tecno and Infinix use similar settings
                intent = new Intent();
                intent.setComponent(new ComponentName("com.transsion.phonemanager",
                        "com.transsion.phonemanager.module.appmanage.autoboot.view.AutoBootActivity"));
            }
        } catch (Exception e) {
            Log.e("AutoStartHelper", "Error creating intent for manufacturer: " + manufacturer, e);
        }

        return intent;
    }

    /**
     * Check if auto-start dialog has been shown before
     */
    public static boolean hasShownDialog(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOWN, false);
    }

    /**
     * Mark that auto-start dialog has been shown
     */
    public static void markDialogShown(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SHOWN, true).apply();
    }

    /**
     * Show auto-start guidance dialog with callback
     */
    public static void showAutoStartDialog(Activity activity, Runnable onDismissCallback) {
        if (!isAutoStartPermissionAvailable(activity)) {
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
            return; // No need to show on devices without restrictions
        }

        if (hasShownDialog(activity)) {
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
            return; // Already shown
        }

        String manufacturer = Build.MANUFACTURER;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Enable Auto-Start");
        builder.setMessage("To ensure the app works properly in the background and starts automatically after device reboot, please enable Auto-Start permission.\n\n" +
                "Device: " + manufacturer + "\n\n" +
                "This is required for:\n" +
                "• Automatic SMS monitoring\n" +
                "• Background sync\n" +
                "• Starting on device boot");

        final Intent settingsIntent = getAutoStartSettingsIntent(activity);

        if (settingsIntent != null) {
            builder.setPositiveButton("Open Settings", (dialog, which) -> {
                try {
                    activity.startActivity(settingsIntent);
                    markDialogShown(activity);
                    if (onDismissCallback != null) {
                        onDismissCallback.run();
                    }
                } catch (Exception e) {
                    Log.e("AutoStartHelper", "Failed to open auto-start settings", e);
                    android.widget.Toast.makeText(activity,
                            "Could not open settings. Please enable Auto-Start manually in Settings.",
                            android.widget.Toast.LENGTH_LONG).show();
                    if (onDismissCallback != null) {
                        onDismissCallback.run();
                    }
                }
            });
        }

        builder.setNegativeButton("Later", (dialog, which) -> {
            markDialogShown(activity);
            dialog.dismiss();
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        });

        builder.setNeutralButton("Don't Show Again", (dialog, which) -> {
            markDialogShown(activity);
            dialog.dismiss();
            if (onDismissCallback != null) {
                onDismissCallback.run();
            }
        });

        builder.setCancelable(false);
        builder.show();
    }
}
