package com.inappstory.sdk.game.reader;

import android.content.Context;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.stories.statistic.StatisticManager;
import com.inappstory.sdk.stories.utils.KeyValueStorage;


public class GameJSInterface {
    Context mContext;
    int lindex;
    String storyId;
    GameManager manager;


    /**
     * Instantiate the interface and set the context
     */
    GameJSInterface(Context c, int lindex, String storyId, GameManager gameManager) {
        this.lindex = lindex;
        this.storyId = storyId;
        mContext = c;
        this.manager = gameManager;
    }

    /**
     * Show a toast from the web page
     */


    @JavascriptInterface
    public void storySetLocalData(int storyId, String data, boolean sendToServer) {
        manager.storySetData(data, sendToServer);
    }

    @JavascriptInterface
    public int pausePlaybackOtherApp() {
        return manager.pausePlaybackOtherApp();
    }

    @JavascriptInterface
    public String storyGetLocalData(int storyId) {
        String res = KeyValueStorage.getString("story" + storyId
                + "__" + InAppStoryService.getInstance().getUserId());
        return res == null ? "" : res;
    }

    @JavascriptInterface
    public void gameLoaded(String data) {
        manager.gameLoaded(data);
    }

    @JavascriptInterface
    public void sendApiRequest(String data) {
        manager.sendApiRequest(data);
    }

    @JavascriptInterface
    public void gameComplete(String data) {
        manager.gameCompleted(data, null, null);
    }

    @JavascriptInterface
    public void setAudioManagerMode(String mode) {
        manager.setAudioManagerMode(mode);
    }

    @JavascriptInterface
    public void gameComplete(String data, String eventData, String deeplink) {
        manager.gameCompleted(data, deeplink, eventData);
    }

    @JavascriptInterface
    public void gameStatisticEvent(String name, String data) {
        StatisticManager.getInstance().sendGameEvent(name, data);
    }

    @JavascriptInterface
    public void showGoodsWidget(String id, String skus) {
        manager.showGoods(skus, id);
    }

    @JavascriptInterface
    public void emptyLoaded() {
    }


    @JavascriptInterface
    public void share(String id, String data) {
        manager.shareData(id, data);
    }
}
