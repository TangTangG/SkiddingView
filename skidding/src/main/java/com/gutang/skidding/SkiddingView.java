package com.gutang.skidding;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.List;

/**
 * Created by TCG on 2017/8/21.
 */

public abstract class SkiddingView extends LinearLayout {

    public static final int NONE = 0x000;
    public static final int LEFT = 0x001;
    public static final int RIGHT = 0x002;

    private View contentView;
    private LinearLayout actView;
    private SimpleDragHelper dragHelper = null;
    private int dragDisX = 0;
    private int dragMin = 0;
    private OnActionViewClick actionViewClick = null;

    private SkiddingView(Context context) {
        super(context);
        init(context);
    }

    private SkiddingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private SkiddingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private SkiddingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        if (contentView == null) {
            contentView = contentView();
        }
        if (contentView == null) {
            throw new NullPointerException("contentView must be initialized");
        }

        setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        setOrientation(HORIZONTAL);
        setElevation(10);
        switch (mode()) {
            case LEFT:
                initializeActions();
                addView(actView);
                addView(contentView);
                break;
            case RIGHT:
                initializeActions();
                addView(contentView);
                addView(actView);
                break;
            case NONE:
            default:
                addView(contentView);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public static SkiddingView build(Context context, final View contentView, final List<View> actionViews,
                                     final int mode, final SkiddingItemObserver observer) {
        return new SkiddingView(context) {

            @Override
            public List<View> actionItemViews() {
                return actionViews;
            }

            @Override
            public View contentView() {
                return contentView;
            }

            @Override
            public int mode() {
                return mode;
            }

            @Override
            public SkiddingItemObserver observer() {
                return observer;
            }
        };
    }

    public View getContentView() {
        return contentView;
    }

    public abstract List<View> actionItemViews();

    public abstract View contentView();

    public abstract int mode();

    public abstract SkiddingItemObserver observer();

    public void setActionViewClick(OnActionViewClick actionViewClick) {
        this.actionViewClick = actionViewClick;
    }

    public interface OnActionViewClick{
        void actionViewClick(View view, int pos);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int contentWidth = contentView.getMeasuredWidth();
        int contentHeight = contentView.getMeasuredHeight();
        contentView.layout(0, 0, contentWidth, contentHeight);

        boolean leftMode = mode() == LEFT;
        int actWidth = actView.getMeasuredWidth();
        int actHeight = actView.getMeasuredHeight();
        actView.layout(leftMode ? -actWidth : contentWidth, 0,
                leftMode ? 0 : actWidth + contentWidth, actHeight);
    }

    public void notifyRemoveActionView(int pos){
        View posChild = actView.getChildAt(pos);
        ViewGroup.LayoutParams posChildLayoutParams = posChild.getLayoutParams();
        int posWidth = posChildLayoutParams.width;
        actView.removeViewAt(pos);
        ViewGroup.LayoutParams layoutParams = actView.getLayoutParams();
        int newWidth = layoutParams.width - posWidth;
        layoutParams.width = newWidth;
        actView.setLayoutParams(layoutParams);
        dragMin -= posWidth;
        actView.invalidate();
    }

    /**
     * 初始化操作列表
     */
    private void initializeActions() {
        Context context = getContext();
        actView = new LinearLayout(context);
        List<View> views = actionItemViews();
        actView.setGravity(Gravity.CENTER);
        actView.setOrientation(HORIZONTAL);
        actView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        int index = 0;
        int widthTotal = 0;
        for (View view : views) {
            final int pos = index;
            int width = view.getLayoutParams().width;
            widthTotal += width;
            dragMin = dragMin == 0 ? width : Math.min(dragMin, width);
            OnClickListener onClickListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (actionViewClick != null){
                        actionViewClick.actionViewClick(v,pos);
                    }
                }
            };
            view.setOnClickListener(onClickListener);
            actView.addView(view);
            index++;
        }
        dragMin--;
        actView.setLayoutParams(new LayoutParams(widthTotal, ViewGroup.LayoutParams.MATCH_PARENT));
        dragHelper = SimpleDragHelper.create(this, new DragCallback());
        actView.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        dragHelper.processTouchEvent(event);
//        EventChecker.add(event);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return dragHelper.shouldInterceptTouchEvent(event)
                || super.onInterceptTouchEvent(event);
    }

    @Override
    public void computeScroll() {
        if (dragHelper.continueSettling(true)) {
            postInvalidateOnAnimation();
        }
    }

    public void close() {
        int contentViewWidth = contentView.getMeasuredWidth();
        int dragDisMax = actView.getMeasuredWidth();
        boolean leftMode = mode() == LEFT;
        dragHelper.smoothSlideViewTo(actView, leftMode ?
                -dragDisMax : (dragDisMax + contentViewWidth), 0);
        dragHelper.smoothSlideViewTo(contentView, 0, 0);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public class DragCallback extends SimpleDragHelper.Callback {

        boolean settleToOpen = false;
        private int[] contentLeftArr = new int[]{0, 0, 0};

        @Override
        public boolean tryCaptureView(View view, int i) {
            boolean show = mode() != NONE;
            if (show) {
                contentLeftArr = new int[]{0, 0, 0};
                contentLeftArr[0] = contentView.getLeft();
                settleToOpen = false;
            }
            return show;
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            int dragDisMax = getViewHorizontalDragRange(child);
            int newLeft = 0;
            if (child.equals(contentView)) {
                final int leftBound = getPaddingLeft();
                final int minLeftBound = -leftBound - dragDisMax;
                newLeft = Math.min(Math.max(minLeftBound, left), 0);
            } else {
                int contentWidth = contentView.getMeasuredWidth();
                final int minLeftBound = getPaddingLeft() + contentWidth - dragDisMax;
                final int maxLeftBound = getPaddingLeft() + contentWidth + getPaddingRight();
                newLeft = Math.min(Math.max(left, minLeftBound), maxLeftBound);
            }
            return newLeft;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            dragDisX = left;
            if (changedView.equals(contentView)) {
                actView.offsetLeftAndRight(dx);
            } else {
                contentView.offsetLeftAndRight(dx);
            }
            invalidate();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            int contentLeft = contentView.getLeft();
            int leftOffset = 0;
            boolean leftMode = mode() == LEFT;
            // TODO: 2017/8/25 在左边模式的时候 这个地方的判断无法触发第一次拉起 在右边模式的情况下就可以
            if (leftMode) {
                leftOffset = settleToOpen ? contentLeftArr[0] - contentLeft
                        : contentLeft - contentLeftArr[0];
            } else {
                leftOffset = settleToOpen ? contentLeft - contentLeftArr[0]
                        : contentLeftArr[0] - contentLeft;
            }
            contentLeftArr[1] = contentLeft;
            contentLeftArr[2] = leftOffset;
            settleToOpen = leftOffset > dragMin & !settleToOpen;
            int contentViewWidth = contentView.getMeasuredWidth();
            int dragDisMax = getViewHorizontalDragRange(releasedChild);
            if (settleToOpen) {
                dragHelper.smoothSlideViewTo(contentView, leftMode ? dragDisMax : -dragDisMax, 0);
                dragHelper.smoothSlideViewTo(actView, leftMode ? 0 : contentViewWidth - dragDisMax, 0);
            } else {
                dragHelper.smoothSlideViewTo(actView, leftMode ?
                        -dragDisMax : (dragDisMax + contentViewWidth), 0);
                dragHelper.smoothSlideViewTo(contentView, 0, 0);
//                EventChecker.handlerEvent2Parent(SkiddingView.this);
            }
            ViewCompat.postInvalidateOnAnimation(SkiddingView.this);

            observer().report(SkiddingView.this, settleToOpen);
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return actView.getMeasuredWidth();
        }
    }

    private static class Event {

        int action;
        Event next;
        Event last;
        MotionEvent motionEvent;

        private Event() {

        }

        public static Event obtainNew(MotionEvent e) {
            Event event = new Event();
            event.action = e.getAction();
            event.next = null;
            event.motionEvent = e;
            return event;
        }

        public Event newChild(MotionEvent e) {
            Event event = new Event();
            event.action = e.getAction();
            event.next = null;
            if (last != null){
                last.next = event;
            }
            last = event;
            return event;
        }

        public void forEach(Consumer c) {
            Event event = this;
            while (event != null) {
                c.each(event);
                event = event.next;
            }
        }

        interface Consumer {
            void each(Event event);
        }
    }

    private static class EventChecker {
        public final static int IDLE = 0;
        public final static int DRAG = 1;
        public final static int CLICK = 2;
        private static Event event;

        public static void add(MotionEvent e) {
            event = event == null ? Event.obtainNew(e) : event.newChild(e);
        }

        private static int check(){
            int moveCount = 0;
            if (event == null){
                return IDLE;
            }
            Event e = event;
            while (e != null) {
                if (e.action == MotionEvent.ACTION_MOVE){
                    moveCount++;
                }
                if (moveCount > 4){
                    break;
                }
                e = e.next;
            }

            if (moveCount > 4){
                return DRAG;
            }
            return CLICK;
        }

        public static void handlerEvent2Parent(SkiddingView view) {
            switch (check()){
                case DRAG:
                    break;
                case CLICK:
                   /* // TODO: 2017/9/1 check here  java.lang.NullPointerException: Attempt to invoke virtual method 'void android.view.MotionEvent.setAction(int)' on a null object reference
                    //do handle
                    final ExpandableListView listView = (ExpandableListView) view.getParent();
                    if (listView == null){
                        break;
                    }
                    final MotionEvent motionEvent = event.motionEvent;
                    event.forEach(new Event.Consumer() {
                        @Override
                        public void each(Event event) {
                            motionEvent.setAction(event.action);
                            listView.onTouchEvent(motionEvent);
                        }
                    });
                    event = null;*/
                    break;
                case IDLE:
                    break;
            }
        }
    }
}
