package com.inappstory.sdk.stories.ui.widgets.readerscreen.storiespager;

import android.content.Context;

public interface SimpleStoriesView {
    void pauseVideo();
    void playVideo();
    void restartVideo();
    void stopVideo();
    void swipeUp();
    void loadJsApiResponse(String result, String cb);
    void resumeVideo();
    Context getContext();
    void changeSoundStatus();
    void cancelDialog(String id);
    void sendDialog(String id, String data);
    void destroyView();
    float getCoordinate();
    void shareComplete(String stId, boolean success);
    void freezeUI();
    void setStoriesView(SimpleStoriesView storiesView);
    void checkIfClientIsSet();
    void goodsWidgetComplete(String widgetId);
    StoriesViewManager getManager();
}
