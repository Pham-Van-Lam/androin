package com.example.alarmclock;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AlarmDialogFragment extends DialogFragment {
    private static final String TAG = "AlarmDialogFragment";

    // UI Components
    private TimePicker timePicker;
    private TextInputEditText editAlarmLabel;
    private MaterialSwitch switchSnooze;
    private MaterialSwitch switchVibration;
    private MaterialButton btnSnoozeDuration;

    // Quick preset chips
    private Chip chipOnce;
    private Chip chipWeekdays;
    private Chip chipDaily;

    // Day selection buttons
    private MaterialButton[] dayButtons;
    private boolean[] selectedDays = new boolean[7]; // Sun=0, Mon=1, ..., Sat=6

    // Alarm data
    private Alarm alarm;
    private OnAlarmSetListener listener;
    private int snoozeDuration = 10; // Default 10 minutes

    public interface OnAlarmSetListener {
        void onAlarmSet(Alarm alarm);
    }

    public static AlarmDialogFragment newInstance(Alarm alarm) {
        AlarmDialogFragment fragment = new AlarmDialogFragment();
        Bundle args = new Bundle();
        if (alarm != null) {
            args.putSerializable("alarm", (Serializable) alarm);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnAlarmSetListener(OnAlarmSetListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_alarm, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeViews(view);
        setupListeners();
        loadAlarmData();
    }

    private void initializeViews(View view) {
        // Time picker
        timePicker = view.findViewById(R.id.time_picker);

        // Text input
        editAlarmLabel = view.findViewById(R.id.edit_alarm_label);

        // Switches
        switchSnooze = view.findViewById(R.id.switch_snooze);
        switchVibration = view.findViewById(R.id.switch_vibration);

        // Buttons
        btnSnoozeDuration = view.findViewById(R.id.btn_snooze_duration);

        // Quick preset chips
        chipOnce = view.findViewById(R.id.chip_once);
        chipWeekdays = view.findViewById(R.id.chip_weekdays);
        chipDaily = view.findViewById(R.id.chip_daily);

        // Day selection buttons
        dayButtons = new MaterialButton[]{
                view.findViewById(R.id.btn_sun),
                view.findViewById(R.id.btn_mon),
                view.findViewById(R.id.btn_tue),
                view.findViewById(R.id.btn_wed),
                view.findViewById(R.id.btn_thu),
                view.findViewById(R.id.btn_fri),
                view.findViewById(R.id.btn_sat)
        };

        // Action buttons
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        MaterialButton btnSave = view.findViewById(R.id.btn_save);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSave.setOnClickListener(v -> saveAlarm());
    }

    private void setupListeners() {
        // Snooze switch listener
        switchSnooze.setOnCheckedChangeListener((buttonView, isChecked) -> {
            View snoozeLayout = getView().findViewById(R.id.layout_snooze_duration);
            snoozeLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Snooze duration button
        btnSnoozeDuration.setOnClickListener(v -> showSnoozeDurationDialog());

        // Quick preset chips
        chipOnce.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                clearAllDays();
                chipWeekdays.setChecked(false);
                chipDaily.setChecked(false);
            }
        });

        chipWeekdays.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setWeekdays();
                chipOnce.setChecked(false);
                chipDaily.setChecked(false);
            }
        });

        chipDaily.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setAllDays();
                chipOnce.setChecked(false);
                chipWeekdays.setChecked(false);
            }
        });

        // Day selection buttons
        for (int i = 0; i < dayButtons.length; i++) {
            final int dayIndex = i;
            dayButtons[i].setOnClickListener(v -> toggleDay(dayIndex));
        }

        // Ringtone selection (if implemented)
        assert getView() != null;
        View ringtoneLayout = (View) getView().findViewById(R.id.text_selected_ringtone).getParent();
        ringtoneLayout.setOnClickListener(v -> showRingtoneSelector());
    }

    private void loadAlarmData() {
        if (getArguments() != null) {
            alarm = (Alarm) getArguments().getSerializable("alarm");
            if (alarm != null) {
                Log.d(TAG, "Editing existing alarm: ID=" + alarm.getId());

                // Set time
                timePicker.setHour(alarm.getHour());
                timePicker.setMinute(alarm.getMinute());

                // Set label
                if (!TextUtils.isEmpty(alarm.getLabel())) {
                    editAlarmLabel.setText(alarm.getLabel());
                }

                // Set switches
                switchSnooze.setChecked(alarm.isSnooze());
                switchVibration.setChecked(alarm.isVibrationEnabled());

                // Set snooze duration
                snoozeDuration = alarm.getSnoozeDuration();
                btnSnoozeDuration.setText(snoozeDuration + " min");

                // Set selected days
                for (int day : alarm.getDaysOfWeek()) {
                    if (day >= 1 && day <= 7) {
                        selectedDays[day - 1] = true; // Convert 1-7 to 0-6
                    }
                }
                updateDayButtons();
                updateChipStates();
            } else {
                Log.d(TAG, "Creating new alarm");
                // Set default values for new alarm
                chipOnce.setChecked(true);
            }
        }
    }

    private void toggleDay(int dayIndex) {
        selectedDays[dayIndex] = !selectedDays[dayIndex];
        updateDayButtons();
        updateChipStates();
    }

    private void updateDayButtons() {
        for (int i = 0; i < dayButtons.length; i++) {
            dayButtons[i].setSelected(selectedDays[i]);
            // Update button appearance based on selection
            if (selectedDays[i]) {
                dayButtons[i].setBackgroundTintList(getResources().getColorStateList(R.color.day_button_selected));
                dayButtons[i].setTextColor(getResources().getColor(R.color.day_button_text_selected));
            } else {
                dayButtons[i].setBackgroundTintList(getResources().getColorStateList(R.color.day_button_unselected));
                dayButtons[i].setTextColor(getResources().getColor(R.color.day_button_text_unselected));
            }
        }
    }

    private void updateChipStates() {
        int selectedCount = 0;
        boolean isWeekdays = true;

        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                selectedCount++;
                // Check if it's not a weekday (Mon-Fri = indices 1-5)
                if (i == 0 || i == 6) { // Sunday or Saturday
                    isWeekdays = false;
                }
            } else if (i >= 1 && i <= 5) { // Missing a weekday
                isWeekdays = false;
            }
        }

        // Update chip states without triggering listeners
        chipOnce.setOnCheckedChangeListener(null);
        chipWeekdays.setOnCheckedChangeListener(null);
        chipDaily.setOnCheckedChangeListener(null);

        if (selectedCount == 0) {
            chipOnce.setChecked(true);
            chipWeekdays.setChecked(false);
            chipDaily.setChecked(false);
        } else if (selectedCount == 7) {
            chipOnce.setChecked(false);
            chipWeekdays.setChecked(false);
            chipDaily.setChecked(true);
        } else if (selectedCount == 5 && isWeekdays) {
            chipOnce.setChecked(false);
            chipWeekdays.setChecked(true);
            chipDaily.setChecked(false);
        } else {
            chipOnce.setChecked(false);
            chipWeekdays.setChecked(false);
            chipDaily.setChecked(false);
        }

        // Restore listeners
        setupChipListeners();
    }

    private void setupChipListeners() {
        chipOnce.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                clearAllDays();
                chipWeekdays.setChecked(false);
                chipDaily.setChecked(false);
            }
        });

        chipWeekdays.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setWeekdays();
                chipOnce.setChecked(false);
                chipDaily.setChecked(false);
            }
        });

        chipDaily.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                setAllDays();
                chipOnce.setChecked(false);
                chipWeekdays.setChecked(false);
            }
        });
    }

    private void clearAllDays() {
        Arrays.fill(selectedDays, false);
        updateDayButtons();
    }

    private void setWeekdays() {
        clearAllDays();
        // Set Monday to Friday (indices 1-5)
        for (int i = 1; i <= 5; i++) {
            selectedDays[i] = true;
        }
        updateDayButtons();
    }

    private void setAllDays() {
        Arrays.fill(selectedDays, true);
        updateDayButtons();
    }

    private void showSnoozeDurationDialog() {
        String[] durations = {"1 min", "5 min", "10 min", "15 min", "30 min"};
        int[] durationValues = {1, 5, 10, 15, 30};

        int currentIndex = Arrays.binarySearch(durationValues, snoozeDuration);
        if (currentIndex < 0) currentIndex = 2; // Default to 10 min

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Snooze Duration")
                .setSingleChoiceItems(durations, currentIndex, (dialog, which) -> {
                    snoozeDuration = durationValues[which];
                    btnSnoozeDuration.setText(snoozeDuration + " min");
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRingtoneSelector() {
        // Implement ringtone selection
        Toast.makeText(getContext(), "Ringtone selector not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void saveAlarm() {
        // Validate input
        if (chipOnce.isChecked() && !hasSelectedDays()) {
            // For "once" alarms, no days selected is OK
        }

        // Get selected days as List<Integer> (1-7 format)
        List<Integer> days = new ArrayList<>();
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) {
                days.add(i + 1); // Convert 0-6 to 1-7
            }
        }

        // Create or update alarm
        int alarmId = (alarm != null) ? alarm.getId() : 0;
        String label = editAlarmLabel.getText() != null ? editAlarmLabel.getText().toString().trim() : "";

        Alarm newAlarm = new Alarm(
                alarmId,
                timePicker.getHour(),
                timePicker.getMinute(),
                days,
                switchSnooze.isChecked(),
                true, // enabled by default
                label,
                switchVibration.isChecked(),
                snoozeDuration
        );

        Log.d(TAG, "Saving alarm: ID=" + newAlarm.getId() +
                ", Time=" + newAlarm.getHour() + ":" + newAlarm.getMinute() +
                ", Days=" + days.toString() +
                ", Label=" + label);

        if (listener != null) {
            listener.onAlarmSet(newAlarm);
            dismiss();
        } else {
            Log.e(TAG, "OnAlarmSetListener is null");
            Toast.makeText(getContext(), "Error saving alarm", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean hasSelectedDays() {
        for (boolean selected : selectedDays) {
            if (selected) return true;
        }
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Make dialog full screen on small devices
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}