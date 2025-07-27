package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;

public class StopReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int alarmId = intent.getIntExtra("alarm_id", 0);
        NotificationManagerCompat.from(context).cancel(alarmId);

        // Close AlarmActivity if open
        Intent activityIntent = new Intent(context, AlarmActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

        // Đóng AlarmActivity nếu đang mở
        activityIntent.putExtra("finish", true);
        context.startActivity(activityIntent);
        // This will recreate the activity, which can be closed by finishing it
    }
}
