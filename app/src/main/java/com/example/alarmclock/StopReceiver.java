package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

public class StopReceiver extends BroadcastReceiver {


    private static final String TAG = "StopReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", 0);
        NotificationManagerCompat.from(context).cancel(alarmId);

        // Hủy báo thức trong AlarmManager
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent cancelIntent = new Intent(context, AlarmReceiver.class);
        cancelIntent.putExtra("alarm_id", alarmId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);

        // Cập nhật trạng thái báo thức
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        Alarm alarm = dbHelper.getAllAlarms().stream()
                .filter(a -> a.getId() == alarmId)
                .findFirst()
                .orElse(null);
        if (alarm != null) {
            alarm.setSnoozing(false); // Reset trạng thái snooze
            if (alarm.getDaysOfWeek().isEmpty()) {
                alarm.setEnabled(false); // Tắt báo thức một lần
                alarm.setNextAlarmTime(0);
            }
            dbHelper.updateAlarm(alarm);
            Log.d(TAG, "Stopped alarm: ID=" + alarmId + ", Enabled=" + alarm.isEnabled());
        }

        // Đóng AlarmActivity nếu đang mở
        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activityIntent.putExtra("finish", true);
        context.startActivity(activityIntent);
    }
}
