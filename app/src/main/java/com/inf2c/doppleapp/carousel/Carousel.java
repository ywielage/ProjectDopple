package com.inf2c.doppleapp.carousel;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import androidx.core.view.MotionEventCompat;

import com.inf2c.doppleapp.R;

public class Carousel extends HorizontalScrollView {

    private int prevScrolled = 0;
    private boolean center = false;
    final HorizontalScrollView hsv = this;

    public Carousel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public Carousel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Carousel(Context context) {
        super(context);
    }

    public void autoSmoothScroll(float pos)
    {
        final int move = (int)(pos);
        hsv.postDelayed(new Runnable() {
            @Override
            public void run()
            {
                hsv.smoothScrollTo(move, 0);
            }
        },0);
    }

    public void selectCenter()
    {
        if(!center)
        {
            hsv.postDelayed(new Runnable() {
                @Override
                public void run()
                {
                    hsv.scrollTo(hsv.getWidth(), 0);
                    center = true;
                }
            },50);
        }
    }

    public void stick() {
        View nextChild = this.getChildAt(0);
        int scrollPos = this.getScrollX();
        double scrollFactor = scrollPos > prevScrolled ? 0.01 : 0.99;

        for(int n = 0; n < ((ViewGroup)nextChild).getChildCount(); n++) {
            float childPos = n * this.getWidth();
            if(childPos + this.getWidth() * scrollFactor > scrollPos)
            {
                autoSmoothScroll(childPos);
                break;
            }
        }

        prevScrolled = scrollPos;
    }

    public void resizeChildren() {
        View nextChild = this.getChildAt(0);

        for(int n = 0; n < ((ViewGroup)nextChild).getChildCount(); n++)
        {
            View child = ((ViewGroup)nextChild).getChildAt(n);
            child.getLayoutParams().width = this.getWidth();
            child.setLayoutParams(child.getLayoutParams());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        selectCenter();
        resizeChildren();

        super.onDraw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        int action = event.getActionMasked();
        if(action == MotionEvent.ACTION_UP)
        {
            stick();
        }
        return super.onTouchEvent(event);
    }
}
