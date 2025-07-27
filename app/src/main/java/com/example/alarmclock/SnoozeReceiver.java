package com.example.alarmclock;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

public class SnoozeReceiver extends BroadcastReceiver {
    private static final String TAG = "SnoozeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", 0);
        String ringtone = intent.getStringExtra("ringtone");

        // Cập nhật trạng thái báo thức
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        Alarm alarm = dbHelper.getAllAlarms().stream()
                .filter(a -> a.getId() == alarmId)
                .findFirst()
                .orElse(null);

        if (alarm != null) {
            alarm.setSnoozing(true);
            alarm.setEnabled(true);
            dbHelper.updateAlarm(alarm);
            Log.d(TAG, "Updated alarm to snoozing in SnoozeReceiver: ID=" + alarmId);
        }

        // Lên lịch báo thức lại sau 10 phút
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.putExtra("alarm_id", alarmId);
        snoozeIntent.putExtra("snooze", true);
        snoozeIntent.putExtra("ringtone", ringtone);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        long snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000; // 10 phút
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);

        // Hủy thông báo
        NotificationManagerCompat.from(context).cancel(alarmId);

        // Đóng AlarmActivity nếu đang mở
        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activityIntent.putExtra("finish", true);
        context.startActivity(activityIntent);

        Log.d(TAG, "Snoozed alarm: ID=" + alarmId);
    }
}
