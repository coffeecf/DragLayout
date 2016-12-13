package com.example.draglayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by Administrator on 2016/12/13.
 */

public class BackFrameLayout extends FrameLayout {
    private DragLayout dl;

    public BackFrameLayout(Context context) {
        super(context);
    }

    public BackFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BackFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (dl == null) {
            dl = (DragLayout) this.getParent();
        }
        return dl.getStatus() != DragLayout.Status.Close || super.onInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (dl.getStatus() != DragLayout.Status.Close) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                dl.close();
            }
            return true;
        }
        return super.onTouchEvent(event);
    }
}
