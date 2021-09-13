package com.inappstory.sdk.game.reader;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;

import com.inappstory.sdk.InAppStoryService;
import com.inappstory.sdk.eventbus.CsEventBus;
import com.inappstory.sdk.game.loader.GameLoadCallback;
import com.inappstory.sdk.game.loader.GameLoader;
import com.inappstory.sdk.network.JsonParser;
import com.inappstory.sdk.network.NetworkCallback;
import com.inappstory.sdk.network.NetworkClient;
import com.inappstory.sdk.network.Response;
import com.inappstory.sdk.stories.api.models.ShareObject;
import com.inappstory.sdk.stories.api.models.StatisticSession;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.api.models.WebResource;
import com.inappstory.sdk.stories.api.models.callbacks.OpenSessionCallback;
import com.inappstory.sdk.stories.callbacks.CallbackManager;
import com.inappstory.sdk.stories.outerevents.CallToAction;
import com.inappstory.sdk.stories.outerevents.ClickOnButton;
import com.inappstory.sdk.stories.outerevents.FinishGame;
import com.inappstory.sdk.stories.ui.ScreensManager;
import com.inappstory.sdk.stories.utils.KeyValueStorage;
import com.inappstory.sdk.stories.utils.SessionManager;
import com.inappstory.sdk.stories.utils.TaskRunner;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Map;

import static com.inappstory.sdk.network.JsonParser.toMap;

public class GameManager {
    String storyId;
    String path;
    String resources;
    String loaderPath;
    String title;
    String tags;
    int index;
    int slidesCount;

    boolean gameLoaded;
    String gameConfig;

    GameLoadCallback callback;

    public GameManager(GameActivity host) {
        this.host = host;
    }

    void loadGame() {
        ArrayList<WebResource> resourceList = new ArrayList<>();

        if (resources != null) {
            resourceList = JsonParser.listFromJson(resources, WebResource.class);
        }

        String[] urlParts = urlParts(path);
        GameLoader.getInstance().downloadAndUnzip(host, resourceList, path, urlParts[0], callback);
    }

    private String[] urlParts(String url) {
        String[] parts = url.split("/");
        String fName = parts[parts.length - 1].split("\\.")[0];
        return fName.split("_");
    }


    void storySetData(String data, boolean sendToServer) {
        KeyValueStorage.saveString("story" + storyId
                + "__" + InAppStoryService.getInstance().getUserId(), data);

        if (!InAppStoryService.getInstance().getSendStatistic()) return;
        if (sendToServer) {
            NetworkClient.getApi().sendStoryData(storyId, data, StatisticSession.getInstance().id)
                    .enqueue(new NetworkCallback<Response>() {
                        @Override
                        public void onSuccess(Response response) {

                        }

                        @Override
                        public Type getType() {
                            return null;
                        }
                    });
        }
    }

    GameActivity host;

    void gameCompleted(String gameState, String link, String eventData) {
        CsEventBus.getDefault().post(new FinishGame(Integer.parseInt(storyId), title, tags,
                slidesCount, index, eventData));
        host.gameCompleted(gameState, link);
    }

    void tapOnLink(String link) {
        Story story = InAppStoryService.getInstance().getDownloadManager().getStoryById(
                Integer.parseInt(storyId));
        CsEventBus.getDefault().post(new ClickOnButton(story.id, story.title,
                story.tags, story.slidesCount, story.lastIndex,
                link));
        int cta = CallToAction.GAME;
        CsEventBus.getDefault().post(new CallToAction(story.id, story.title,
                story.tags, story.slidesCount, story.lastIndex,
                link, cta));
        // OldStatisticManager.getInstance().addLinkOpenStatistic();
        if (CallbackManager.getInstance().getUrlClickCallback() != null) {
            CallbackManager.getInstance().getUrlClickCallback().onUrlClick(
                    link
            );
        } else {
            if (!InAppStoryService.isConnected()) {
                return;
            }
            host.tapOnLinkDefault(link);
        }
    }

    int pausePlaybackOtherApp() {
        AudioManager am = (AudioManager) host.getSystemService(Context.AUDIO_SERVICE);
        return am.requestAudioFocus(host.audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
    }

    void gameLoaded(String data) {
        GameLoadedConfig config = JsonParser.fromJson(data, GameLoadedConfig.class);
        host.gameReaderGestureBack = config.backGesture;
        host.showClose = config.showClose;
        gameLoaded = true;
        host.updateUI();
    }

    void sendApiRequest(String data) {
        GameRequestConfig config = JsonParser.fromJson(data, GameRequestConfig.class);
        Map<String, String> headers = null;
        if (config.headers != null && !config.headers.isEmpty()) {
            headers = toMap(config.headers);
        }
        Map<String, String> getParams = null;
        if (config.params != null && !config.params.isEmpty()) {
            getParams = toMap(config.params);
        }
        checkAndSendRequest(config.method, config.url, headers, getParams,
                config.data, config.id, config.cb);
    }

    public void checkAndSendRequest(final String method,
                                    final String path,
                                    final Map<String, String> headers,
                                    final Map<String, String> getParams,
                                    final String body,
                                    final String requestId,
                                    final String cb) {
        if (StatisticSession.needToUpdate()) {
            SessionManager.getInstance().useOrOpenSession(new OpenSessionCallback() {
                @Override
                public void onSuccess() {
                    sendRequest(method, path, headers, getParams, body, requestId, cb);
                }

                @Override
                public void onError() {

                }
            });
        } else {
            sendRequest(method, path, headers, getParams, body, requestId, cb);
        }
    }


    TaskRunner taskRunner = new TaskRunner();


    private String oldEscape(String raw) {
        String escaped = JSONObject.quote(raw)
                .replaceFirst("^\"(.*)\"$", "$1")
                .replaceAll("\n", " ")
                .replaceAll("\r", " ");
        return escaped;
    }

    void sendRequest(final String method,
                     final String path,
                     final Map<String, String> headers,
                     final Map<String, String> getParams,
                     final String body,
                     final String requestId,
                     final String cb) {
        taskRunner.executeAsync(new GameRequestAsync(method, path,
                headers, getParams, body, requestId,
                host), new TaskRunner.Callback<GameResponse>() {
            @Override
            public void onComplete(GameResponse result) {
                try {
                    JSONObject resultJson = new JSONObject();
                    resultJson.put("requestId", result.requestId);
                    resultJson.put("status", result.status);
                    resultJson.put("data", oldEscape(result.data));
                    try {
                        resultJson.put("headers", new JSONObject(result.headers));
                    } catch (Exception e) {
                    }
                    host.loadGameResponse(resultJson.toString(), cb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    void onResume() {
        ScreensManager.getInstance().setTempShareStoryId(0);
        ScreensManager.getInstance().setTempShareId(null);
        if (ScreensManager.getInstance().getOldTempShareId() != null) {
            host.shareComplete(ScreensManager.getInstance().getOldTempShareId(), true);
        }
        ScreensManager.getInstance().setOldTempShareStoryId(0);
        ScreensManager.getInstance().setOldTempShareId(null);
    }

    void shareData(String id, String data) {
        ShareObject shareObj = JsonParser.fromJson(data, ShareObject.class);
        if (CallbackManager.getInstance().getShareCallback() != null) {
            CallbackManager.getInstance().getShareCallback()
                    .onShare(shareObj.getUrl(), shareObj.getTitle(), shareObj.getDescription(), id);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                ScreensManager.getInstance().setTempShareId(id);
                ScreensManager.getInstance().setTempShareStoryId(-1);
            } else {
                ScreensManager.getInstance().setOldTempShareId(id);
                ScreensManager.getInstance().setOldTempShareStoryId(-1);
            }
            host.shareDefault(shareObj);
        }
    }
}
