package com.inappstory.sdk.stories.ui.widgets.readerscreen.progresstimeline;

import android.os.Build;

public class TimelineManager {
    public void setTimeline(Timeline timeline) {
        this.timeline = timeline;
    }

    public static TimelineManager INSTANCE = new TimelineManager();

    public static TimelineManager getInstance() {
        if (INSTANCE == null) INSTANCE = new TimelineManager();
        return INSTANCE;
    }

    Timeline timeline;

    public void start(int ind) {
        if (ind < 0) return;
        if (ind > timeline.slidesCount) return;
        for (int i = 0; i < ind; i++) {
            timeline.progressBars.get(i).setMax();
        }
        for (int i = ind+1; i < timeline.slidesCount; i++) {
            timeline.progressBars.get(i).clear();
        }
        timeline.progressBars.get(ind).setMin();
        timeline.setActive(ind);
        timeline.curAnimation.start();
    }

    public void stop() {

    }


    private long mAnimationTime;

    public void pause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            timeline.curAnimation.pause();
        } else {
            mAnimationTime = timeline.curAnimation.getCurrentPlayTime();
            timeline.curAnimation.cancel();
        }
    }

    public void resume() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            timeline.curAnimation.resume();
        } else {
            timeline.curAnimation.start();
            timeline.curAnimation.setCurrentPlayTime(mAnimationTime);
        }
    }

    public void next() {
        start(timeline.activeInd+1);
    }

    public void prev() {
        start(timeline.activeInd-1);
    }
}
