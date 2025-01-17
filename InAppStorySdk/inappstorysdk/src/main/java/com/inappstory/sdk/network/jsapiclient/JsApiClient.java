package com.inappstory.sdk.network.jsapiclient;

import android.content.Context;

import com.inappstory.sdk.network.JsonParser;
import com.inappstory.sdk.stories.api.models.StatisticSession;
import com.inappstory.sdk.stories.api.models.callbacks.OpenSessionCallback;
import com.inappstory.sdk.stories.statistic.ProfilingManager;
import com.inappstory.sdk.stories.utils.SessionManager;
import com.inappstory.sdk.stories.utils.TaskRunner;

import org.json.JSONObject;

import java.util.Map;

import static com.inappstory.sdk.network.JsonParser.toMap;

public class JsApiClient {
    TaskRunner taskRunner = new TaskRunner();

    Context context;

    public JsApiClient(Context context) {
        this.context = context;
    }

    public void sendApiRequest(String data, JsApiResponseCallback callback) {
        JsApiRequestConfig config = JsonParser.fromJson(data, JsApiRequestConfig.class);
        Map<String, String> headers = null;
        if (config.headers != null && !config.headers.isEmpty()) {
            headers = toMap(config.headers);
        }
        Map<String, String> getParams = null;
        if (config.params != null && !config.params.isEmpty()) {
            getParams = toMap(config.params);
        }
        checkSessionAndSendRequest(config.method, config.url, headers, getParams,
                config.data, config.id, config.cb, config.profilingKey, callback);
    }

    void checkSessionAndSendRequest(final String method,
                                    final String path,
                                    final Map<String, String> headers,
                                    final Map<String, String> getParams,
                                    final String body,
                                    final String requestId,
                                    final String cb,
                                    final String profilingKey,
                                    final JsApiResponseCallback callback) {
        if (StatisticSession.needToUpdate()) {
            SessionManager.getInstance().useOrOpenSession(new OpenSessionCallback() {
                @Override
                public void onSuccess() {
                    sendRequest(method, path, headers, getParams, body, requestId, cb, profilingKey, callback);
                }

                @Override
                public void onError() {

                }
            });
        } else {
            sendRequest(method, path, headers, getParams, body, requestId, cb, profilingKey, callback);
        }
    }

    String oldEscape(String raw) {
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
                     final String cb,
                     final String profilingKey,
                     final JsApiResponseCallback callback) {
        final String sarHash;
        if (profilingKey != null && !profilingKey.isEmpty()) {
            sarHash = ProfilingManager.getInstance().addTask(profilingKey);
        } else {
            sarHash = null;
        }
        taskRunner.executeAsync(new JsApiRequestAsync(method, path,
                headers, getParams, body, requestId,
                context), new TaskRunner.Callback<JsApiResponse>() {
            @Override
            public void onComplete(JsApiResponse result) {
                ProfilingManager.getInstance().setReady(sarHash);
                try {
                    JSONObject resultJson = new JSONObject();
                    resultJson.put("requestId", result.requestId);
                    resultJson.put("status", result.status);
                    resultJson.put("data", oldEscape(result.data));
                    try {
                        resultJson.put("headers", new JSONObject(result.headers));
                    } catch (Exception e) {
                    }
                    callback.onJsApiResponse(resultJson.toString(), cb);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
