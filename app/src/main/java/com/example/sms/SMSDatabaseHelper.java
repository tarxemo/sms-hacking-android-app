package com.example.sms;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SMSDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "sms_db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_SMS = "sms";

    // Column names
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SENDER = "sender";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TYPE = "type"; // "received" or "sent"

    public SMSDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_SMS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SENDER + " TEXT, " +
                COLUMN_MESSAGE + " TEXT, " +
                COLUMN_TIMESTAMP + " TEXT, " +
                COLUMN_TYPE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMS);
        onCreate(db);
    }

    /**
     * Inserts an SMS record into the database.
     * @param sender The sender's phone number (or recipient if sent).
     * @param message The SMS content.
     * @param timestamp The timestamp of the message.
     * @param type "received" or "sent".
     */
    public void insertSMS(String sender, String message, String timestamp, String type) {
        if (isExists(sender, message, timestamp)) {
            return;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SENDER, sender);
        values.put(COLUMN_MESSAGE, message);
        values.put(COLUMN_TIMESTAMP, timestamp);
        values.put(COLUMN_TYPE, type);

        db.insert(TABLE_SMS, null, values);
        // Don't close db here - managed by SQLiteOpenHelper
    }

    private boolean isExists(String sender, String message, String timestamp) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM " + TABLE_SMS + 
            " WHERE " + COLUMN_SENDER + "=? AND " + COLUMN_MESSAGE + "=? AND " + COLUMN_TIMESTAMP + "=?",
            new String[]{sender, message, timestamp});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        // Don't close db here - managed by SQLiteOpenHelper
        return exists;
    }

    /**
     * Fetches all SMS messages (both received and sent) sorted by timestamp (latest first).
     * @return Cursor containing all messages.
     */
    public Cursor getAllSMS() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_SMS + " ORDER BY " + COLUMN_TIMESTAMP + " DESC", null);
    }

    /**
     * Fetches only received SMS messages.
     * @return Cursor containing received messages.
     */
    public Cursor getReceivedSMS() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_SMS + " WHERE " + COLUMN_TYPE + " = 'received' ORDER BY " + COLUMN_TIMESTAMP + " DESC", null);
    }

    /**
     * Fetches only sent SMS messages.
     * @return Cursor containing sent messages.
     */
    public Cursor getSentSMS() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_SMS + " WHERE " + COLUMN_TYPE + " = 'sent' ORDER BY " + COLUMN_TIMESTAMP + " DESC", null);
    }

    /**
     * Deletes an SMS record after it is successfully sent to the Django API.
     * @param timestamp The timestamp of the SMS message to delete.
     */
    public void deleteSMS(String timestamp) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SMS, COLUMN_TIMESTAMP + " = ?", new String[]{timestamp});
        // Don't close db here - managed by SQLiteOpenHelper
    }
}
