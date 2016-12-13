package com.example.draglayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * Created by Administrator on 2016/9/30.
 */

public class FrontLinearLayout extends LinearLayout {

    private DragLayout dl;

    public FrontLinearLayout(Context context) {
        super(context);
    }

    public FrontLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FrontLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (dl == null){
            dl = (DragLayout)this.getParent();
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


