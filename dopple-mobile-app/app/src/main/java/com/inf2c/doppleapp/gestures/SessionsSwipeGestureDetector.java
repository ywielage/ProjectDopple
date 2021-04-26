package com.inf2c.doppleapp.gestures;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public abstract class SessionsSwipeGestureDetector extends GestureDetector.SimpleOnGestureListener
{
    private static final String TAG = SessionsSwipeGestureDetector.class.getSimpleName();

    private View view;

    private boolean selectionStart;

    public SessionsSwipeGestureDetector(View view) {
        this.view = view;
    }

    //FOR GESTURE
    @Override
    public boolean onFling(MotionEvent motionEventOne, MotionEvent motionEventTwo, float velocityX, float velocityY) {
        if (motionEventOne == null || motionEventTwo == null) {
            return false;
        } else if (motionEventOne.getPointerCount() > 1 || motionEventTwo.getPointerCount() > 1) {
            return false;
        } else {
            if (isSelectionStart()) {

            } else {

                try {
                    float mRightToLeftCover = motionEventOne.getX() - motionEventTwo.getX();
                    float mTopToBottomCover = motionEventTwo.getY() - motionEventOne.getY();
                    float mVelocityX = velocityX;
                    float mVelocityY = velocityY;

                    if (mRightToLeftCover >= 0) {
                        if (mTopToBottomCover >= 0) {
                            if (mTopToBottomCover < 100) {
                                if (mRightToLeftCover > 100) {
                                    Log.d(TAG, "1. R =>> L");
                                    //onRightToLeftSwap();
                                }
                            } else {
                                if (mRightToLeftCover < 100) {
                                    Log.d(TAG, "9. T ==>> B");
                                    onTopToBottomSwap();
                                } else {
                                    Log.d(TAG, "2. T ==>> B, R =>> L");
                                }
                            }
                        } else {
                            if (mTopToBottomCover > -100) {
                                if (mRightToLeftCover > 100) {
                                    Log.d(TAG, "3. R =>> L");
                                    //onRightToLeftSwap();
                                }
                            } else {
                                if (mRightToLeftCover < 100) {
                                    Log.d(TAG, "10. B ==>> T");
                                    onBottomToTopSwap();
                                } else {
                                    Log.d(TAG, "4. B ==>> T, R =>> L");
                                }
                            }
                        }
                    } else if (mRightToLeftCover < 0) {
                        if (mTopToBottomCover >= 0) {
                            if (mTopToBottomCover < 100) {
                                if (mRightToLeftCover > -100) {
                                    Log.d(TAG, "5. L =>> R");
                                    //onLeftToRightSwap();
                                }
                            } else {
                                if (mRightToLeftCover > -100) {
                                    Log.d(TAG, "11. T ==>> B");
                                    onTopToBottomSwap();
                                } else {
                                    Log.d(TAG, "6. T ==>> B, L =>> R");
                                }
                            }
                        } else {
                            if (mTopToBottomCover > -100) {
                                if (mRightToLeftCover < -100) {
                                    Log.d(TAG, "7. L =>> R");
                                    //onLeftToRightSwap();
                                }
                            } else {
                                if (mRightToLeftCover < -100) {
                                    Log.d(TAG, "12. B ==>> T");
                                    onBottomToTopSwap();
                                } else {
                                    Log.d(TAG, "8. B ==>> T, L =>> R");
                                }
                            }
                        }
                    }

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    //EXPERIMENTAL PURPOSE
    public abstract void onTopToBottomSwap();

    public abstract void onBottomToTopSwap();


    public boolean isSelectionStart() {
        return selectionStart;
    }

    public void setSelectionStart(boolean selectionStart) {
        this.selectionStart = selectionStart;
    }

}

