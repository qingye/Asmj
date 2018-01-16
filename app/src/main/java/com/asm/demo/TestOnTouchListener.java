package com.asm.demo;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by chris on 2018/1/8.
 */

public class TestOnTouchListener implements View.OnTouchListener {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.e("DEMO", "TestOnTouchListener onTouch");
        return false;
    }
}
