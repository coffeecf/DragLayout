package com.example.draglayout;

/**
 * 1.0版本
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;


public class DragLayout extends FrameLayout {
    public static final int RIGHT_DRAW_TO_SHOW = 0;
    public static final int LEFT_DRAW_TO_SHOW = 1;
    public static final int RIGHT_DRAW_TO_LEAVE = 2;
    public static final int RIGHT_LEAVE_AND_LEFT_SHOW = 3;

    private int mode = RIGHT_DRAW_TO_SHOW;
    private boolean hasBackground = true; //只用于右滑退出
    private boolean canDrag = true;
    private Status status = Status.Close;

    private Drawable backupBackground; //只用于左显右退
    private GestureDetectorCompat gestureDetector;
    private ViewDragHelper dragHelper;
    private DragListener dragListener;
    private int range; //表示释放后是执行还是取消的距离
    private Context context;

    private View backView;
    private View mainView;
    private int mainViewWidth;
    private int mainViewHeight;
    private int mainViewLeft;
    private int mainViewRight;

    private ViewDragHelper.Callback dragHelperCallback = new ViewDragHelper.Callback() {

        @Override
        public int clampViewPositionHorizontal(View child, int back, int dx) {
            switch (mode) {
                case RIGHT_DRAW_TO_SHOW:
                    if (mainViewLeft + dx < 0) {
                        return 0;
                    } else if (mainViewLeft + dx > range) {
                        return range;
                    } else {
                        return back;
                    }
                case LEFT_DRAW_TO_SHOW:
                    if (back > 0) {
                        return 0;
                    } else if (back < range) {
                        return range;
                    } else {
                        return back;
                    }
                case RIGHT_DRAW_TO_LEAVE:
                    if (back < 0) {
                        return 0;
                    }
                    return back;
                case RIGHT_LEAVE_AND_LEFT_SHOW:
                    if (back < range) {
                        return range;
                    }
                    return back;

                default:
                    return back;
            }


        }

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return child == mainView && canDrag;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mainViewWidth;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (mode == RIGHT_DRAW_TO_SHOW) {
                if (xvel > 0) {
                    open();
                } else if (xvel < 0) {
                    close();
                } else if (mainViewHeight > mainViewWidth * 1.2) {
                    open();
                } else {
                    close();
                }
            } else if (mode == LEFT_DRAW_TO_SHOW) {
                if (xvel > 0) {
                    close();
                } else if (xvel < 0) {
                    open();
                } else if (mainViewHeight < mainViewWidth * 0.7) {
                    open();
                } else {
                    close();
                }
            } else if (mode == RIGHT_DRAW_TO_LEAVE) {
                if (xvel > 0) {
                    activityExit();
                } else if (xvel < 0) {
                    close();
                } else if (mainViewHeight > mainViewWidth * 1.2) {
                    activityExit();
                } else {
                    close();
                }
            } else if (mode == RIGHT_LEAVE_AND_LEFT_SHOW) {
                if (xvel > 0) {
                    if (status == Status.Close) {
                        activityExit();
                    } else {
                        close();
                    }
                } else if (xvel < 0) {
                    if (mainViewLeft >= 0) {
                        close();
                    } else {
                        open();
                    }
                } else if (mainViewHeight > mainViewWidth * 1.3) {
                    activityExit();
                } else if (mainViewHeight < mainViewWidth * 0.7) {
                    open();
                } else {
                    close();
                }

            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int back, int top,
                                          int dx, int dy) {
            mainViewLeft = back;
            mainViewHeight = back + mainViewWidth;

            if (changedView == backView) {
                backView.layout(0, 0, mainViewWidth, mainViewRight);
                mainView.layout(mainViewLeft, 0, mainViewLeft + mainViewWidth, mainViewRight);
                }

            dispatchDragEvent(mainViewLeft);
        }
    };

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        this.context = context;
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        gestureDetector = new GestureDetectorCompat(context, new YScrollDetector());
        dragHelper = ViewDragHelper.create(this, dragHelperCallback);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.DragLayout);
        if (typedArray != null) {
            mode = typedArray.getInteger(R.styleable.DragLayout_mode, RIGHT_DRAW_TO_SHOW);
            typedArray.recycle();
        }
    }

    private void dispatchDragEvent(int i) {
        float percent = 0;
        switch (mode) {
            case RIGHT_DRAW_TO_SHOW:
                percent = i / (float) range;
                animateRightShow(percent);
                break;
            case LEFT_DRAW_TO_SHOW:
                i = i + mainViewWidth;
                percent = 1 - (i - (mainViewWidth + range)) / (float) -range;
                animateLeftShow(percent);
                break;
            case RIGHT_DRAW_TO_LEAVE:
                percent = i / (float) mainViewWidth;
                animateRightLeave(percent);
                break;
            case RIGHT_LEAVE_AND_LEFT_SHOW:
                percent = i / (float) mainViewWidth;
                animateLShowRLeave(percent);
        }
        if (dragListener == null) {
            return;
        }
        dragListener.onDrag(percent);
        Status lastStatus = status;
        if (lastStatus != getStatus() && status == Status.Close) {
            dragListener.onClose();
        } else if (lastStatus != getStatus() && status == Status.Open) {
            dragListener.onOpen();
        }
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mode != RIGHT_DRAW_TO_LEAVE) {
            backView = getChildAt(0);
            mainView = getChildAt(1);
            backView.setClickable(true);

        } else {//表示mode == RIGHT_DRAW_TO_LEAVE
            mainView = getChildAt(0);
            setBackgroundColor(Color.BLACK);
            setActivityBackgroundTransparent();
        }
        mainView.setClickable(true);

        if (mode == RIGHT_LEAVE_AND_LEFT_SHOW) {
            backupBackground = getBackground();
            setActivityBackgroundTransparent();
        }
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mainViewWidth = mainView.getMeasuredWidth();
        mainViewRight = mainView.getMeasuredHeight();
        if (mode == RIGHT_DRAW_TO_SHOW) {
            range = (int) (mainViewWidth * 0.6f);
        } else {
            range = (int) -(mainViewWidth * 0.65f);
        }


    }

    @Override
    protected void onLayout(boolean changed, int back, int top, int right, int bottom) {
        if (backView != null) {
            backView.layout(0, 0, mainViewWidth, mainViewRight);
        }
        mainView.layout(mainViewLeft, 0, mainViewLeft + mainViewWidth, mainViewRight);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return dragHelper.shouldInterceptTouchEvent(ev) && gestureDetector.onTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        try {
            dragHelper.processTouchEvent(e);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    public void computeScroll() {
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }


    private void setActivityBackgroundTransparent(){
        Activity activity = (Activity) context;
        activity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        activity.getWindow().getDecorView().setBackground(null);
    }


    /**
     * 显示释放后动画的方法↓
     */
    private void animateRightShow(float percent) {
        float f1 = 1 - percent * 0.3f;
        mainView.setScaleX(f1);
        mainView.setScaleY(f1);
        backView.setTranslationX(-backView.getWidth() / 2.3f + backView.getWidth() / 2.3f * percent);
        backView.setScaleX(0.5f + 0.5f * percent);
        backView.setScaleY(0.5f + 0.5f * percent);
        backView.setAlpha(percent);

    }

    private void animateLeftShow(float percent) {
        backView.setTranslationX(-mainViewWidth / 6.0f * percent + mainViewWidth / 6.0f);
    }

    private void animateRightLeave(float percent) {
        int i = (int) ((1 - percent) * 80);
        getBackground().setAlpha(i);
    }

    private void animateLShowRLeave(float percent) {
        if (percent > 0) {
            if (hasBackground) {
                hasBackground = false;
                setBackgroundColor(Color.BLACK);
                backView.setVisibility(INVISIBLE);
            }
            int i = (int) ((1 - percent) * 100);
            getBackground().setAlpha(i);
        } else {
            if (!hasBackground) {
                hasBackground = true;
                setBackground(backupBackground);
                backView.setVisibility(VISIBLE);
            }
            backView.setTranslationX(mainViewWidth / 6.0f * percent + mainViewWidth / 6.0f);
        }

    }

    /**
     * 公开外边调用的方法↓
     */
    public void setDragListener(DragListener dragListener) {
        this.dragListener = dragListener;
    }

    public Status getStatus() {
        if (mainViewHeight == mainViewWidth || mainViewLeft == 0) {
            status = Status.Close;
        } else if (mainViewLeft == range || mainViewHeight - range == mainViewWidth) {
            status = Status.Open;
        } else {
            status = Status.Drag;
        }

        return status;
    }

    public void activityExit() {
        setBackgroundColor(Color.TRANSPARENT);
        Activity activity = (Activity) context;
        activity.finish();
    }

    public void open() {
        open(true);
    }

    public void open(boolean animate) {
        if (animate) {
            if (dragHelper.smoothSlideViewTo(mainView, range, 0)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            mainView.layout(range, 0, range * 2, mainViewRight);
            dispatchDragEvent(range);
        }
    }

    public void close() {
        close(true);
    }

    public void close(boolean animate) {
        if (animate) {
            if (dragHelper.smoothSlideViewTo(mainView, 0, 0)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            mainView.layout(0, 0, mainViewWidth, mainViewRight);
            dispatchDragEvent(0);
        }
    }

    public void setCanDrag(boolean canDrag) {
        this.canDrag = canDrag;
    }

    public enum Status {
        Drag, Open, Close
    }

    public interface DragListener {
        void onOpen();

        void onClose();

        void onDrag(float percent);
    }

    class YScrollDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return Math.abs(dy) <= Math.abs(dx);
        }
    }

}
