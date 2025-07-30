package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;

public class AlarmAdapter extends RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder> {
    private static final String TAG = "AlarmAdapter";
    private List<Alarm> alarms;
    private OnAlarmClickListener listener;
    private DatabaseHelper dbHelper;
    private Context context;

    public interface OnAlarmClickListener {
        void onAlarmClick(Alarm alarm);
        void onAlarmToggle(Alarm alarm, boolean enabled); // xu ly khi bat tat 1 bao thuc
    }

    public AlarmAdapter(List<Alarm> alarms, OnAlarmClickListener listener, Context context) {
        this.alarms = alarms;
        this.listener = listener;
        this.context = context;
        this.dbHelper = new DatabaseHelper(context);
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = alarms.get(position);
        bindAlarmData(holder, alarm);
        setupClickListeners(holder, alarm, position);
    }

    // Cập nhật giao diện cho từng báo thức.
    private void bindAlarmData(@NonNull AlarmViewHolder holder, Alarm alarm) {
        // Time display
        holder.timeText.setText(alarm.getFormattedTime12Hour());
        holder.amPmText.setText(alarm.getAmPm());

        // Days display
        holder.daysText.setText(alarm.getDaysString());

        // Alarm label
        if (!alarm.getLabel().isEmpty()) {
            holder.labelText.setText(alarm.getLabel());
            holder.labelText.setVisibility(View.VISIBLE);
        } else {
            holder.labelText.setVisibility(View.GONE);
        }

        // Next alarm info
        if (alarm.isEnabled() && alarm.getNextAlarmTime() > 0) {
            holder.nextAlarmText.setText(alarm.getTimeUntilNext());
            holder.nextAlarmText.setVisibility(View.VISIBLE);
        } else {
            holder.nextAlarmText.setVisibility(View.GONE);
        }

        // Snooze status
        if (alarm.isSnoozing()) {
            holder.snoozeLayout.setVisibility(View.VISIBLE);
            holder.snoozeText.setText("Snoozing for " + alarm.getSnoozeDuration() + " min");
        } else {
            holder.snoozeLayout.setVisibility(View.GONE);
        }

        // Switch state
        holder.enabledSwitch.setOnCheckedChangeListener(null); // Prevent unwanted triggers
        holder.enabledSwitch.setChecked(alarm.isEnabled());

        // Visual state based on enabled/disabled
        updateAlarmVisualState(holder, alarm.isEnabled());

        // Status indicator
        holder.statusIndicator.setActivated(alarm.isEnabled());

        // Alarm icon color based on state
        int iconColor = alarm.isEnabled() ?
                context.getResources().getColor(R.color.alarm_icon_color) :
                context.getResources().getColor(R.color.alarm_icon_color_disabled);
        holder.alarmIcon.setColorFilter(iconColor);
    }

    private void setupClickListeners(@NonNull AlarmViewHolder holder, Alarm alarm, int position) {
        // Switch toggle listener
        holder.enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Toggled alarm: ID=" + alarm.getId() + ", Enabled=" + isChecked);

            alarm.setEnabled(isChecked);
            updateAlarmVisualState(holder, isChecked);

            // Update database
            dbHelper.updateAlarm(alarm);

            // Schedule or cancel alarm
            if (isChecked) {
                scheduleAlarm(context, alarm);
                // Update next alarm time display
                alarm.setNextAlarmTime(alarm.calculateNextAlarmTime());
                holder.nextAlarmText.setText(alarm.getTimeUntilNext());
                holder.nextAlarmText.setVisibility(View.VISIBLE);
            } else {
                cancelAlarm(context, alarm.getId());
                holder.nextAlarmText.setVisibility(View.GONE);
            }

            // Update status indicator
            holder.statusIndicator.setActivated(isChecked);

            // Notify listener
            if (listener != null) {
                listener.onAlarmToggle(alarm, isChecked);
            }
        });

        // Card click listener for editing
        holder.cardView.setOnClickListener(v -> {
            Log.d(TAG, "Item clicked at position: " + position + ", Alarm ID: " + alarm.getId());
            if (listener != null) {
                listener.onAlarmClick(alarm);
            } else {
                Log.e(TAG, "OnAlarmClickListener is null");
            }
        });
    }

    private void updateAlarmVisualState(@NonNull AlarmViewHolder holder, boolean enabled) {
        // Update text colors
        int timeColor = enabled ?
                context.getResources().getColor(R.color.alarm_time_color) :
                context.getResources().getColor(R.color.alarm_time_color_disabled);
        int secondaryColor = enabled ?
                context.getResources().getColor(R.color.alarm_secondary_text) :
                context.getResources().getColor(R.color.alarm_secondary_text_disabled);

        holder.timeText.setTextColor(timeColor);
        holder.amPmText.setTextColor(secondaryColor);
        holder.daysText.setTextColor(secondaryColor);
        holder.labelText.setTextColor(context.getResources().getColor(R.color.alarm_label_color));

        // Update card background
        int cardColor = enabled ?
                context.getResources().getColor(R.color.alarm_card_background) :
                context.getResources().getColor(R.color.alarm_card_background_disabled);
        holder.cardView.setCardBackgroundColor(cardColor);

        // Update card alpha
        holder.cardView.setAlpha(enabled ? 1.0f : 0.7f);
    }

    @Override
    public int getItemCount() {
        return alarms.size();
    }

    // Alarm scheduling methods
    private void scheduleAlarm(Context context, Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);

        // Pass alarm data to receiver
        intent.putExtra("alarm_id", alarm.getId());
        intent.putExtra("alarm_hour", alarm.getHour());
        intent.putExtra("alarm_minute", alarm.getMinute());
        intent.putExtra("alarm_label", alarm.getLabel());
        intent.putExtra("snooze_enabled", alarm.isSnooze());
        intent.putExtra("snooze_duration", alarm.getSnoozeDuration());
        intent.putExtra("vibration_enabled", alarm.isVibrationEnabled());
        intent.putExtra("ringtone", alarm.getRingtone());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarm.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tính toán thời gian kích hoạt tiếp theo
        long triggerTime = alarm.calculateNextAlarmTime();
        alarm.setNextAlarmTime(triggerTime); // Cập nhật nextAlarmTime

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
            );
            Log.d(TAG, "Scheduled alarm: ID=" + alarm.getId() +
                    ", Time=" + alarm.getFormattedTime() +
                    ", TriggerTime=" + triggerTime);
        }
    }

    private void cancelAlarm(Context context, int alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled alarm: ID=" + alarmId);
        }
    }

    // Public methods for fragment interaction
    public void addAlarm(Alarm alarm) {
        alarms.add(alarm);
        notifyItemInserted(alarms.size() - 1);
    }

    public void updateAlarm(Alarm alarm) {
        int index = findAlarmIndex(alarm.getId());
        if (index != -1) {
            alarms.set(index, alarm);
            notifyItemChanged(index);
        }
    }

    public void removeAlarm(int position) {
        if (position >= 0 && position < alarms.size()) {
            Alarm alarm = alarms.get(position);
            cancelAlarm(context, alarm.getId());
            alarms.remove(position);
            notifyItemRemoved(position);
        }
    }

    private int findAlarmIndex(int alarmId) {
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).getId() == alarmId) {
                return i;
            }
        }
        return -1;
    }

    // ViewHolder class with modern UI components
    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView alarmIcon;
        TextView timeText;
        TextView amPmText;
        TextView daysText;
        TextView labelText;
        TextView nextAlarmText;
        LinearLayout snoozeLayout;
        TextView snoozeText;
        MaterialSwitch enabledSwitch;
        View statusIndicator;

        AlarmViewHolder(View itemView) {
            super(itemView);

            cardView = (CardView) itemView;
            alarmIcon = itemView.findViewById(R.id.icon_alarm);
            timeText = itemView.findViewById(R.id.text_time);
            amPmText = itemView.findViewById(R.id.text_ampm);
            daysText = itemView.findViewById(R.id.text_days);
            labelText = itemView.findViewById(R.id.text_label);
            nextAlarmText = itemView.findViewById(R.id.text_next_alarm);
            snoozeLayout = itemView.findViewById(R.id.layout_snooze);
            snoozeText = itemView.findViewById(R.id.text_snooze);
            enabledSwitch = itemView.findViewById(R.id.switch_enabled);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }
    }
}