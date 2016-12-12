package com.example.draglayout;

/**
 * Created by Administrator on 2016/9/30.
 */

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;


public class DragLayout extends FrameLayout {
    public static final int RIGHT_DRAW_TO_SHOW = 0;
    public static final int LEFT_DRAW_TO_SHOW = 1;
    public static final int RIGHT_DRAW_TO_LEAVE = 2;
    public static final int RIGHT_LEAVE_AND_LEFT_SHOW = 3;

    private static final int DEFAULT_MODE = RIGHT_DRAW_TO_SHOW;
    private int mode;

    private boolean hasBackground = true;

    private boolean canDrag = true;

    private Drawable background;
    private GestureDetectorCompat gestureDetector;
    private ViewDragHelper dragHelper;
    private DragListener dragListener;
    private int range;
    private int width;
    private int height;
    private int mainLeft;
    private int mainRight;
    private Context context;
    private ImageView iv_shadow;
    private LinearLayout vg_back;
    private MyLinearLayout vg_main;
    private Status status = Status.Close;
    private ViewDragHelper.Callback dragHelperCallback = new ViewDragHelper.Callback() {

        @Override
        public int clampViewPositionHorizontal(View child, int back, int dx) {
            switch (mode) {
                case RIGHT_DRAW_TO_SHOW:
                    if (mainLeft + dx < 0) {
                        return 0;
                    } else if (mainLeft + dx > range) {
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
            return child == vg_main && canDrag;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return width;
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            if (mode == RIGHT_DRAW_TO_SHOW) {
                if (xvel > 0) {
                    open();
                } else if (xvel < 0) {
                    close();
                } else if (mainRight > width * 1.2) {
                    open();
                } else {
                    close();
                }
            } else if (mode == LEFT_DRAW_TO_SHOW) {
                if (xvel > 0) {
                    close();
                } else if (xvel < 0) {
                    open();
                } else if (mainRight < width * 0.7) {
                    open();
                } else {
                    close();
                }
            } else if (mode == RIGHT_DRAW_TO_LEAVE) {
                if (xvel > 0) {
                    activityExit();
                } else if (xvel < 0) {
                    close();
                } else if (mainRight > width * 1.2) {
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
                    if (mainLeft >= 0) {
                        close();
                    } else {
                        open();
                    }
                } else if (mainRight > width * 1.3) {
                    activityExit();
                } else if (mainRight < width * 0.7) {
                    open();
                } else {
                    close();
                }

            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int back, int top,
                                          int dx, int dy) {
            mainLeft = back;
            mainRight = width + back;
            if (mode != RIGHT_DRAW_TO_LEAVE) {
                iv_shadow.layout(mainLeft, 0, mainLeft + width, height);
                if (changedView == vg_back) {
                    vg_back.layout(0, 0, width, height);
                    vg_main.layout(mainLeft, 0, mainLeft + width, height);
                }
            }
            dispatchDragEvent(mainLeft);
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
            mode = typedArray.getInteger(R.styleable.DragLayout_mode, DEFAULT_MODE);
            typedArray.recycle();
        } else {
            mode = DEFAULT_MODE;
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
                i = i + width;
                percent = 1 - (i - (width + range)) / (float) -range;
                animateLeftShow(percent);
                break;
            case RIGHT_DRAW_TO_LEAVE:
                percent = i / (float) width;
                animateRightLeave(percent);
                break;
            case RIGHT_LEAVE_AND_LEFT_SHOW:
                percent = i / (float) width;
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

    public void setDragListener(DragListener dragListener) {
        this.dragListener = dragListener;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mode != RIGHT_DRAW_TO_LEAVE) {
            iv_shadow = new ImageView(context);
            iv_shadow.setImageResource(R.drawable.shadow);
            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            addView(iv_shadow, 1, lp);
            vg_back = (LinearLayout) getChildAt(0);
            vg_main = (MyLinearLayout) getChildAt(2);
            vg_back.setClickable(true);

        } else {//表示mode == RIGHT_DRAW_TO_LEAVE
            vg_main = (MyLinearLayout) getChildAt(0);
            setBackgroundColor(Color.BLACK);
            Activity activity = (Activity) context;
            activity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            activity.getWindow().getDecorView().setBackground(null);
        }

        if (mode == RIGHT_LEAVE_AND_LEFT_SHOW) {
            background = getBackground();
            Activity activity = (Activity) context;
            activity.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            activity.getWindow().getDecorView().setBackground(null);
        }

        vg_main.setDragLayout(this);
        vg_main.setClickable(true);
    }

    public ViewGroup getVg_main() {
        return vg_main;
    }

    public ViewGroup getVg_back() {
        return vg_back;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        width = vg_main.getMeasuredWidth();
        height = vg_main.getMeasuredHeight();
        if (mode == RIGHT_DRAW_TO_SHOW) {
            range = (int) (width * 0.6f);
        } else {
            range = (int) -(width * 0.65f);
        }


    }

    @Override
    protected void onLayout(boolean changed, int back, int top, int right, int bottom) {
        if (vg_back != null) {
            vg_back.layout(0, 0, width, height);
        }
        vg_main.layout(mainLeft, 0, mainLeft + width, height);
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

    class YScrollDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            return Math.abs(dy) <= Math.abs(dx);
        }
    }


    /**
     * 显示释放后动画的方法↓
     */
    private void animateRightShow(float percent) {
        float f1 = 1 - percent * 0.3f;
        vg_main.setScaleX(f1);
        vg_main.setScaleY(f1);
        vg_back.setTranslationX(-vg_back.getWidth() / 2.3f + vg_back.getWidth() / 2.3f * percent);
        vg_back.setScaleX(0.5f + 0.5f * percent);
        vg_back.setScaleY(0.5f + 0.5f * percent);
        vg_back.setAlpha(percent);
        iv_shadow.setScaleX(f1 * 1.4f * (1 - percent * 0.12f));
        iv_shadow.setScaleY(f1 * 1.85f * (1 - percent * 0.12f));

    }

    private void animateLeftShow(float percent) {
        vg_back.setTranslationX(-width / 6.0f * percent + width / 6.0f);
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
                vg_back.setVisibility(INVISIBLE);
            }
            int i = (int) ((1 - percent) * 100);
            getBackground().setAlpha(i);
        } else {
            if (!hasBackground) {
                hasBackground = true;
                setBackground(background);
                vg_back.setVisibility(VISIBLE);
            }
            vg_back.setTranslationX(width / 6.0f * percent + width / 6.0f);
        }

    }


    /**
     * 公开外边调用的方法↓
     */
    public Status getStatus() {
        if (mainRight == width || mainLeft == 0) {
            status = Status.Close;
        } else if (mainLeft == range || mainRight - range == width) {
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
            if (dragHelper.smoothSlideViewTo(vg_main, range, 0)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            vg_main.layout(range, 0, range * 2, height);
            dispatchDragEvent(range);
        }
    }

    public void close() {
        close(true);
    }

    public void close(boolean animate) {
        if (animate) {
            if (dragHelper.smoothSlideViewTo(vg_main, 0, 0)) {
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            vg_main.layout(0, 0, width, height);
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


    /**
     * 数据的内部类↓
     */
    private class MainView {
        public View view;
        public int width;
        public int mainLeft;
        public int mainRight;
    }
    private class BackView {
        public View view;
        public int width;
        public int mainLeft;
        public int mainRight;
    }
}
