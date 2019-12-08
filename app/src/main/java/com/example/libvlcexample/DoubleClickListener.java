package com.example.libvlcexample;

import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import java.util.Timer;
import java.util.TimerTask;

public abstract class DoubleClickListener implements OnClickListener {

    private Timer timer = null;
    private int SCHEDULE_DELAY = 400;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // milliseconds
    private long lastClickTime = 0;

    @Override
    public void onClick(View v) {
        long clickTime = System.currentTimeMillis();

        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
            processDoubleClickEvent(v);
        } else {
            processSingleClickEvent(v);
        }

        lastClickTime = clickTime;
    }

    private void processSingleClickEvent(final View v) {
        final Handler handler = new Handler();

        TimerTask timertask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        onSingleClick(v);
                    }
                });
            }
        };

        timer = new Timer();
        timer.schedule(timertask, SCHEDULE_DELAY);

    }

    private void processDoubleClickEvent(View v) {
        if(timer != null) {
            timer.cancel(); // Cancels Running Tasks or Waiting Tasks.
            timer.purge();  // Frees Memory by erasing cancelled Tasks.
        }

        onDoubleClick(v);
    }

    public abstract void onSingleClick(View v);
    public abstract void onDoubleClick(View v);
}