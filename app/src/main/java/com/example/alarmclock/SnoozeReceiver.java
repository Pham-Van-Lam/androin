package com.example.alarmclock;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class SnoozeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", 0);
        NotificationManagerCompat.from(context).cancel(alarmId);

        // Close AlarmActivity if open
        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(activityIntent); // This will recreate the activity, which can be closed by finishing it

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.putExtra("alarm_id", alarmId);
        snoozeIntent.putExtra("snooze", true);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarmId, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        long snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000; // 10 minutes
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
    }
}
