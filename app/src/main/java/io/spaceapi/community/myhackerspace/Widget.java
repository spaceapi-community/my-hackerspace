/*
 * Copyright (C) 2012-2017 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2020-2022 Danilo Bargen (dbrgn)
 * Licensed under GNU's GPL 3, see README
 */
package io.spaceapi.community.myhackerspace;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;

import io.spaceapi.ParseError;
import io.spaceapi.SpaceApiParser;

public class Widget extends AppWidgetProvider {

    static final String TAG ="MyHackerspace_Widget";
    static final String WIDGET_IDS = "widget_ids";
    static final String WIDGET_FORCE = "widget_force";

    public void onReceive(Context ctxt, Intent intent) {
        String action = intent.getAction();
        if (intent.hasExtra(WIDGET_IDS)
                && (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action) ||
                Intent.ACTION_BOOT_COMPLETED.equals(action))) {
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(ctxt);
            Editor edit = prefs.edit();
            int[] ids = intent.getIntArrayExtra(WIDGET_IDS);
            for (final int widgetId : ids) {
                // Set force to false by default
                edit.putBoolean(Main.PREF_FORCE_WIDGET + widgetId,
                        intent.getBooleanExtra(Widget.WIDGET_FORCE, false));
                // Start alarm
                Intent newIntent = getIntent(ctxt, widgetId);
                setAlarm(ctxt, newIntent, widgetId);
            }
            edit.apply();
        } else {
            super.onReceive(ctxt, intent);
        }
    }

    @Override
    public void onDeleted(Context ctxt, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            // Remove widget alarm
            PendingIntent pi = PendingIntent.getService(ctxt, widgetId,
                    getIntent(ctxt, widgetId), 0);
            AlarmManager am = (AlarmManager) ctxt
                    .getSystemService(Context.ALARM_SERVICE);
            am.cancel(pi);

            // remove preference
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(ctxt);
            Editor edit = prefs.edit();
            edit.remove(Main.PREF_API_URL_WIDGET + widgetId);
            edit.remove(Main.PREF_LAST_WIDGET + widgetId);
            edit.remove(Main.PREF_FORCE_WIDGET + widgetId);
            edit.apply();

            Log.i(TAG, "Remove widget alarm for id=" + widgetId);
        }
    }

    protected static Intent getIntent(Context ctxt, int widgetId) {
        Intent i = new Intent(ctxt, UpdateService.class);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return i;
    }

    protected static void setAlarm(Context ctxt, Intent i, int widgetId) {
        setAlarm(ctxt, i, widgetId, 50);
    }

    protected static void setAlarm(Context ctxt, Intent i, int widgetId,
            int delay) {
        // Get interval
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(ctxt);
        long update_interval = Long.parseLong(prefs.getString(
                Prefs.KEY_CHECK_INTERVAL, Prefs.DEFAULT_CHECK_INTERVAL)) * 60L * 1000L;
        // Set alarm
        AlarmManager am = (AlarmManager) ctxt
                .getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getService(ctxt, widgetId, i, 0);
        am.cancel(pi);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                delay, update_interval, pi);
        // Log.i(TAG, "start notification every " + update_interval / 1000
        // + "s");
    }

    private static class GetImage extends AsyncTask<URL, Void, Bitmap> {

        private final int mId;
        private WeakReference<Context> mCtxt;
        private final String mText;
        private String mError = null;

        public GetImage(Context ctxt, int id, String text) {
            mCtxt = new WeakReference<>(ctxt);
            mId = id;
            mText = text;
        }

        @Override
        protected Bitmap doInBackground(URL... url) {
            try {
                return new Net(url[0].toString()).getBitmap();
            } catch (Throwable e) {
                e.printStackTrace();
                mError = e.getMessage();
                cancel(true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            final Context ctxt = mCtxt.get();
            if(ctxt == null) { Log.e(TAG, "Context error (postExecute)"); return; }
            AppWidgetManager manager = AppWidgetManager.getInstance(ctxt);
            updateWidget(ctxt, mId, manager, result, mText);
        }

        @Override
        protected void onCancelled () {
            final Context ctxt = mCtxt.get();
            if (mError != null && ctxt != null) {
                printMessage(ctxt, mError);
            }
        }

    }

    protected static void updateWidget(final Context ctxt, int widgetId,
            AppWidgetManager manager, Bitmap bitmap, String text) {
        RemoteViews views = new RemoteViews(ctxt.getPackageName(),
                R.layout.widget);
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(ctxt);
        Editor edit = prefs.edit();
        if (prefs.getBoolean(Prefs.KEY_WIDGET_TRANSPARENCY,
                Prefs.DEFAULT_WIDGET_TRANSPARENCY)) {
            views.setInt(R.id.widget_image, "setBackgroundResource", 0);
        } else {
            views.setInt(R.id.widget_image, "setBackgroundResource",
                    android.R.drawable.btn_default_small);
        }
        if (bitmap != null) {
            views.setImageViewBitmap(R.id.widget_image, bitmap);
            edit.putBoolean(Main.PREF_FORCE_WIDGET + widgetId, false);
        } else {
            // Something went wrong
            views.setImageViewResource(R.id.widget_image,
                    android.R.drawable.ic_popup_sync);
            edit.putBoolean(Main.PREF_FORCE_WIDGET + widgetId, true);
        }
        if (text != null) {
            views.setTextViewText(R.id.widget_status, text);
            views.setViewVisibility(R.id.widget_status, View.VISIBLE);
        } else {
            views.setViewVisibility(R.id.widget_status, View.GONE);
        }
        edit.commit();
        Intent clickIntent = new Intent(ctxt, Main.class);
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(ctxt, widgetId,
                clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent);
        manager.updateAppWidget(widgetId, views);
    }

    private static class GetApiTask extends AsyncTask<String, Void, String> {

        private final int mId;
        private WeakReference<Context> mCtxt;
        private String mError = null;

        public GetApiTask(Context ctxt, int id) {
            mCtxt = new WeakReference<>(ctxt);
            mId = id;
        }

        @Override
        protected String doInBackground(String... url) {
            try {
                return new Net(url[0], false).getString();
            } catch (Throwable e) {
                e.printStackTrace();
                mError = e.getMessage();
                cancel(true);
            }
            return "";
        }

        @Override
        protected void onCancelled() {
            Log.i(TAG, "Reset alarm after cancel");
            final Context ctxt = mCtxt.get();
            if(ctxt != null) {
                Intent intent = getIntent(ctxt, mId);
                setAlarm(ctxt, intent, mId, 500);
                if (mError != null) {
                    printMessage(ctxt, mError);
                }
            }
        }

        @Override
        protected void onPostExecute(String endpointJson) {
            final Context ctxt = mCtxt.get();
            if(ctxt == null) { Log.e(TAG, "Context error (postExecute)"); return; }

            try {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);

                final io.spaceapi.types.Status data = SpaceApiParser.parseString(endpointJson);

                boolean statusBool = data.state != null && data.state.open;

                // Update only if different than last status or not forced
                if (prefs.contains(Main.PREF_LAST_WIDGET + mId)
                        && prefs.getBoolean(Main.PREF_LAST_WIDGET + mId, false) == statusBool
                        && !prefs.getBoolean(Main.PREF_FORCE_WIDGET + mId, false)) {
                    Log.d(TAG, "Nothing to update");
                    return;
                }

                // Mandatory fields
                final Editor edit = prefs.edit();
                edit.putBoolean(Main.PREF_LAST_WIDGET + mId, statusBool);
                edit.apply();

                String status_text = null;
                if (prefs.getBoolean(Prefs.KEY_WIDGET_TEXT, Prefs.DEFAULT_WIDGET_TEXT)) {
                    if (data.state != null && data.state.message != null) {
                        status_text = data.state.message;
                    } else {
                        status_text = statusBool
                            ? ctxt.getString(R.string.status_open)
                            : ctxt.getString(R.string.status_closed);
                    }
                }

                // Status icon or space icon
                if (data.state != null && data.state.icon != null) {
                    new GetImage(ctxt, mId, status_text).execute(statusBool ? data.state.icon.open : data.state.icon.closed);
                } else {
                    new GetImage(ctxt, mId, status_text).execute(new URL(data.logo));
                }
            } catch (ParseError | MalformedURLException e) {
                e.printStackTrace();
                String msg = e.getMessage();
                printMessage(ctxt, msg);
            }
        }
    }

    public static class UpdateService extends IntentService {

        public UpdateService() {
            super("MyHackerspaceWidgetService");
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            final Context ctxt = UpdateService.this;
            final int widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(ctxt);
            if (Main.hasNetwork(ctxt) && prefs.contains(Main.PREF_API_URL_WIDGET + widgetId)) {
                final String url = prefs.getString(Main.PREF_API_URL_WIDGET + widgetId, Main.API_DEFAULT);
                Log.i(TAG, "Update widgetid " + widgetId + " with url " + url);
                new Handler(Looper.getMainLooper())
                        .post(() -> new GetApiTask(ctxt, widgetId).execute(url));
            }
            stopSelf();
        }
    }

    public static void UpdateAllWidgets(final Context ctxt, boolean force) {
        AppWidgetManager man = AppWidgetManager.getInstance(ctxt);
        int[] ids = man.getAppWidgetIds(new ComponentName(ctxt, Widget.class));
        Intent ui = new Intent();
        ui.setClass(ctxt, Widget.class);
        ui.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        ui.putExtra(Widget.WIDGET_IDS, ids);
        ui.putExtra(Widget.WIDGET_FORCE, force);
        ctxt.sendBroadcast(ui);
        Log.i(TAG, "UpdateAllWidgets force=" + force);
    }

    private static void printMessage(final Context ctxt, String msg){
        if(msg == null){
            return;
        }
        Log.e(TAG, msg);
        Toast.makeText(ctxt, msg, Toast.LENGTH_SHORT).show();
    }

}
