package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
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
        String ringtone = intent.getStringExtra("ringtone");

        // Launch full-screen AlarmActivity
        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.putExtra("alarm_id", alarmId);
        activityIntent.putExtra("snooze", snooze);
        activityIntent.putExtra("ringtone", ringtone);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(activityIntent);

        // Optionally show notification as fallback
        if (ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") == PackageManager.PERMISSION_GRANTED) {
            Uri alarmUri = ringtone != null && !ringtone.equals("default") ?
                    Uri.parse(ringtone) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "alarm_channel")
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Alarm")
                    .setContentText("Time to wake up!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(alarmUri)
                    .setAutoCancel(true);

            // Sửa lỗi: Sử dụng Broadcast thay vì Activity cho PendingIntent
            Intent snoozeIntent = new Intent(context, SnoozeReceiver.class);
            snoozeIntent.putExtra("alarm_id", alarmId);
            snoozeIntent.putExtra("ringtone", ringtone);
            PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId + 1000,
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            builder.addAction(android.R.drawable.ic_menu_recent_history, "Snooze", snoozePendingIntent);

            Intent stopIntent = new Intent(context, StopReceiver.class);
            stopIntent.putExtra("alarm_id", alarmId);
            PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                    context,
                    alarmId + 2000,
                    stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
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

        if (alarm != null) {
            if (snooze) {
                // Nếu là snooze, giữ enabled = true và cập nhật isSnoozing
                alarm.setSnoozing(true);
                alarm.setEnabled(true);
                dbHelper.updateAlarm(alarm);
                Log.d(TAG, "Updated alarm to snoozing: ID=" + alarm.getId());
            } else if (!alarm.getDaysOfWeek().isEmpty()) {
                // Lên lịch lại cho báo thức lặp lại
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent newIntent = new Intent(context, AlarmReceiver.class);
                newIntent.putExtra("alarm_id", alarm.getId());
                newIntent.putExtra("snooze", false);
                newIntent.putExtra("ringtone", alarm.getRingtone());
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        alarm.getId(),
                        newIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                long triggerTime = alarm.calculateNextAlarmTime();
                alarm.setNextAlarmTime(triggerTime);
                alarm.setSnoozing(false); // Reset snooze status
                dbHelper.updateAlarm(alarm);
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
                Log.d(TAG, "Rescheduled repeating alarm: ID=" + alarm.getId());
            }
            // Không đặt enabled = false cho báo thức một lần ở đây, để lại cho StopReceiver
        }
    }
}