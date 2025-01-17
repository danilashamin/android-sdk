package com.inappstory.sdk.listwidget;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.widget.RemoteViewsService;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.inappstory.sdk.AppearanceManager;
import com.inappstory.sdk.R;
import com.inappstory.sdk.eventbus.CsEventBus;
import com.inappstory.sdk.exceptions.DataException;
import com.inappstory.sdk.network.ApiSettings;
import com.inappstory.sdk.network.JsonParser;
import com.inappstory.sdk.network.NetworkCallback;
import com.inappstory.sdk.network.NetworkClient;
import com.inappstory.sdk.stories.api.models.CachedSessionData;
import com.inappstory.sdk.stories.api.models.Story;
import com.inappstory.sdk.stories.api.models.StoryListType;
import com.inappstory.sdk.stories.statistic.SharedPreferencesAPI;

import java.lang.ref.WeakReference;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StoriesWidgetService extends RemoteViewsService {

    public static StoriesWidgetService getInstance() {
        return INSTANCE;
    }


    private static final String TEST_DOMAIN = "https://api.test.inappstory.com/";
    private static final String PRODUCT_DOMAIN = "https://api.inappstory.com/";

    public static void loadData(@NonNull Context context) throws DataException {
        if (AppearanceManager.csWidgetAppearance() == null || AppearanceManager.csWidgetAppearance().getWidgetClass() == null)
            throw new DataException("'widgetClass' must not be null", new Throwable("Widget data is not valid"));
        if (ApiSettings.getInstance().getCmsUrl() == null) {
            ApiSettings
                    .getInstance()
                    .cacheDirPath(context.getCacheDir().getAbsolutePath())
                    .apiKey(context.getResources().getString(R.string.csApiKey))
                    .setWebUrl(AppearanceManager.csWidgetAppearance().isSandbox() ?
                            "https://api.test.inappstory.com/" : "https://api.inappstory.com/")
                    .cmsUrl(AppearanceManager.csWidgetAppearance().isSandbox() ?
                            "https://api.test.inappstory.com/" : "https://api.inappstory.com/");
        }

        if (isConnected(context)) {
            loadList(context, AppearanceManager.csWidgetAppearance().getWidgetClass());
        } else {
            loadNoConnection(context, AppearanceManager.csWidgetAppearance().getWidgetClass());
        }
    }

    static boolean isConnected(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network nw = connectivityManager.getActiveNetwork();
                if (nw == null) return false;
                NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(nw);
                return actNw != null && (
                        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
            } else {
                NetworkInfo nwInfo = connectivityManager.getActiveNetworkInfo();
                return nwInfo != null && nwInfo.isConnected();
            }
        } catch (Exception e) {
            return true;
        }
    }

    private static void sendBroadcast(String action, Class widgetClass, Context context) {
        Intent i = new Intent(context, widgetClass);
        i.setAction(action);
        context.sendBroadcast(i);
    }

    public static void loadEmpty(Context context, Class widgetClass) {
        sendBroadcast(UPDATE_EMPTY, widgetClass, context);
    }

    public static void loadAuth(Context context, Class widgetClass) {
        sendBroadcast(UPDATE_AUTH, widgetClass, context);
    }

    public static void loadNoConnection(Context context, Class widgetClass) {
        sendBroadcast(UPDATE_NO_CONNECTION, widgetClass, context);
    }

   /* public static void load(Context context, Class widgetClass) {
        sendBroadcast(UPDATE, widgetClass, context);
    }*/

    public static void loadSuccess(Context context, Class widgetClass) {
        sendBroadcast(UPDATE_SUCCESS, widgetClass, context);
    }


    public static final String UPDATE = "ias_w.UPDATE_WIDGETS";
    public static final String CLICK_ITEM = "ias_w.CLICK_ITEM";
    public static final String POSITION = "item_position";
    public static final String ID = "item_id";
    public static final String UPDATE_SUCCESS = "ias_w.UPDATE_SUCCESS_WIDGETS";
    public static final String UPDATE_EMPTY = "ias_w.UPDATE_EMPTY_WIDGETS";
    public static final String UPDATE_NO_CONNECTION = "ias_w.UPDATE_NO_CONNECTION";
    public static final String UPDATE_AUTH = "ias_w.UPDATE_AUTH";

    private static void loadList(final Context context, final Class widgetClass) {
        CachedSessionData cachedSessionData = CachedSessionData.getInstance(context);
        if (cachedSessionData == null) {
            loadAuth(context, widgetClass);
            return;
        }
        /*if (InAppStoryManager.getInstance() == null) {
            try {
                new InAppStoryManager.Builder()
                        .userId(cachedSessionData.userId)
                        .context(context)
                        .sandbox(false)
                        .hasLike(true)
                        .hasFavorite(true)
                        .hasShare(true)
                        .create();
            } catch (DataException e) {
                e.printStackTrace();
            }
        }*/
        if (NetworkClient.getAppContext() == null) {
            NetworkClient.setContext(context);
        }
        NetworkClient.getApi().getStories(cachedSessionData.testKey, 0,
                cachedSessionData.tags, null).enqueue(new NetworkCallback<List<Story>>() {
            @Override
            public void onSuccess(List<Story> response) {
                if (response.size() > 0) {
                    if (!SharedPreferencesAPI.hasContext()) {
                        SharedPreferencesAPI.setContext(context);
                    }
                    ArrayList<Story> stories = new ArrayList<>();
                    for (int i = 0; i < Math.min(response.size(), 4); i++) {
                        stories.add(response.get(i));
                    }
                    try {
                        SharedPreferencesAPI.saveString("widgetStories", JsonParser.getJson(stories));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    if (INSTANCE != null) {
                        INSTANCE.refreshFactory();
                    }

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            loadSuccess(context, widgetClass);
                        }
                    }, 500);
                } else {
                    loadEmpty(context, widgetClass);
                }

            }

            @Override
            public void onError(int code, String message) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }


            @Override
            public Type getType() {
                return new StoryListType();
            }
        });
    }

    public void refreshFactory() {
        if (factory != null && factory.get() != null) {
            factory.get().refreshData();
        }
    }

    private static StoriesWidgetService INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    WeakReference<StoriesWidgetFactory> factory;

    @Override
    public void onDestroy() {
        super.onDestroy();
        INSTANCE = null;
    }

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        // TODO Auto-generated method stub
        factory = new WeakReference<>(new StoriesWidgetFactory(this.getApplicationContext(), intent));
        return factory.get();
    }

}