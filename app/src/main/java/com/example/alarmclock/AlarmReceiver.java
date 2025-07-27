package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", 0);
        boolean snooze = intent.getBooleanExtra("snooze", false);

        // Launch full-screen AlarmActivity
        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.putExtra("alarm_id", alarmId);
        activityIntent.putExtra("snooze", snooze);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(activityIntent);

        // Optionally show notification as fallback
        if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "alarm_channel")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Alarm")
                    .setContentText("Time to wake up!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            if (snooze) {
                Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
                snoozeIntent.putExtra("alarm_id", alarmId);
                PendingIntent snoozePendingIntent = PendingIntent.getActivity(context, alarmId + 1000, snoozeIntent, PendingIntent.FLAG_IMMUTABLE);
                builder.addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent);
            }

            Intent stopIntent = new Intent(context, StopReceiver.class);
            stopIntent.putExtra("alarm_id", alarmId);
            PendingIntent stopPendingIntent = PendingIntent.getActivity(context, alarmId + 2000, stopIntent, PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent);


            try {
                NotificationManagerCompat.from(context).notify(alarmId, builder.build());
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to send notification due to missing permission", e);
            }
        } else {
            Log.w(TAG, "POST_NOTIFICATIONS permission not granted, notification skipped");
        }

        // Reschedule if repeating
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        Alarm alarm = dbHelper.getAllAlarms().stream()
                .filter(a -> a.getId() == alarmId)
                .findFirst()
                .orElse(null);

        // Chỉ lên lịch lại nếu đây là báo thức lặp lại và KHÔNG phải snooze
        if (alarm != null && !alarm.getDaysOfWeek().isEmpty() && !snooze) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent newIntent = new Intent(context, AlarmReceiver.class);
            newIntent.putExtra("alarm_id", alarm.getId());
            newIntent.putExtra("snooze", false);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarm.getId(),
                    newIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            long triggerTime = alarm.calculateNextAlarmTime(); // Tính toán lại thời gian
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
        }

    }
}