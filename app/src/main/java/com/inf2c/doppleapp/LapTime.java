package com.inf2c.doppleapp;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

public class LapTime extends LinearLayout {
    private Context viewContext = null;

    public LapTime(Context context) {
        super(context);
        this.viewContext = context;
        this.create(1);
    }

    public LapTime(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.viewContext = context;
        this.create(1);
    }

    public LapTime(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.viewContext = context;
        this.create(1);
    }

    public LapTime(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.viewContext = context;
        this.create(1);
    }

    public void create(int number)
    {
        this.setOrientation(LinearLayout.HORIZONTAL);
        this.setBackgroundColor(Color.BLUE);

        TextView textView1 = new TextView(this.viewContext);
        textView1.setTextSize(40);
        textView1.setText(number);

        this.addView(textView1);
        this.invalidate();
    }

}
