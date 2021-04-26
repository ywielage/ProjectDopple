package com.inf2c.doppleapp.heart_rate;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DraggableLayout extends FrameLayout {
    private float startingPointerX;
    private float startingViewX;

    private float minX = 0;
    private float maxX = 850;

    public DraggableLayout(@NonNull Context context) {
        this(context, null, 0);
    }

    public DraggableLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setWillNotDraw(false);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        maxX = getMeasuredWidth()- 280;
        minX = getLeft();
        setX(maxX);
        Log.d("DragLayout", "max x: " + maxX);
    }

    public float getMinX(){
        return minX;
    }

    public float getMaxX(){
        return maxX;
    }

    public float getCurrentX(){
        return getX();
    }

    public void moveToX(float x){
        animate().x(x);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float pointerX = event.getRawX();
        float dx = pointerX - startingPointerX;
        float viewX = startingViewX + dx;

        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                startingViewX = getX();
                startingPointerX = event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                //voor smooth slide:
                if(viewX < minX ){
                    viewX = minX;
                }
                if(viewX > maxX ){
                    viewX = maxX;
                }
                setX(viewX);
                break;
            case MotionEvent.ACTION_UP:
                if(viewX < minX || (viewX > minX && viewX <= maxX / 2)){
                    viewX = minX;
                }
                if(viewX > maxX || (viewX < maxX && viewX >= maxX / 2)){
                    viewX = maxX;
                }
                //setX(viewX);
                animate().x(viewX);
                break;
        }
        return true;
    }
}
