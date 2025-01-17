package com.inappstory.sdk.stories.ui.reader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.inappstory.sdk.InAppStoryManager;
import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.eventbus.CsEventBus;
import com.inappstory.sdk.stories.callbacks.CallbackManager;
import com.inappstory.sdk.stories.statistic.ProfilingManager;
import com.inappstory.sdk.stories.statistic.StatisticManager;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.statistic.OldStatisticManager;
import com.inappstory.sdk.stories.outerevents.ShowStory;
import com.inappstory.sdk.stories.ui.ScreensManager;
import com.inappstory.sdk.stories.ui.widgets.readerscreen.storiespager.ReaderPageManager;
import com.inappstory.sdk.stories.utils.ShowGoodsCallback;
import com.inappstory.sdk.stories.utils.Sizes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ReaderManager {

    public void close() {
        parentFragment.getActivity().finish();
    }


    public void swipeUp(int position) {
        int storyId = storiesIds.get(position);
        ReaderPageManager manager = getSubscriberByStoryId(storyId);
        if (manager != null)
            manager.swipeUp();
    }

    public void hideGoods() {
        ScreensManager.getInstance().hideGoods();
    }

    public void showGoods(String skusString, String widgetId, ShowGoodsCallback showGoodsCallback, int storyId, int slideIndex) {
        ScreensManager.getInstance().showGoods(skusString,
                parentFragment.getActivity(), showGoodsCallback, false,
                widgetId, storyId, slideIndex);
    }

    public void pause() {
        if (parentFragment != null)
            parentFragment.pause();
    }

    public void resume() {
        if (parentFragment != null)
            parentFragment.resume();
    }

    public void gameComplete(String data, int storyId, int slideIndex) {
        getSubscriberByStoryId(storyId).gameComplete(data);
    }

    public void showSingleStory(final int storyId, final int slideIndex) {

        OldStatisticManager.getInstance().addLinkOpenStatistic();
        if (storiesIds.contains(storyId)) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Story story = InAppStoryService.getInstance().getDownloadManager()
                            .getStoryById(storyId);
                    if (story == null) return;
                    if (story.getSlidesCount() <= slideIndex) {
                        story.setLastIndex(0);
                    } else {
                        story.setLastIndex(slideIndex);
                    }
                    parentFragment.setCurrentItem(storiesIds.indexOf(storyId));
                }
            });
        } else {
            InAppStoryManager.getInstance().showStoryWithSlide(storyId + "", parentFragment.getContext(), slideIndex, parentFragment.readerSettings);
        }
    }

    void sendStat(int position, int source) {
        if (lastPos < position && lastPos > -1) {
            sendStatBlock(true, StatisticManager.NEXT, storiesIds.get(position));
        } else if (lastPos > position && lastPos > -1) {
            sendStatBlock(true, StatisticManager.PREV, storiesIds.get(position));
        } else if (lastPos == -1) {
            String whence = StatisticManager.DIRECT;
            switch (source) {
                case 1:
                    whence = StatisticManager.ONBOARDING;
                    break;
                case 2:
                    whence = StatisticManager.LIST;
                    break;
                case 3:
                    whence = StatisticManager.FAVORITE;
                    break;
                default:
                    break;
            }
            sendStatBlock(false, whence, storiesIds.get(position));
        }
    }

    void newStoryTask(int pos) {
        ArrayList<Integer> adds = new ArrayList<>();
        if (storiesIds.size() > 1) {
            if (pos == 0) {
                adds.add(storiesIds.get(pos + 1));
            } else if (pos == storiesIds.size() - 1) {
                adds.add(storiesIds.get(pos - 1));
            } else {
                adds.add(storiesIds.get(pos + 1));
                adds.add(storiesIds.get(pos - 1));
            }
        }
        if (InAppStoryService.isNull()) return;
        InAppStoryService.getInstance().getDownloadManager().changePriority(storiesIds.get(pos), adds);
        InAppStoryService.getInstance().getDownloadManager().addStoryTask(storiesIds.get(pos), adds);

    }

    void restartCurrentStory() {
        getCurrentSubscriber().restartSlide();
    }

    void onPageSelected(int source, int position) {
        sendStat(position, source);

        lastPos = position;

        currentStoryId = storiesIds.get(position);
        Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(currentStoryId);
        if (story == null) return;
        if (firstStoryId > 0 && startedSlideInd > 0) {
            if (story.getSlidesCount() > startedSlideInd)
                story.lastIndex = startedSlideInd;
            cleanFirst();
        }
        if (story != null) {
            CsEventBus.getDefault().post(new ShowStory(story.id, story.title, story.tags,
                    story.getSlidesCount(), source));

            if (CallbackManager.getInstance().getShowStoryCallback() != null) {
                CallbackManager.getInstance().getShowStoryCallback().showStory(story.id, story.title,
                        story.tags, story.getSlidesCount(),
                        CallbackManager.getInstance().getSourceFromInt(source));
            }
        }
        final int pos = position;

        ProfilingManager.getInstance().addTask("slide_show",
                currentStoryId + "_" +
                        story.lastIndex);
        InAppStoryService.getInstance().getListReaderConnector().changeStory(currentStoryId);
        if (Sizes.isTablet()) {
            if (parentFragment.getParentFragment() instanceof StoriesDialogFragment) {
                ((StoriesDialogFragment) parentFragment.getParentFragment()).changeStory(position);
            }
        }
        InAppStoryService.getInstance().setCurrentId(currentStoryId);
        currentSlideIndex =
                story.lastIndex;
        parentFragment.showGuardMask(600);
        new Thread(new Runnable() {
            @Override
            public void run() {
                newStoryTask(pos);

                if (storiesIds != null && storiesIds.size() > pos) {
                    changeStory();
                }


            }
        }).start();
    }

    public void storyClick() {
        parentFragment.showGuardMask(300);
    }

    void changeStory() {
        OldStatisticManager.getInstance().addStatisticBlock(currentStoryId,
                currentSlideIndex);

        ArrayList<Integer> lst = new ArrayList<>();
        lst.add(currentStoryId);
        OldStatisticManager.getInstance().previewStatisticEvent(lst);
        synchronized (subscribers) {
            for (ReaderPageManager pageManager : subscribers) {
                if (pageManager.getStoryId() != currentStoryId) {
                    pageManager.stopStory(currentStoryId);
                } else {
                    pageManager.setSlideIndex(currentSlideIndex);
                    pageManager.storyOpen(currentStoryId);
                }
            }
        }
    }

    int lastPos = -1;

    private void sendStatBlock(boolean hasCloseEvent, String whence, int id) {
        Story story2 = InAppStoryService.getInstance().getDownloadManager().getStoryById(id);
        StatisticManager.getInstance().sendCurrentState();
        if (hasCloseEvent) {
            Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(storiesIds.get(lastPos));
            if (story != null)
                StatisticManager.getInstance().sendCloseStory(story.id, whence, story.lastIndex, story.getSlidesCount());
        }
        StatisticManager.getInstance().sendViewStory(id, whence);
        StatisticManager.getInstance().sendOpenStory(id, whence);
        if (story2 != null)
            StatisticManager.getInstance().createCurrentState(story2.id, story2.lastIndex);
    }

    public void shareComplete() {
        getSubscriberByStoryId(ScreensManager.getInstance().getTempShareStoryId()).
                shareComplete("" + ScreensManager.getInstance().getTempShareId(),
                        true);
    }

    void resumeWithShareId() {
        ScreensManager.getInstance().setTempShareStoryId(0);
        ScreensManager.getInstance().setTempShareId(null);
        if (ScreensManager.getInstance().getOldTempShareId() != null) {
            getSubscriberByStoryId(ScreensManager.getInstance().getOldTempShareStoryId()).
                    shareComplete("" + ScreensManager.getInstance().getOldTempShareStoryId(),
                            true);
           /* CsEventBus.getDefault().post(new ShareCompleteEvent(
                    ScreensManager.getInstance().getOldTempShareStoryId(),
                    ScreensManager.getInstance().getOldTempShareId(), true));*/
        }
        ScreensManager.getInstance().setOldTempShareStoryId(0);
        ScreensManager.getInstance().setOldTempShareId(null);
    }

    void saveShareId() {

    }

    public int getCurrentStoryId() {
        return currentStoryId;
    }

    public void setCurrentStoryId(int currentStoryId) {
        this.currentStoryId = currentStoryId;
    }

    public int getCurrentSlideIndex() {
        return currentSlideIndex;
    }

    public void setCurrentSlideIndex(int currentSlideIndex) {
        this.currentSlideIndex = currentSlideIndex;
    }

    public List<Integer> getStoriesIds() {
        return storiesIds;
    }

    public void setStoriesIds(List<Integer> storiesIds) {
        this.storiesIds = storiesIds;
    }

    private int currentStoryId;
    private int currentSlideIndex;
    private List<Integer> storiesIds;

    public void setParentFragment(StoriesFragment parentFragment) {
        this.parentFragment = parentFragment;
    }


    public int startedSlideInd;
    public int firstStoryId = -1;

    public void cleanFirst() {
        Bundle bundle = parentFragment.getArguments();
        bundle.remove("slideIndex");
        parentFragment.setArguments(bundle);
        startedSlideInd = 0;
        firstStoryId = -1;
    }

    private StoriesFragment parentFragment;
    private HashSet<ReaderPageManager> subscribers = new HashSet<>();

    public void addSubscriber(ReaderPageManager manager) {
        synchronized (subscribers) {
            for (ReaderPageManager readerPageManager : subscribers) {
                if (readerPageManager.getStoryId() == manager.getStoryId()) return;
            }
            subscribers.add(manager);
        }
    }

    public void removeSubscriber(ReaderPageManager manager) {
        synchronized (subscribers) {
            subscribers.remove(manager);
        }
    }

    private ReaderPageManager getSubscriberByStoryId(int storyId) {
        for (ReaderPageManager subscriber : subscribers) {
            if (subscriber.getStoryId() == storyId)
                return subscriber;
        }
        return null;
    }

    private ReaderPageManager getCurrentSubscriber() {
        return getSubscriberByStoryId(currentStoryId);
    }

    public void nextStory() {
        parentFragment.nextStory();
    }

    public void prevStory() {
        parentFragment.prevStory();
    }


    public void defaultTapOnLink(String url) {
        parentFragment.defaultUrlClick(url);
    }

    public void pauseCurrent(boolean withBackground) {
        if (getCurrentSubscriber() != null)
            getCurrentSubscriber().pauseSlide(withBackground);
        StatisticManager.getInstance().pauseStoryEvent(withBackground);
    }

    public void resumeCurrent(boolean withBackground) {
        if (getCurrentSubscriber() != null)
            getCurrentSubscriber().resumeSlide(withBackground);
        if (withBackground && OldStatisticManager.getInstance() != null) {
            OldStatisticManager.getInstance().refreshTimer();
        }
        StatisticManager.getInstance().resumeStoryEvent(withBackground);
    }

    public void swipeUp() {

    }

    public void swipeDown() {

    }

    public void swipeLeft() {

    }

    public void swipeRight() {

    }

    public void slideLoadedInCache(int storyId, int slideIndex) {
        ReaderPageManager pageManager = getSubscriberByStoryId(storyId);
        if (pageManager != null) pageManager.slideLoadedInCache(slideIndex);
    }
}
