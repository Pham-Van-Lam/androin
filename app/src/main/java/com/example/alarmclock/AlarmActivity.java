package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

public class AlarmActivity extends AppCompatActivity {

    private int alarmId;
    private boolean snooze;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().getBooleanExtra("finish", false)) {
            finish();
            return;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        // Allow activity to show over lock screen and turn on screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Get alarm details from intent
        alarmId = getIntent().getIntExtra("alarm_id", 0);
        snooze = getIntent().getBooleanExtra("snooze", false);

        Button snoozeButton = findViewById(R.id.button_snooze);
        Button stopButton = findViewById(R.id.button_stop);

        // Snooze button
        snoozeButton.setEnabled(snooze);
        snoozeButton.setOnClickListener(v -> {
            if (snooze) {
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent snoozeIntent = new Intent(this, AlarmReceiver.class);
                snoozeIntent.putExtra("alarm_id", alarmId);
                snoozeIntent.putExtra("snooze", true);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmId, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                long snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000; // 10 minutes
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
            }
            finish();
        });

        // Stop button
        stopButton.setOnClickListener(v -> {
            // Hủy notification
            NotificationManagerCompat.from(this).cancel(alarmId);

            // Hủy báo thức trong AlarmManager
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.putExtra("alarm_id", alarmId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    alarmId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);  // ← dòng này là quan trọng nhất

            finish();
        });

    }
}