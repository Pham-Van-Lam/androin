package com.example.alarmclock;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

public class Alarm implements Serializable {
    private int id;
    private int hour;
    private int minute;
    private List<Integer> daysOfWeek;
    private boolean snooze;
    private boolean enabled;
    private String label;
    private boolean vibrationEnabled;
    private int snoozeDuration; // in minutes
    private String ringtone;
    private boolean isSnoozing;
    private long nextAlarmTime; // timestamp for next alarm

    // Constructor for backward compatibility
    public Alarm(int id, int hour, int minute, List<Integer> daysOfWeek, boolean snooze, boolean enabled) {
        this(id, hour, minute, daysOfWeek, snooze, enabled, "", true, 10);
    }

    // Full constructor with all features
    public Alarm(int id, int hour, int minute, List<Integer> daysOfWeek, boolean snooze,
                 boolean enabled, String label, boolean vibrationEnabled, int snoozeDuration) {
        this.id = id;
        this.hour = hour;
        this.minute = minute;
        this.daysOfWeek = daysOfWeek != null ? new ArrayList<>(daysOfWeek) : new ArrayList<>();
        this.snooze = snooze;
        this.enabled = enabled;
        this.label = label != null ? label : "";
        this.vibrationEnabled = vibrationEnabled;
        this.snoozeDuration = snoozeDuration;
        this.ringtone = "default";
        this.isSnoozing = false;
        this.nextAlarmTime = 0;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public List<Integer> getDaysOfWeek() {
        return new ArrayList<>(daysOfWeek);
    }

    public boolean isSnooze() {
        return snooze;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getLabel() {
        return label;
    }

    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    public int getSnoozeDuration() {
        return snoozeDuration;
    }

    public String getRingtone() {
        return ringtone;
    }

    public boolean isSnoozing() {
        return isSnoozing;
    }

    public long getNextAlarmTime() {
        return nextAlarmTime;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setDaysOfWeek(List<Integer> daysOfWeek) {
        this.daysOfWeek = daysOfWeek != null ? new ArrayList<>(daysOfWeek) : new ArrayList<>();
    }

    public void setSnooze(boolean snooze) {
        this.snooze = snooze;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setLabel(String label) {
        this.label = label != null ? label : "";
    }

    public void setVibrationEnabled(boolean vibrationEnabled) {
        this.vibrationEnabled = vibrationEnabled;
    }

    public void setSnoozeDuration(int snoozeDuration) {
        this.snoozeDuration = snoozeDuration;
    }

    public void setRingtone(String ringtone) {
        this.ringtone = ringtone != null ? ringtone : "default";
    }

    public void setSnoozing(boolean snoozing) {
        this.isSnoozing = snoozing;
    }

    public void setNextAlarmTime(long nextAlarmTime) {
        this.nextAlarmTime = nextAlarmTime;
    }

    // Helper methods
    public String getFormattedTime() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String getAmPm() {
        return hour >= 12 ? "PM" : "AM";
    }

    public String getFormattedTime12Hour() {
        int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
        return String.format("%d:%02d", displayHour, minute);
    }

    public String getDaysString() {
        if (daysOfWeek.isEmpty()) {
            return "Once";
        }

        if (daysOfWeek.size() == 7) {
            return "Daily";
        }

        // Check for weekdays (Mon-Fri)
        if (daysOfWeek.size() == 5 &&
                daysOfWeek.contains(2) && daysOfWeek.contains(3) &&
                daysOfWeek.contains(4) && daysOfWeek.contains(5) &&
                daysOfWeek.contains(6)) {
            return "Weekdays";
        }

        // Build custom string
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        StringBuilder sb = new StringBuilder();

        for (int i = 1; i <= 7; i++) {
            if (daysOfWeek.contains(i)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(dayNames[i - 1]);
            }
        }

        return sb.toString();
    }

    public boolean isRepeating() {
        return !daysOfWeek.isEmpty();
    }

    public boolean shouldTriggerToday(int dayOfWeek) {
        return daysOfWeek.contains(dayOfWeek);
    }

    // Calculate next alarm time
    public long calculateNextAlarmTime() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        java.util.Calendar now = java.util.Calendar.getInstance();

        // Đặt thời gian báo thức
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour);
        calendar.set(java.util.Calendar.MINUTE, minute);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        // Lấy ngày hiện tại (1=Chủ nhật, 2=Thứ hai, ..., 7=Thứ bảy)
        int currentDayOfWeek = now.get(java.util.Calendar.DAY_OF_WEEK);

        // Nếu không có ngày nào được chọn (báo thức chạy một lần)
        if (daysOfWeek.isEmpty()) {
            // Nếu thời gian đã qua trong ngày hiện tại, chuyển sang ngày tiếp theo
            if (calendar.before(now)) {
                calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
            }
            return calendar.getTimeInMillis();
        }

        // Tìm ngày tiếp theo trong danh sách daysOfWeek
        int daysToAdd = 7; // Tối đa 7 ngày để tìm ngày tiếp theo
        for (int i = 0; i < 7; i++) {
            int nextDay = (currentDayOfWeek + i - 1) % 7 + 1; // Chuyển đổi sang 1-7
            if (daysOfWeek.contains(nextDay)) {
                // Nếu là ngày hiện tại, kiểm tra xem thời gian có còn hợp lệ không
                if (i == 0 && calendar.after(now)) {
                    daysToAdd = 0; // Dùng ngay ngày hiện tại
                    break;
                } else if (i > 0) {
                    daysToAdd = i; // Chọn ngày tiếp theo phù hợp
                    break;
                }
            }
        }

        // Thêm số ngày cần thiết
        calendar.add(java.util.Calendar.DAY_OF_MONTH, daysToAdd);

        return calendar.getTimeInMillis();
    }

    public String getTimeUntilNext() {
        if (nextAlarmTime == 0) {
            return "";
        }

        long currentTime = System.currentTimeMillis();
        long timeDiff = nextAlarmTime - currentTime;

        if (timeDiff <= 0) {
            return "Now";
        }

        long days = timeDiff / (1000 * 60 * 60 * 24);
        long hours = (timeDiff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (timeDiff % (1000 * 60 * 60)) / (1000 * 60);

        if(days > 0){
            return String.format("Rings in %dd %dh %dm", days, hours, minutes);
        }
        else if (hours > 0) {
            return String.format("Rings in %dh %dm", hours, minutes);
        } else {
            return String.format("Rings in %dm", minutes);
        }
    }

    @Override
    public String toString() {
        return "Alarm{" +
                "id=" + id +
                ", time=" + getFormattedTime() +
                ", days=" + getDaysString() +
                ", enabled=" + enabled +
                ", label='" + label + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Alarm alarm = (Alarm) obj;
        return id == alarm.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}