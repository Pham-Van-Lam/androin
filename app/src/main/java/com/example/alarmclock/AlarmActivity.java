package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

public class AlarmActivity extends AppCompatActivity {

    private int alarmId;
    private boolean snooze;
    private Ringtone ringtone; // Thêm biến để quản lý âm thanh
    private Handler handler;
    private Runnable autoSnoozeRunnable;
    private static final long AUTO_SNOOZE_DELAY = 180 * 1000; // 1 phút
    private static final long AUTO_SNOOZE_TIME = 10 * 60 * 1000; // 10 phút

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
            setShowWhenLocked(true); //  hiển thị trên màn hình khóa
            setTurnScreenOn(true); //  bật sáng màn hình
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        // Get alarm details from intent
        alarmId = getIntent().getIntExtra("alarm_id", 0);
        snooze = getIntent().getBooleanExtra("snooze", false);

        String ringtoneUri = getIntent().getStringExtra("ringtone"); // Lấy URI ringtone

        // Phát âm thanh báo thức
        playAlarmSound(ringtoneUri);

        // Thiết lập Handler để tự động snooze sau 1 phút
        handler = new Handler(Looper.getMainLooper());
        autoSnoozeRunnable = () -> {
            // Lên lịch báo thức lại sau 10 phút
            scheduleAutoSnooze();
            stopAlarmSound();
            finish();
        };
        handler.postDelayed(autoSnoozeRunnable, AUTO_SNOOZE_DELAY);

        Button snoozeButton = findViewById(R.id.button_snooze);
        Button stopButton = findViewById(R.id.button_stop);

        // Snooze button
        snoozeButton.setEnabled(!snooze);
        snoozeButton.setOnClickListener(v -> {
            handler.removeCallbacks(autoSnoozeRunnable); // Hủy đếm ngược auto-snooze

            // Cập nhật trạng thái báo thức
            DatabaseHelper dbHelper = new DatabaseHelper(AlarmActivity.this);
            Alarm alarm = dbHelper.getAllAlarms().stream()
                    .filter(a -> a.getId() == alarmId)
                    .findFirst()
                    .orElse(null);

            if (alarm != null) {
                alarm.setSnoozing(true); // Đặt trạng thái snooze
                alarm.setEnabled(true); // Đảm bảo báo thức vẫn bật
                dbHelper.updateAlarm(alarm); // Cập nhật vào cơ sở dữ liệu
                Log.d("AlarmActivity", "Set alarm to snoozing: ID=" + alarmId);
            }

            // Gửi Intent tới SnoozeReceiver
            Intent snoozeIntent = new Intent(AlarmActivity.this, SnoozeReceiver.class);
            snoozeIntent.putExtra("alarm_id", alarmId);
            snoozeIntent.putExtra("ringtone", getIntent().getStringExtra("ringtone"));
            sendBroadcast(snoozeIntent);

            stopAlarmSound(); // Dừng âm thanh
            finish(); // Đóng Activity
        });

        // Stop button
        stopButton.setOnClickListener(v -> {
            handler.removeCallbacks(autoSnoozeRunnable); // Hủy đếm ngược
            DatabaseHelper dbHelper = new DatabaseHelper(AlarmActivity.this);
            Alarm alarm = dbHelper.getAllAlarms().stream()
                    .filter(a -> a.getId() == alarmId)
                    .findFirst()
                    .orElse(null);

            if (alarm != null && alarm.getDaysOfWeek().isEmpty()) {
                alarm.setEnabled(false); // Tắt báo thức một lần
                alarm.setNextAlarmTime(0);
                dbHelper.updateAlarm(alarm); // Cập nhật vào cơ sở dữ liệu
            }

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
            stopAlarmSound(); // Dừng âm thanh khi snooze
            finish();
        });

    }
    private void playAlarmSound(String ringtoneUri) {
        try {
            // Nếu không có ringtone được chỉ định, sử dụng âm thanh báo thức mặc định
            Uri alarmUri = ringtoneUri != null && !ringtoneUri.equals("default") ?
                    Uri.parse(ringtoneUri) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                // Fallback nếu không có âm thanh báo thức mặc định
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) {
                ringtone.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void scheduleAutoSnooze() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Alarm alarm = dbHelper.getAllAlarms().stream()
                .filter(a -> a.getId() == alarmId)
                .findFirst()
                .orElse(null);

        if (alarm != null) {
            alarm.setSnoozing(true); // Đặt trạng thái snooze
            alarm.setEnabled(true); // Đảm bảo báo thức vẫn bật
            dbHelper.updateAlarm(alarm); // Cập nhật vào cơ sở dữ liệu
            Log.d("AlarmActivity", "Set alarm to snoozing: ID=" + alarmId);

            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent snoozeIntent = new Intent(this, AlarmReceiver.class);
            snoozeIntent.putExtra("alarm_id", alarmId);
            snoozeIntent.putExtra("snooze", true);
            snoozeIntent.putExtra("ringtone", getIntent().getStringExtra("ringtone"));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, alarmId, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            long snoozeTime = System.currentTimeMillis() + AUTO_SNOOZE_TIME; // 10 phút
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pendingIntent);
            Log.d("AlarmActivity", "Scheduled auto-snooze for alarm ID=" + alarmId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(autoSnoozeRunnable);
        stopAlarmSound(); // Đảm bảo dừng âm thanh khi activity bị hủy
    }
}