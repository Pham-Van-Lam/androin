package com.example.alarmclock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "alarms.db";
    private static final int DATABASE_VERSION = 2; // Tăng version để upgrade
    private static final String TABLE_ALARMS = "alarms";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_HOUR = "hour";
    private static final String COLUMN_MINUTE = "minute";
    private static final String COLUMN_DAYS = "days";
    private static final String COLUMN_SNOOZE = "snooze";
    private static final String COLUMN_ENABLED = "enabled";
    private static final String COLUMN_SNOOZING = "is_snoozing"; // Thêm cột mới

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_ALARMS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_HOUR + " INTEGER, " +
                COLUMN_MINUTE + " INTEGER, " +
                COLUMN_DAYS + " TEXT, " +
                COLUMN_SNOOZE + " INTEGER, " +
                COLUMN_ENABLED + " INTEGER, " +
                COLUMN_SNOOZING + " INTEGER)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_ALARMS + " ADD COLUMN " + COLUMN_SNOOZING + " INTEGER DEFAULT 0");
        }
    }

    public long addAlarm(Alarm alarm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HOUR, alarm.getHour());
        values.put(COLUMN_MINUTE, alarm.getMinute());
        values.put(COLUMN_DAYS, listToString(alarm.getDaysOfWeek()));
        values.put(COLUMN_SNOOZE, alarm.isSnooze() ? 1 : 0);
        values.put(COLUMN_ENABLED, alarm.isEnabled() ? 1 : 0);
        values.put(COLUMN_SNOOZING, alarm.isSnoozing() ? 1 : 0);
        return db.insert(TABLE_ALARMS, null, values);
    }

    public void updateAlarm(Alarm alarm) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_HOUR, alarm.getHour());
        values.put(COLUMN_MINUTE, alarm.getMinute());
        values.put(COLUMN_DAYS, listToString(alarm.getDaysOfWeek()));
        values.put(COLUMN_SNOOZE, alarm.isSnooze() ? 1 : 0);
        values.put(COLUMN_ENABLED, alarm.isEnabled() ? 1 : 0);
        values.put(COLUMN_SNOOZING, alarm.isSnoozing() ? 1 : 0);
        db.update(TABLE_ALARMS, values, COLUMN_ID + "=?", new String[]{String.valueOf(alarm.getId())});
    }

    public void deleteAlarm(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ALARMS, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public List<Alarm> getAllAlarms() {
        List<Alarm> alarms = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_ALARMS, null, null, null, null, null, null);
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
            int hour = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_HOUR));
            int minute = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_MINUTE));
            String daysStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAYS));
            boolean snooze = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SNOOZE)) == 1;
            boolean enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ENABLED)) == 1;
            boolean isSnoozing = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SNOOZING)) == 1;
            List<Integer> days = stringToList(daysStr);
            Alarm alarm = new Alarm(id, hour, minute, days, snooze, enabled);
            alarm.setSnoozing(isSnoozing);
            alarms.add(alarm);
        }
        cursor.close();
        return alarms;
    }

    private String listToString(List<Integer> days) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < days.size(); i++) {
            sb.append(days.get(i));
            if (i < days.size() - 1) sb.append(",");
        }
        return sb.toString();
    }

    private List<Integer> stringToList(String daysStr) {
        List<Integer> days = new ArrayList<>();
        if (daysStr != null && !daysStr.isEmpty()) {
            String[] parts = daysStr.split(",");
            for (String part : parts) {
                days.add(Integer.parseInt(part.trim()));
            }
        }
        return days;
    }
}