package com.example.alarmclock;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Locale;

public class TimerFragment extends Fragment {
    private TextView timeText;
    private EditText inputMinutes;
    private Button startStopButton, resetButton;
    private long timeLeft = 0;
    private boolean isRunning = false;
    private final android.os.Handler handler = new android.os.Handler();

    private final Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            timeLeft -= 10;
            if (timeLeft <= 0) {
                timeText.setText("00:00");
                isRunning = false;
                startStopButton.setText("Start");
                return;
            }
            int seconds = (int) (timeLeft / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            timeText.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            handler.postDelayed(this, 10);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);
        timeText = view.findViewById(R.id.text_timer);
        inputMinutes = view.findViewById(R.id.input_minutes);
        startStopButton = view.findViewById(R.id.button_start_stop);
        resetButton = view.findViewById(R.id.button_reset);

        startStopButton.setOnClickListener(v -> {
            if (isRunning) {
                handler.removeCallbacks(updateTime);
                startStopButton.setText("Start");
            } else {
                String minutesStr = inputMinutes.getText().toString();
                if (!minutesStr.isEmpty()) {
                    timeLeft = Integer.parseInt(minutesStr) * 60 * 1000;
                    handler.post(updateTime);
                    startStopButton.setText("Stop");
                }
            }
            isRunning = !isRunning;
        });

        resetButton.setOnClickListener(v -> {
            handler.removeCallbacks(updateTime);
            timeLeft = 0;
            isRunning = false;
            timeText.setText("00:00");
            startStopButton.setText("Start");
            inputMinutes.setText("");
        });

        return view;
    }
}