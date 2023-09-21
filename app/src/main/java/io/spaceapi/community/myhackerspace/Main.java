/*
 * Copyright (C) 2012-2017 Aubort Jean-Baptiste (Rorist)
 * Copyright (C) 2020-2023 Danilo Bargen (dbrgn)
 * Licensed under GNU's GPL 3, see README
 */
package io.spaceapi.community.myhackerspace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.appwidget.AppWidgetManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.http.HttpResponseCache;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;

import com.woozzu.android.util.StringMatcher;
import com.woozzu.android.widget.IndexableListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import io.spaceapi.SpaceApiParser;
import io.spaceapi.types.AccountBalance;
import io.spaceapi.types.Barometer;
import io.spaceapi.types.BeverageSupply;
import io.spaceapi.types.DoorLocked;
import io.spaceapi.types.Humidity;
import io.spaceapi.types.MemberCount;
import io.spaceapi.types.NetworkConnection;
import io.spaceapi.types.PeoplePresent;
import io.spaceapi.types.PowerConsumption;
import io.spaceapi.types.Status;
import io.spaceapi.types.Temperature;

public class Main extends Activity {
    protected static final String TAG = "MyHackerspace";

    // API: https://spaceapi.io/

    static final String API_DEFAULT = "https://fixme.ch/status.json";

    protected static final String PREF_API_URL_WIDGET = "api_url_widget_";
    protected static final String PREF_LAST_WIDGET = "last_widget_";
    protected static final String PREF_FORCE_WIDGET = "force_widget_";
    protected static final String STATE_HS = "hs";
    protected static final String STATE_DIR = "dir";
    protected static final String STATE_URL = "url";
    private static final int DIALOG_LOADING = 0;
    private static final int DIALOG_LIST = 1;
    private static final String MAP_COORD = "geo:%s,%s?z=23&q=";

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);

    // Shared preferences
    private SharedPreferences mPrefs;
    // Hashmap with the endpoint URL as key and the endpoint JSON string as value
    private HashMap<String, String> mResultHs;
    // Contains directory endpoint JSON data as string
    public String mResultDir;
    // The endpoint URL of the currently showing space
    private String mApiUrl;
    private boolean finishApi = false;
    private boolean finishDir = false;

    private ArrayList<String> mHsNames;
    private ArrayList<String> mHsUrls;

    private GetDirTask getDirTask;
    private GetApiTask getApiTask;
    private GetImage getImageTask;

    @Override
    @UiThread
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load layout
        setContentView(R.layout.main);

        // Hide views until loaded
        setViewVisibility(false);

        // Load shared prefs
        mPrefs = PreferenceManager.getDefaultSharedPreferences(Main.this);

        // Load data
        mResultHs = new HashMap<>();
        if (hasNetwork()) {
            Log.d(TAG, "onCreate() intent=" + getIntent().toString());
            setCache();
            getHsList();
            showHsInfo(getIntent(), true);
        } else {
            showNetworkError();
        }
    }

    @Override
    @UiThread
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent()=" + intent);
        showHsInfo(intent, false);
    }

    @Override
    @UiThread
    protected void onDestroy() {
        if (getApiTask != null) {
            getApiTask.cancel(true);
        }
        if (getDirTask != null) {
            getDirTask.cancel(true);
        }
        if (getImageTask != null) {
            getImageTask.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.menu_refresh) {
            if (hasNetwork()) {
                showHsInfo(getIntent(), true);
            } else {
                showNetworkError();
            }
            return true;
        } else if (id == R.id.menu_choose) {
            showDialog(DIALOG_LIST);
            return true;
        } else if (id == R.id.menu_prefs) {
            startActivity(new Intent(Main.this, Prefs.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Bundle onRetainNonConfigurationInstance() {
        Bundle data = new Bundle(3);
        if(mResultHs != null && mResultHs.size() > 0) {
            data.putSerializable(STATE_HS, mResultHs);
        }
        if(mResultDir != null && mResultDir.length() > 0) {
            data.putString(STATE_DIR, mResultDir);
        }
        if(mApiUrl != null && mApiUrl.length() > 0) {
            data.putString(STATE_URL, mApiUrl);
        }
        return data;
    }

    @Override
    protected void onSaveInstanceState(Bundle data) {
        if(mResultHs != null && mResultHs.size() > 0) {
            data.putSerializable(STATE_HS, mResultHs);
        }
        if(mResultDir != null && mResultDir.length() > 0) {
            data.putString(STATE_DIR, mResultDir);
        }
        if(mApiUrl != null && mApiUrl.length() > 0) {
            data.putString(STATE_URL, mApiUrl);
        }
        super.onSaveInstanceState(data);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        ProgressDialog dialog = null;
        switch (id) {
        case DIALOG_LOADING:
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
            dialog.setMessage(getString(R.string.msg_loading));
            dialog.setCancelable(true);
            dialog.setIndeterminate(true);
            break;
        case DIALOG_LIST:
            return createHsDialog();
        }
        return dialog;
    }

    private void setViewVisibility(boolean show) {
        int visibility1 = View.GONE;
        int visibility2 = View.VISIBLE;
        if (show) {
            visibility1 = View.VISIBLE;
            visibility2 = View.GONE;
        }
        // Views visibility
        findViewById(R.id.space_image).setVisibility(visibility1);
        findViewById(R.id.space_name).setVisibility(visibility1);
        findViewById(R.id.space_url).setVisibility(visibility1);
        findViewById(R.id.placeholder).setVisibility(visibility2);
        findViewById(R.id.main_wrapper).invalidate();
    }

    private AlertDialog createHsDialog() {
        // Construct hackerspaces list
        if (mResultDir == null){
            return null;
        }
        try {
            JSONObject obj = new JSONObject(mResultDir);
            JSONArray arr = obj.names();
            int len = obj.length();
            mHsNames = new ArrayList<>(len);
            mHsUrls = new ArrayList<>(len);
            for (int i = 0; i < len; i++) {
                mHsNames.add(arr.getString(i));
            }
            Collections.sort(mHsNames, String.CASE_INSENSITIVE_ORDER);
            for (int i = 0; i < len; i++) {
                mHsUrls.add(i, obj.getString(mHsNames.get(i)));
            }

            // Create the dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(Main.this);
            View view = getLayoutInflater().inflate(R.layout.hs_choose, null);
            ContentAdapter adapter = new ContentAdapter(Main.this,
                    android.R.layout.simple_list_item_2, mHsNames);

            IndexableListView listView = (IndexableListView) view
                    .findViewById(R.id.listview);
            listView.setAdapter(adapter);
            listView.setFastScrollEnabled(true);
            listView.setOnItemClickListener((parent, view1, position, id) -> {
                Log.d(TAG, "hs dialog, on click");
                String url = mHsUrls.get(position);
                Editor edit = mPrefs.edit();
                edit.putString(Prefs.KEY_API_URL, url);
                getApiTask = new GetApiTask();
                getApiTask.execute(url);
                edit.apply();
                setIntent(null);
                dismissDialog(DIALOG_LIST);
                Log.i(TAG, "Item clicked=" + url +  " (" + position + ")");
            });
            builder.setView(view);
            builder.setTitle(R.string.choose_hs);

            return builder.create();
        } catch (Exception e) {
            e.printStackTrace();
            return showError(e.getClass().getCanonicalName(), e.getLocalizedMessage(), true);
        }
    }

    /**
     * Fetch the directory and update the `mResultDir` variable.
     */
    private void getHsList() {
        final Bundle data = (Bundle) getLastNonConfigurationInstance();
        if (data == null) {
            Log.d(TAG, "getHsList(fresh data)");
            String apiEndpoint = mPrefs.getString(Prefs.KEY_API_ENDPOINT, Prefs.DEFAULT_API_ENDPOINT);
            getDirTask = new GetDirTask();
            getDirTask.execute(apiEndpoint);
        } else {
            Log.d(TAG, "getHsList(from state)");
            finishDir = true;
            mResultDir = data.getString(STATE_DIR);
        }
    }

    /**
     * Fetch the endpoint and update the `mApiUrl` and `mResultHs` variables.
     */
    private void showHsInfo(@Nullable Intent intent, boolean skipCache) {
        final Bundle data = (Bundle) getLastNonConfigurationInstance();

        // Get space endpoint URL
        if (data != null && data.containsKey(STATE_URL)) {
            Log.d(TAG, "showHsInfo(uri from state)");
            mApiUrl = data.getString(STATE_URL);
        } else if (intent != null && intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) {
            Log.d(TAG, "showHsInfo(uri from widget intent)");
            mApiUrl = mPrefs.getString(
                    PREF_API_URL_WIDGET
                            + intent.getIntExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                                    AppWidgetManager.INVALID_APPWIDGET_ID),
                    API_DEFAULT);
        } else if (intent != null && intent.hasExtra(STATE_HS)) {
            Log.d(TAG, "showHsInfo(uri from intent)");
            mApiUrl = intent.getStringExtra(STATE_HS);
        } else {
            Log.d(TAG, "showHsInfo(uri from prefs)");
            mApiUrl = mPrefs.getString(Prefs.KEY_API_URL, API_DEFAULT);
        }

        // Now that we have the URL, fetch the data
        if (data != null && data.containsKey(STATE_HS)) {
            Log.d(TAG, "showHsInfo(data from state)");
            finishApi = true;
            mResultHs = (HashMap<String, String>) data.getSerializable(STATE_HS);
            populateDataHs();
        } else if(mResultHs.containsKey(mApiUrl) && !skipCache) {
            Log.d(TAG, "showHsInfo(data from cache)");
            finishApi = true;
            populateDataHs();
        } else {
            Log.d(TAG, "showHsInfo(fresh data)");
            getApiTask = new GetApiTask();
            getApiTask.execute(mApiUrl);
        }

        // Update widget
        Widget.UpdateAllWidgets(getApplicationContext(), false);
    }

    /**
     * Return whether the phone has an active network connection or not.
     */
    private boolean hasNetwork() {
        return hasNetwork(getApplicationContext());
    }

    /**
     * Return whether the phone has an active network connection or not.
     */
    protected static boolean hasNetwork(Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context
            .getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo netInfo = Objects.requireNonNull(cm).getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private void setCache() {
        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        showError(title, msg, false);
    }

    private AlertDialog showError(String title, String msg, boolean ret) {
        if (title != null && msg != null) {
            AlertDialog dialog = new AlertDialog.Builder(Main.this)
                    .setTitle(getString(R.string.error_title) + " " + title)
                    .setMessage(msg)
                    .setNeutralButton(getString(R.string.ok), null).create();
            if (ret) {
                return dialog;
            } else {
                dialog.show();
            }
        }
        return null;
    }

    private void showNetworkError() {
        showError(getString(R.string.error_network_title), getString(R.string.error_network_msg));
    }

    private void dismissLoading() {
        if (finishApi && finishDir) {
            try {
                removeDialog(DIALOG_LOADING);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public class GetDirTask extends AsyncTask<String, Void, String> {

        private String mErrorTitle;
        private String mErrorMsg;

        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_LOADING);
        }

        @Override
        protected String doInBackground(String... url) {
            try {
                return new Net(url[0]).getString();
            } catch (Throwable e) {
                mErrorTitle = e.getClass().getCanonicalName();
                mErrorMsg = e.getLocalizedMessage() + " " + url[0];
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            finishDir = true;
            dismissLoading();
            if (mErrorMsg == null) {
                mResultDir = result;
            } else {
                showError(mErrorTitle, mErrorMsg);
            }
        }

        @Override
        protected void onCancelled() {
            finishDir = true;
            dismissLoading();
        }
    }

    private class GetApiTask extends AsyncTask<String, Void, String> {

        private String mErrorTitle;
        private String mErrorMsg;
        private String mUrl;

        @Override
        protected void onPreExecute() {
            showDialog(DIALOG_LOADING);
            // Clean UI
            ((ScrollView) findViewById(R.id.scroll)).removeAllViews();
            setViewVisibility(false);
        }

        @Override
        protected String doInBackground(String... url) {
            mUrl = url[0];
            Log.d(TAG, "GetApiTask(), mUrl="+mUrl);
            try {
                return new Net(url[0], false).getString();
            } catch (Throwable e) {
                mErrorTitle = e.getClass().getCanonicalName();
                mErrorMsg = e.getLocalizedMessage() + " " + url[0];
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            finishApi = true;
            dismissLoading();
            if (mErrorMsg == null) {
                mResultHs.put(mUrl, result);
                showHsInfo(getIntent(), false);
            } else {
                setViewVisibility(false);
                showError(mErrorTitle, mErrorMsg);
            }
        }

        @Override
        protected void onCancelled() {
            finishApi = true;
            dismissLoading();
        }
    }

    private class GetImage extends AsyncTask<String, Void, Bitmap> {

        private final int mId;
        private String mErrorTitle;
        private String mErrorMsg;

        GetImage(int id) {
            mId = id;
        }

        @Override
        protected void onPreExecute() {
            ImageView img = findViewById(mId);
            img.setImageResource(android.R.drawable.ic_popup_sync);
            AnimationDrawable anim = (AnimationDrawable) img.getDrawable();
            anim.start();
        }

        @Override
        protected Bitmap doInBackground(String... url) {
            try {
                return new Net(url[0]).getBitmap();
            } catch (Throwable e) {
                mErrorTitle = e.getClass().getCanonicalName();
                mErrorMsg = e.getLocalizedMessage() + " " + url[0];
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (mErrorMsg == null) {
                ((ImageView) findViewById(mId)).setImageBitmap(result);
            } else {
                showError(mErrorTitle, mErrorMsg);
                ((ImageView) findViewById(mId))
                        .setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

    }

    private TextView addEntry(LayoutInflater inflater, LinearLayout vg, String value) {
        TextView tv = (TextView) inflater.inflate(R.layout.entry, null);
        tv.setAutoLinkMask(0);
        tv.setText(value);
        vg.addView(tv);
        return tv;
    }

    /**
     * Shortcut function. If the value is not null, add it as an entry.
     */
    private @Nullable TextView addEntryIfValueNotNull(
        @NonNull LayoutInflater inflater,
        @NonNull LinearLayout vg,
        @Nullable @StringRes int title,
        @Nullable Object value
    ) {
        if (value != null) {
            return addEntry(inflater, vg, getString(title) + ": " + value.toString());
        }
        return null;
    }

    private TextView addTitle(LayoutInflater inflater, LinearLayout vg, @StringRes int value) {
        return addTitle(inflater, vg, getString(value));
    }

    private TextView addTitle(LayoutInflater inflater, LinearLayout vg, @NonNull String value) {
        final TextView title = (TextView) inflater.inflate(R.layout.title, null);
        title.setText(value);
        vg.addView(title);
        inflater.inflate(R.layout.separator, vg);
        return title;
    }

    private TextView addSubtitle(LayoutInflater inflater, LinearLayout vg, @StringRes int value) {
        return addSubtitle(inflater, vg, getString(value));
    }

    private TextView addSubtitle(LayoutInflater inflater, LinearLayout vg, @NonNull String value) {
        final TextView title = (TextView) inflater.inflate(R.layout.subtitle, null);
        title.setText(value);
        vg.addView(title);
        return title;
    }

    /**
     * Add a sensor with the "sensor" layout.
     */
    private void addSensor(
        @NonNull LayoutInflater inflater,
        @NonNull LinearLayout vg,
        @NonNull String value,
        @Nullable String details1,
        @Nullable String details2
    ) {
        final RelativeLayout rl = (RelativeLayout) inflater.inflate(R.layout.entry_sensor, null);
        final TextView viewValue = rl.findViewById(R.id.entry_value);
        final TextView viewDetails1 = rl.findViewById(R.id.entry_details1);
        final TextView viewDetails2 = rl.findViewById(R.id.entry_details2);
        viewValue.setText(value);
        if (details1 != null) {
            viewDetails1.setText(details1);
        } else {
            viewDetails1.setVisibility(View.GONE);
        }
        if (details2 != null) {
            viewDetails2.setText(details2);
        } else {
            viewDetails2.setVisibility(View.GONE);
        }
        vg.addView(rl);
    }

    /**
     * Parse the endpoint JSON and populate UI with parsed information.
     */
    private void populateDataHs() {
        try {
            Log.i(TAG, "populateDataHs()=" + mApiUrl);
            setViewVisibility(false);

            // Look up endpoint JSOn
            final String endpointJson = mResultHs.get(mApiUrl);
            if (endpointJson == null) {
                throw new IllegalStateException("Endpoint JSON not found");
            }

            // Parse the JSON string using the `spaceapi-kt` library.
            final Status data = SpaceApiParser.parseString(endpointJson);

            // Initialize views
            LayoutInflater iftr = getLayoutInflater();
            LinearLayout vg = (LinearLayout) iftr.inflate(R.layout.base, null);
            ScrollView scroll = findViewById(R.id.scroll);
            scroll.removeAllViews();
            scroll.addView(vg);

            // Mandatory fields
            ((TextView) findViewById(R.id.space_name)).setText(data.space);
            ((TextView) findViewById(R.id.space_url)).setText(data.url.toString());
            getImageTask = new GetImage(R.id.space_image);
            getImageTask.execute(data.logo);

            // Status text
            String status_txt;
            if (data.state == null) {
                status_txt = getString(R.string.status_unknown);
                ((TextView) findViewById(R.id.status_txt))
                    .setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.presence_invisible, 0, 0, 0);
            } else if (Boolean.TRUE.equals(data.state.open)) {
                status_txt = getString(R.string.status_open);
                ((TextView) findViewById(R.id.status_txt))
                    .setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.presence_online, 0, 0, 0);
            } else {
                status_txt = getString(R.string.status_closed);
                ((TextView) findViewById(R.id.status_txt))
                    .setCompoundDrawablesWithIntrinsicBounds(
                        android.R.drawable.presence_busy, 0, 0, 0);
            }
            if (data.state.message != null) {
                status_txt += ": " + data.state.message;
            }
            ((TextView) findViewById(R.id.status_txt)).setText(status_txt);

            // Status last change
            if (data.state != null && data.state.lastchange != null) {
                final ZonedDateTime lastchange = data.state.lastchange.atZone(ZoneId.systemDefault());
                final String lastchangeString = lastchange.format(this.dateTimeFormatter);
                addEntry(iftr, vg, getString(R.string.api_lastchange) + " " + lastchangeString);
            }

            // Location
            addTitle(iftr, vg, R.string.api_location);
            TextView latLonTv = addEntry(iftr, vg, data.location.lat + ", " + data.location.lon);
            Linkify.addLinks(
                latLonTv,
                Pattern.compile("^.*$", Pattern.DOTALL),
                String.format(MAP_COORD, data.location.lat, data.location.lon)
            );

            // Postal address
            if (data.location.address != null) {
                addTitle(iftr, vg, R.string.api_postal_addr);
                addEntry(iftr, vg, data.location.address);
            }

            // Contact
            addTitle(iftr, vg, R.string.api_contact);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_phone, data.contact.phone);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_sip, data.contact.sip);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_email, data.contact.email);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_ml, data.contact.ml);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_irc, data.contact.irc);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_twitter, data.contact.twitter);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_identica, data.contact.identica);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_mastodon, data.contact.mastodon);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_facebook, data.contact.facebook); // Eeeew!
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_foursquare, data.contact.foursquare);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_xmpp, data.contact.xmpp);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_gopher, data.contact.gopher);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_matrix, data.contact.matrix);
            addEntryIfValueNotNull(iftr, vg, R.string.api_contact_mumble, data.contact.mumble);

            // Sensors: Space
            if (data.sensors != null && (
                data.sensors.people_now_present.length > 0
                    || data.sensors.door_locked.length > 0
                    || data.sensors.beverage_supply.length > 0
                    || data.sensors.power_consumption.length > 0
                    || data.sensors.network_connections.length > 0
                /*|| data.sensors.network_traffic.length > 0*/
            )) {
                addTitle(iftr, vg, getString(R.string.api_sensors) + ": " + getString(R.string.api_sensors_space));

                // People present
                if (data.sensors.people_now_present.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_people_now_present);
                    for (PeoplePresent entry : data.sensors.people_now_present) {
                        addSensor(
                            iftr,
                            vg,
                            String.format("%d", entry.value),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }

                // Door status
                if (data.sensors.door_locked.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_door_locked);
                    for (DoorLocked entry : data.sensors.door_locked) {
                        addSensor(
                            iftr,
                            vg,
                            getString(entry.value ? R.string.api_sensor_door_locked_yes : R.string.api_sensor_door_locked_no),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }

                // Beverage supply
                if (data.sensors.beverage_supply.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_beverage_supply);
                    for (BeverageSupply entry : data.sensors.beverage_supply) {
                        addSensor(
                            iftr,
                            vg,
                            String.format("%.1f %s", entry.value, entry.unit),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }

                // Power consumption
                if (data.sensors.power_consumption.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_power_consumption);
                    for (PowerConsumption entry : data.sensors.power_consumption) {
                        addSensor(
                            iftr,
                            vg,
                            String.format("%.1f %s", entry.value, entry.unit),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }

                // Network connections
                if (data.sensors.network_connections.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_network_connections);
                    for (NetworkConnection entry : data.sensors.network_connections) {
                        String details = Utils.joinStrings(" / ", entry.location, entry.name);
                        if (details != null && entry.type != null) {
                            details += " (" + entry.type + ")";
                        } else if (entry.type != null) {
                            details = entry.type;
                        }
                        addSensor(
                            iftr,
                            vg,
                            String.format("%d", entry.value),
                            details,
                            entry.description
                        );
                    }
                }
            }

            // Sensors: Environment
            if (data.sensors != null && (
                data.sensors.temperature.length > 0
                    || data.sensors.humidity.length > 0
                    || data.sensors.barometer.length > 0
                    || data.sensors.wind.length > 0
            )) {
                addTitle(iftr, vg, getString(R.string.api_sensors) + ": " + getString(R.string.api_sensors_environment));

                // Temperature
                if (data.sensors.temperature.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_temperature);
                    for (Temperature entry : data.sensors.temperature) {
                        addSensor(
                            iftr,
                            vg,
                            String.format("%.1f %s", entry.value, entry.unit),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }

                // Humidity
                if (data.sensors.humidity.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_humidity);
                    for (Humidity entry : data.sensors.humidity) {
                        addSensor(
                            iftr,
                            vg,
                            String.format("%.1f %s", entry.value, entry.unit),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }

                // Air pressure
                if (data.sensors.barometer.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_barometer);
                    for (Barometer entry : data.sensors.barometer) {
                        addSensor(
                            iftr,
                            vg,
                            String.format("%.1f %s", entry.value, entry.unit),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }

                // Missing: Wind (not used by any space right now)
                // and Radiation (I don't know how to interpret and visualize the measurements in a meaningful way)
            }

            // Sensors: Organization
            if (data.sensors != null && (
                data.sensors.total_member_count.length > 0
                    || data.sensors.account_balance.length > 0
            )) {
                addTitle(iftr, vg, getString(R.string.api_sensors) + ": " + getString(R.string.api_sensors_organization));

                // Total Member Count
                if (data.sensors.total_member_count.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_total_member_count);
                    for (MemberCount entry : data.sensors.total_member_count) {
                        addSensor(
                            iftr,
                            vg,
                            String.format("%d", entry.value),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }

                // Account balance
                if (data.sensors.account_balance.length > 0) {
                    addSubtitle(iftr, vg, R.string.api_sensor_account_balance);
                    for (AccountBalance entry : data.sensors.account_balance) {
                        addSensor(
                            iftr,
                            vg,
                            String.format("%.2f %s", entry.value, entry.unit),
                            Utils.joinStrings(" / ", entry.location, entry.name),
                            entry.description
                        );
                    }
                }
            }

            // Webcams
            if (data.cam.length > 0) {
                addTitle(iftr, vg, R.string.api_webcams);
                for (String url : data.cam) {
                    final TextView tv = addEntry(iftr, vg, url);
                    Linkify.addLinks(tv, Pattern.compile("^.*$", Pattern.DOTALL), null);
                }
            }

            setViewVisibility(true);
        } catch (Exception e) {
            e.printStackTrace();
            setViewVisibility(false);
            showError(e.getClass().getCanonicalName(), e.getLocalizedMessage()
                    + getString(R.string.error_generic));
        }
    }

    static class ViewHolder {
        TextView name;
        TextView desc;
    }

    // https://github.com/woozzu/IndexableListView/
    private class ContentAdapter extends ArrayAdapter<String> implements
            SectionIndexer {

        private final LayoutInflater mInflater;
        private final String mSections = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        ContentAdapter(Context context, int textViewResourceId,
                List<String> objects) {
            super(context, textViewResourceId, objects);
            mInflater = getLayoutInflater();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.hs_entry, null);
                holder = new ViewHolder();
                holder.name = convertView.findViewById(R.id.text1);
                holder.desc = convertView.findViewById(R.id.text2);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.name.setText(mHsNames.get(position));
            holder.desc.setText(mHsUrls.get(position));
            return convertView;
        }

        @Override
        public int getPositionForSection(int section) {
            // If there is no item for current section, previous section will be
            // selected
            for (int i = section; i >= 0; i--) {
                for (int j = 0; j < getCount(); j++) {
                    if (i == 0) {
                        // For numeric section
                        for (int k = 0; k <= 9; k++) {
                            if (StringMatcher.match(
                                    String.valueOf(getItem(j).charAt(0)),
                                    String.valueOf(k)))
                                return j;
                        }
                    } else {
                        if (StringMatcher.match(
                                String.valueOf(getItem(j).charAt(0)),
                                String.valueOf(mSections.charAt(i))))
                            return j;
                    }
                }
            }
            return 0;
        }

        @Override
        public int getSectionForPosition(int position) {
            return 0;
        }

        @Override
        public Object[] getSections() {
            String[] sections = new String[mSections.length()];
            for (int i = 0; i < mSections.length(); i++)
                sections[i] = String.valueOf(mSections.charAt(i));
            return sections;
        }
    }

}
