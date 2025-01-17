package com.inappstory.sdk.stories.managers;

import android.os.Handler;
import android.util.Log;

import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.stories.statistic.StatisticManager;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.statistic.OldStatisticManager;
import com.inappstory.sdk.stories.ui.widgets.readerscreen.storiespager.ReaderPageManager;

public class TimerManager {
    private Handler timerHandler = new Handler();
    private long timerStart;

    public void setTimerDuration(long timerDuration) {
        this.timerDuration = timerDuration;
    }

    private long timerDuration;
    private long totalTimerDuration;
    private long pauseShift;

    public void setPageManager(ReaderPageManager pageManager) {
        this.pageManager = pageManager;
    }

    ReaderPageManager pageManager;

    Runnable timerTask = new Runnable() {
        @Override
        public void run() {
            if (timerDuration > 0 && System.currentTimeMillis() - timerStart >= timerDuration) {
                timerHandler.removeCallbacks(timerTask);
                pauseShift = 0;
                if (pageManager != null)
                    pageManager.nextSlide();
                return;
            }
            timerHandler.postDelayed(timerTask, 50);
        }
    };

    public void resumeLocalTimer() {
        startTimer(timerDuration - pauseShift, false);
    }

    public long startPauseTime;


    public long pauseTime = 0;

    public void resumeTimer() {
        StatisticManager.getInstance().cleanFakeEvents();
        resumeLocalTimer();
        if (OldStatisticManager.getInstance().currentEvent == null) return;
        OldStatisticManager.getInstance().currentEvent.eventType = 1;
        OldStatisticManager.getInstance().currentEvent.timer = System.currentTimeMillis();
        pauseTime += System.currentTimeMillis() - startPauseTime;
        if (StatisticManager.getInstance() != null && StatisticManager.getInstance().currentState != null)
            StatisticManager.getInstance().currentState.storyPause = pauseTime;
        startPauseTime = 0;
    }

    public void stopTimer() {
        try {
            timerHandler.removeCallbacks(timerTask);
        } catch (Exception e) {

        }
    }

    public void resumeTimer(int timer) {
        StatisticManager.getInstance().cleanFakeEvents();
        startTimer(timer, false);
        if (OldStatisticManager.getInstance().currentEvent == null) return;
        OldStatisticManager.getInstance().currentEvent.eventType = 1;
        OldStatisticManager.getInstance().currentEvent.timer = System.currentTimeMillis();
        pauseTime += System.currentTimeMillis() - startPauseTime;
        StatisticManager.getInstance().currentState.storyPause = pauseTime;
        startPauseTime = 0;
    }


    public void startTimer(long timerDuration, boolean clearDuration) {
        if (timerDuration == 0) {
            try {
                timerHandler.removeCallbacks(timerTask);
                this.timerDuration = timerDuration;
                if (clearDuration)
                    this.totalTimerDuration = timerDuration;
            } catch (Exception e) {

            }
            return;
        }
        if (timerDuration < 0) {
            return;
        }
        pauseShift = 0;
        timerStart = System.currentTimeMillis();
        this.timerDuration = timerDuration;
        try {
            timerHandler.removeCallbacks(timerTask);
        } catch (Exception e) {

        }
        timerHandler.post(timerTask);
    }

    public void restartTimer(long duration) {
        startTimer(duration, true);
    }

    public void setCurrentDuration(int currentDuration) {
        this.currentDuration = currentDuration;
    }

    int currentDuration;

    public void startCurrentTimer() {
        if (currentDuration != 0)
            startTimer(currentDuration, false);
    }

    public void pauseLocalTimer() {
        try {
            timerHandler.removeCallbacks(timerTask);
        } catch (Exception e) {

        }
        pauseShift = (System.currentTimeMillis() - timerStart);
    }

    public void pauseTimer() {
        Story story = InAppStoryService.getInstance().getDownloadManager()
                .getStoryById(InAppStoryService.getInstance().getCurrentId());
        if (story != null) {
            StatisticManager.getInstance().addFakeEvents(story.id, story.lastIndex, story.getSlidesCount());
        }
        pauseLocalTimer();
        startPauseTime = System.currentTimeMillis();
        OldStatisticManager.getInstance().closeStatisticEvent(null, true);
        OldStatisticManager.getInstance().sendStatistic();
        OldStatisticManager.getInstance().eventCount++;
    }

}
