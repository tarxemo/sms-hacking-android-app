package com.example.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getOriginatingAddress();
                        String message = smsMessage.getMessageBody();
                        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                        Log.d("SMSReceiver", "Received SMS from: " + sender + ", Message: " + message);

                        SMSDatabaseHelper dbHelper = new SMSDatabaseHelper(context);

                        if (NetworkUtil.isOnline(context)) {
                            // If online, send immediately
                            new SendSMSTask(context).execute();
                        } else {
                            // If offline, store in local database
                            dbHelper.insertSMS(sender, message, timestamp, "received");
                        }
                    }
                }
            }
        }
    }
}
