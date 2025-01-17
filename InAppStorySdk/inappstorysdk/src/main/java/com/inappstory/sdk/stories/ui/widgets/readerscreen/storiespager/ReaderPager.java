package com.inappstory.sdk.stories.ui.widgets.readerscreen.storiespager;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

import com.inappstory.sdk.R;
import com.inappstory.sdk.stories.ui.reader.StoriesFragment;
import com.inappstory.sdk.stories.ui.widgets.viewpagertransforms.CubeTransformer;
import com.inappstory.sdk.stories.ui.widgets.viewpagertransforms.DepthTransformer;

public class ReaderPager extends ViewPager {
    public void setHost(StoriesFragment host) {
        this.host = host;
    }

    StoriesFragment host;

    public ReaderPager(@NonNull Context context) {
        super(context);
    }

    public ReaderPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean canUseNotLoaded;

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int res = super.getChildDrawingOrder(childCount, i);
        return childCount - res - 1;
    }

    private float pressedX;
    private float pressedY;
    boolean startMove;

    public static PageTransformer cubeTransformer = new CubeTransformer();
    public static PageTransformer depthTransformer = new DepthTransformer();

    public void setTransformAnimation(int transformAnimation) {
        this.transformAnimation = transformAnimation;
    }

    private int transformAnimation;
    boolean closeOnSwipe;

    public void setParameters(int transformAnimation) {
        this.transformAnimation = transformAnimation;
        init();
    }

    public void init(@Nullable AttributeSet attrs) {
        if (attrs != null) {
            TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ReaderPager);
        }
    }

    public void init() {
        switch (transformAnimation) {
            case 0:
                setChildrenDrawingOrderEnabled(false);
                setPageTransformer(true, cubeTransformer);
                break;
            case 1:
                setChildrenDrawingOrderEnabled(true);
                setPageTransformer(true, depthTransformer);
                break;
            default:
                setChildrenDrawingOrderEnabled(false);
                setPageTransformer(true, cubeTransformer);
                break;

        }
    }

    public void pageScrolled(float positionOffset) {
        if (positionOffset == 0f) {
            cubeAnimation = false;
            requestDisallowInterceptTouchEvent(false);
        } else {
            cubeAnimation = true;
            requestDisallowInterceptTouchEvent(true);
        }
    }

    public boolean cubeAnimation = false;


    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (cubeAnimation) {
            return true;
        }
        // Story st = InAppStoryService.getInstance().getDownloadManager().getStoryById(InAppStoryService.getInstance().getCurrentId());
        float pressedEndX = 0f;
        float pressedEndY = 0f;
        boolean distance = false;
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            pressedX = motionEvent.getX();
            pressedY = motionEvent.getY();
            //CsEventBus.getDefault().post(new WidgetTapEvent());
        } else if (!(motionEvent.getAction() == MotionEvent.ACTION_UP
                || motionEvent.getAction() == MotionEvent.ACTION_CANCEL)) {
            pressedEndX = motionEvent.getX() - pressedX;
            pressedEndY = motionEvent.getY() - pressedY;
            distance = (float) Math.sqrt(pressedEndX * pressedEndX + pressedEndY * pressedEndY) > 20;
        } else if (motionEvent.getAction() == MotionEvent.ACTION_UP
                || motionEvent.getAction() == MotionEvent.ACTION_CANCEL) {
            pressedEndY = motionEvent.getY() - pressedY;
            pressedEndX = motionEvent.getX() - pressedX;
            if (pressedEndY > 400) {
                host.swipeDownEvent(getCurrentItem());
                // CsEventBus.getDefault().post(new SwipeDownEvent());
                return true;
            }
            if (pressedEndY < -400) {
                host.swipeUpEvent(getCurrentItem());
                return true;
            }
            if (getCurrentItem() == 0 &&
                    pressedEndX * pressedEndX > pressedEndY * pressedEndY &&
                    pressedEndX > 300) {
                host.swipeRightEvent(getCurrentItem());
                return true;
            }

            if (getCurrentItem() == getAdapter().getCount() - 1 &&
                    pressedEndX * pressedEndX > pressedEndY * pressedEndY &&
                    pressedEndX < -300) {
                host.swipeLeftEvent(getCurrentItem());
                return true;
            }
        }
        if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            if (pressedEndX * pressedEndX < pressedEndY * pressedEndY
                    && (float) Math.sqrt(pressedEndY * pressedEndY) > 20) {
                return false;
            }
            if (distance && !(
                    getCurrentItem() == 0 &&
                            pressedEndX * pressedEndX > pressedEndY * pressedEndY &&
                            pressedEndX > 0)
                    &&
                    !(getCurrentItem() == getAdapter().getCount() - 1 &&
                            pressedEndX * pressedEndX > pressedEndY * pressedEndY &&
                            pressedEndX < 0)) {
                return true;
            } else {
                return false;
            }
        }
        return super.onInterceptTouchEvent(motionEvent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        return super.onTouchEvent(motionEvent);
    }
}
