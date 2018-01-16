package com.asm.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends BaseActivity {

    private TextView center = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("DEMO", "textView onClick");
            }
        });
        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.e("DEMO", "textView onTouch");
                return false;
            }
        });

        initView();
    }

    private void initView() {
        center = (TextView) findViewById(R.id.center);
        center.setOnTouchListener(new TestOnTouchListener());
    }
}
