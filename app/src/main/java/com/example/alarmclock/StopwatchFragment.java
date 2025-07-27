package com.example.alarmclock;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.Locale;

public class StopwatchFragment extends Fragment {
    private TextView timeText;
    private Button startStopButton, resetButton;
    private long startTime = 0;
    private boolean isRunning = false;
    private final android.os.Handler handler = new android.os.Handler();

    private final Runnable updateTime = new Runnable() {
        @Override
        public void run() {
            long elapsed = System.currentTimeMillis() - startTime;
            int seconds = (int) (elapsed / 1000);
            int minutes = seconds / 60;
            seconds %= 60;
            int milliseconds = (int) (elapsed % 1000) / 10;
            timeText.setText(String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, milliseconds));
            handler.postDelayed(this, 10);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stopwatch, container, false);
        timeText = view.findViewById(R.id.text_stopwatch);
        startStopButton = view.findViewById(R.id.button_start_stop);
        resetButton = view.findViewById(R.id.button_reset);

        startStopButton.setOnClickListener(v -> {
            if (isRunning) {
                handler.removeCallbacks(updateTime);
                startStopButton.setText("Start");
            } else {
                startTime = System.currentTimeMillis();
                handler.post(updateTime);
                startStopButton.setText("Stop");
            }
            isRunning = !isRunning;
        });

        resetButton.setOnClickListener(v -> {
            handler.removeCallbacks(updateTime);
            startTime = 0;
            isRunning = false;
            timeText.setText("00:00.00");
            startStopButton.setText("Start");
        });

        return view;
    }
}
