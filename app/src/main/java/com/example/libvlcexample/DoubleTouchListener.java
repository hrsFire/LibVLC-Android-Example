package com.example.libvlcexample;

import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public abstract class DoubleTouchListener implements OnTouchListener {

    private static final long DOUBLE_CLICK_TIME_DELTA = 300; // milliseconds
    private long lastClickTime = 0;
    private MotionEvent clickEventDown;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            long clickTime = System.currentTimeMillis();

            if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                onDoubleClick(v);
            } else {
                // ignore the first move down
                clickEventDown = MotionEvent.obtain(event);
            }

            lastClickTime = clickTime;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && clickEventDown != null) {
            onMotion(v, clickEventDown);
            clickEventDown = null;
            onMotion(v, MotionEvent.obtain(event));
        } else {
            onMotion(v, MotionEvent.obtain(event));
        }

        return true;
    }

    public abstract void onMotion(View v, MotionEvent event);
    public abstract void onDoubleClick(View v);
}