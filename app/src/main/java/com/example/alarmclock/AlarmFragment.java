package com.example.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AlarmFragment extends Fragment implements AlarmAdapter.OnAlarmClickListener {
    private static final String TAG = "AlarmFragment";

    private RecyclerView recyclerView;
    private AlarmAdapter alarmAdapter;
    private DatabaseHelper dbHelper;
    private List<Alarm> alarmList;
    //private TextView emptyStateText;
    private FloatingActionButton fab;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarm, container, false);

        initializeViews(view);
        setupRecyclerView();
        setupFloatingActionButton();
        loadAlarms();

        return view;
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view_alarms);
        fab = view.findViewById(R.id.fab_add_alarm);
        //emptyStateText = view.findViewById(R.id.text_label);

        dbHelper = new DatabaseHelper(getContext());
    }

    // xoa khi vuot
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup swipe to delete with modern UI feedback
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback());
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void setupFloatingActionButton() {
        fab.setOnClickListener(v -> {
            Log.d(TAG, "FAB clicked to add new alarm");
            showAlarmDialog(null);
        });
    }

    // Tải và hiển thị danh sách báo thức
    private void loadAlarms() {
        alarmList = dbHelper.getAllAlarms();

        // Calculate next alarm times for enabled alarms
        for (Alarm alarm : alarmList) {
            if (alarm.isEnabled()) {
                alarm.setNextAlarmTime(alarm.calculateNextAlarmTime());
            }
        }

        alarmAdapter = new AlarmAdapter(alarmList, this, getContext());
        recyclerView.setAdapter(alarmAdapter);

        updateEmptyState();
    }

    // an hien danh sach bao thuc
    private void updateEmptyState() {
        if (alarmList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            //emptyStateText.setVisibility(View.VISIBLE);
            //emptyStateText.setText("No alarms set\nTap + to add your first alarm");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            //emptyStateText.setVisibility(View.GONE);
        }
    }

    private void showAlarmDialog(Alarm alarm) {
        AlarmDialogFragment dialog = AlarmDialogFragment.newInstance(alarm);
        dialog.setOnAlarmSetListener(alarm == null ? this::addAlarm : this::updateAlarm);

        try {
            String tag = alarm == null ? "add_alarm" : "edit_alarm";
            dialog.show(getParentFragmentManager(), tag);
        } catch (Exception e) {
            Log.e(TAG, "Failed to show AlarmDialogFragment", e);
        }
    }

    // AlarmAdapter.OnAlarmClickListener implementation
    @Override
    public void onAlarmClick(Alarm alarm) {
        Log.d(TAG, "Alarm clicked: ID=" + alarm.getId() + ", Time=" + alarm.getFormattedTime());
        showAlarmDialog(alarm);
    }

    @Override
    public void onAlarmToggle(Alarm alarm, boolean enabled) {
        Log.d(TAG, "Alarm toggled: ID=" + alarm.getId() + ", Enabled=" + enabled);

        if (enabled) {
            alarm.setNextAlarmTime(alarm.calculateNextAlarmTime()); // Tính toán lại thời gian
            scheduleAlarm(alarm);
        } else {
            cancelAlarm(alarm.getId());
            alarm.setNextAlarmTime(0); // Reset nextAlarmTime khi tắt
        }

        // Show feedback to user
        String message = enabled ?
                "Alarm set for " + alarm.getTimeUntilNext() :
                "Alarm disabled";

        Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show();
    }

    // Alarm management methods
    private void addAlarm(Alarm alarm) {
        try {
            long id = dbHelper.addAlarm(alarm);
            alarm.setId((int) id);

            if (alarm.isEnabled()) {
                alarm.setNextAlarmTime(alarm.calculateNextAlarmTime());
                scheduleAlarm(alarm);
            }

            alarmAdapter.addAlarm(alarm);
            updateEmptyState();

            // Scroll to new alarm
            recyclerView.smoothScrollToPosition(alarmList.size() - 1);

            // Show success message
            String message = "Alarm " + (alarm.isEnabled() ? "set for " + alarm.getTimeUntilNext() : "created");
            Snackbar.make(recyclerView, message, Snackbar.LENGTH_LONG).show();

            Log.d(TAG, "Added alarm: ID=" + alarm.getId() + ", Time=" + alarm.getFormattedTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed to add alarm", e);
            Snackbar.make(recyclerView, "Failed to create alarm", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void updateAlarm(Alarm alarm) {
        try {
            dbHelper.updateAlarm(alarm);

            if (alarm.isEnabled()) {
                alarm.setNextAlarmTime(alarm.calculateNextAlarmTime());
                scheduleAlarm(alarm);
            } else {
                cancelAlarm(alarm.getId());
            }

            alarmAdapter.updateAlarm(alarm);

            // Show success message
            String message = "Alarm " + (alarm.isEnabled() ? "updated" : "disabled");
            Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show();

            Log.d(TAG, "Updated alarm: ID=" + alarm.getId() + ", Time=" + alarm.getFormattedTime());
        } catch (Exception e) {
            Log.e(TAG, "Failed to update alarm", e);
            Snackbar.make(recyclerView, "Failed to update alarm", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void deleteAlarm(int position) {
        if (position < 0 || position >= alarmList.size()) {
            return;
        }

        Alarm alarm = alarmList.get(position);

        try {
            // Remove from database
            dbHelper.deleteAlarm(alarm.getId());

            // Cancel scheduled alarm
            cancelAlarm(alarm.getId());

            // Remove from adapter
            alarmAdapter.removeAlarm(position);
            updateEmptyState();

            Log.d(TAG, "Deleted alarm: ID=" + alarm.getId());

            // Show undo option
            Snackbar.make(recyclerView, "Alarm deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO", v -> restoreAlarm(alarm, position))
                    .show();

        } catch (Exception e) {
            Log.e(TAG, "Failed to delete alarm", e);
            Snackbar.make(recyclerView, "Failed to delete alarm", Snackbar.LENGTH_SHORT).show();
        }
    }

    private void restoreAlarm(Alarm alarm, int position) {
        try {
            // Add back to database
            long newId = dbHelper.addAlarm(alarm);
            alarm.setId((int) newId);

            // Schedule if enabled
            if (alarm.isEnabled()) {
                alarm.setNextAlarmTime(alarm.calculateNextAlarmTime());
                scheduleAlarm(alarm);
            }

            // Add back to list at original position
            alarmList.add(Math.min(position, alarmList.size()), alarm);
            alarmAdapter.notifyItemInserted(Math.min(position, alarmList.size()));
            updateEmptyState();

            Log.d(TAG, "Restored alarm: ID=" + alarm.getId());
        } catch (Exception e) {
            Log.e(TAG, "Failed to restore alarm", e);
        }
    }

    // len lich bao thuc
    // Alarm scheduling methods
    private void scheduleAlarm(Alarm alarm) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);

        // Pass comprehensive alarm data
        intent.putExtra("alarm_id", alarm.getId());
        intent.putExtra("alarm_hour", alarm.getHour());
        intent.putExtra("alarm_minute", alarm.getMinute());
        intent.putExtra("alarm_label", alarm.getLabel());
        intent.putExtra("snooze_enabled", alarm.isSnooze());
        intent.putExtra("snooze_duration", alarm.getSnoozeDuration());
        intent.putExtra("vibration_enabled", alarm.isVibrationEnabled());
        intent.putExtra("ringtone", alarm.getRingtone());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(),
                alarm.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long triggerTime = alarm.calculateNextAlarmTime();

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

    // hủy một báo thức đã được đặt
    private void cancelAlarm(int alarmId) {
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(),
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

    // Swipe to delete implementation with modern UI
    private class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {
        private final ColorDrawable background;
        private final Drawable deleteIcon;

        SwipeToDeleteCallback() {
            super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
            background = new ColorDrawable(Color.parseColor("#FF5252"));
            deleteIcon = ContextCompat.getDrawable(getContext(), R.drawable.ic_alarm);
            if (deleteIcon != null) {
                deleteIcon.setTint(Color.WHITE);
            }
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            deleteAlarm(position);
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder,
                                float dX, float dY, int actionState, boolean isCurrentlyActive) {

            View itemView = viewHolder.itemView;

            if (dX > 0) { // Swiping right
                background.setBounds(itemView.getLeft(), itemView.getTop(),
                        itemView.getLeft() + (int) dX, itemView.getBottom());
            } else if (dX < 0) { // Swiping left
                background.setBounds(itemView.getRight() + (int) dX, itemView.getTop(),
                        itemView.getRight(), itemView.getBottom());
            } else {
                background.setBounds(0, 0, 0, 0);
            }

            background.draw(c);

            // Draw delete icon
            if (deleteIcon != null) {
                int iconTop = itemView.getTop() + (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();

                if (dX > 0) { // Swiping right
                    int iconLeft = itemView.getLeft() + 32;
                    int iconRight = iconLeft + deleteIcon.getIntrinsicWidth();
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                } else if (dX < 0) { // Swiping left
                    int iconLeft = itemView.getRight() - 32 - deleteIcon.getIntrinsicWidth();
                    int iconRight = itemView.getRight() - 32;
                    deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                }

                deleteIcon.draw(c);
            }

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Làm mới danh sách báo thức
        if (alarmAdapter != null) {
            alarmList = dbHelper.getAllAlarms();
            for (Alarm alarm : alarmList) {
                if (alarm.isEnabled()) {
                    alarm.setNextAlarmTime(alarm.calculateNextAlarmTime());
                }
            }
            alarmAdapter = new AlarmAdapter(alarmList, this, getContext());
            recyclerView.setAdapter(alarmAdapter);
            updateEmptyState();
            Log.d(TAG, "Refreshed alarm list on resume");
        }
    }
}