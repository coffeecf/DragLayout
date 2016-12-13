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
    private View frontView;
    private int frontViewWidth;
    private int frontViewHeight;
    private int frontViewLeft;
    private int frontViewRight;

    private ViewDragHelper.Callback dragHelperCallback = new ViewDragHelper.Callback() {

        @Override
        public int clampViewPositionHorizontal(View child, int back, int dx) {
            switch (mode) {
                case RIGHT_DRAW_TO_SHOW:
                    if (frontViewLeft + dx < 0) {
                        return 0;
                    } else if (frontViewLeft + dx > range) {
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
            return child == frontView && canDrag;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return frontViewWidth;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (mode == RIGHT_DRAW_TO_SHOW) {
                if (xvel > 0) {
                    open();
                } else if (xvel < 0) {
                    close();
                } else if (frontViewHeight > frontViewWidth * 1.2) {
                    open();
                } else {
                    close();
                }
            } else if (mode == LEFT_DRAW_TO_SHOW) {
                if (xvel > 0) {
                    close();
                } else if (xvel < 0) {
                    open();
                } else if (frontViewHeight < frontViewWidth * 0.7) {
                    open();
                } else {
                    close();
                }
            } else if (mode == RIGHT_DRAW_TO_LEAVE) {
                if (xvel > 0) {
                    activityExit();
                } else if (xvel < 0) {
                    close();
                } else if (frontViewHeight > frontViewWidth * 1.2) {
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
                    if (frontViewLeft >= 0) {
                        close();
                    } else {
                        open();
                    }
                } else if (frontViewHeight > frontViewWidth * 1.3) {
                    activityExit();
                } else if (frontViewHeight < frontViewWidth * 0.7) {
                    open();
                } else {
                    close();
                }

            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int back, int top,
                                          int dx, int dy) {
            frontViewLeft = back;
            frontViewHeight = back + frontViewWidth;

            if (changedView == backView) {
                backView.layout(0, 0, frontViewWidth, frontViewRight);
                frontView.layout(frontViewLeft, 0, frontViewLeft + frontViewWidth, frontViewRight);
                }

            dispatchDragEvent(frontViewLeft);
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
                i = i + frontViewWidth;
                percent = 1 - (i - (frontViewWidth + range)) / (float) -range;
                animateLeftShow(percent);
                break;
            case RIGHT_DRAW_TO_LEAVE:
                percent = i / (float) frontViewWidth;
                animateRightLeave(percent);
                break;
            case RIGHT_LEAVE_AND_LEFT_SHOW:
                percent = i / (float) frontViewWidth;
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
            frontView = getChildAt(1);
            backView.setClickable(true);

        } else {//表示mode == RIGHT_DRAW_TO_LEAVE
            frontView = getChildAt(0);
            setBackgroundColor(Color.BLACK);
            setActivityBackgroundTransparent();
        }
        frontView.setClickable(true);

        if (mode == RIGHT_LEAVE_AND_LEFT_SHOW) {
            backupBackground = getBackground();
            setActivityBackgroundTransparent();
        }
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        frontViewWidth = frontView.getMeasuredWidth();
        frontViewRight = frontView.getMeasuredHeight();
        if (mode == RIGHT_DRAW_TO_SHOW) {
            range = (int) (frontViewWidth * 0.6f);
        } else {
            range = (int) -(frontViewWidth * 0.65f);
        }


    }

    @Override
    protected void onLayout(boolean changed, int back, int top, int right, int bottom) {
        if (backView != null) {
            backView.layout(0, 0, frontViewWidth, frontViewRight);
        }
        frontView.layout(frontViewLeft, 0, frontViewLeft + frontViewWidth, frontViewRight);
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
        frontView.setScaleX(f1);
        frontView.setScaleY(f1);
        backView.setTranslationX(-backView.getWidth() / 2.3f + backView.getWidth() / 2.3f * percent);
        backView.setScaleX(0.5f + 0.5f * percent);
        backView.setScaleY(0.5f + 0.5f * percent);
        backView.setAlpha(percent);

    }

    private void animateLeftShow(float percent) {
        backView.setTranslationX(-frontViewWidth / 6.0f * percent + frontViewWidth / 6.0f);
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
            backView.setTranslationX(frontViewWidth / 6.0f * percent + frontViewWidth / 6.0f);
        }

    }

    /**
     * 公开外边调用的方法↓
     */
    public void setDragListener(DragListener dragListener) {
        this.dragListener = dragListener;
    }

    public Status getStatus() {
        if (frontViewHeight == frontViewWidth || frontViewLeft == 0) {
            status = Status.Close;
        } else if (frontViewLeft == range || frontViewHeight - range == frontViewWidth) {
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
            if (dragHelper.smoothSlideViewTo(frontView, range, 0)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            frontView.layout(range, 0, range * 2, frontViewRight);
            dispatchDragEvent(range);
        }
    }

    public void close() {
        close(true);
    }

    public void close(boolean animate) {
        if (animate) {
            if (dragHelper.smoothSlideViewTo(frontView, 0, 0)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            frontView.layout(0, 0, frontViewWidth, frontViewRight);
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
