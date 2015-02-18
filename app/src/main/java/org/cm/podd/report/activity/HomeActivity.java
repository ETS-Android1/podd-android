/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cm.podd.report.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.cm.podd.report.BuildConfig;
import org.cm.podd.report.PoddApplication;
import org.cm.podd.report.R;
import org.cm.podd.report.db.NotificationDataSource;
import org.cm.podd.report.fragment.DashboardFeedFragment;
import org.cm.podd.report.fragment.NotificationInterface;
import org.cm.podd.report.fragment.NotificationListFragment;
import org.cm.podd.report.fragment.ReportListFragment;
import org.cm.podd.report.fragment.VisualizationFragment;
import org.cm.podd.report.service.ConnectivityChangeReceiver;
import org.cm.podd.report.service.DataSubmitService;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONObject;

import java.io.IOException;

public class HomeActivity extends ActionBarActivity implements ReportListFragment.OnReportSelectListener, NotificationInterface {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    public static final String RECEIVE_MESSAGE_ACTION = "podd.receive_message_action";
    public static final String TAG = "HomeActivity";
    private static final String APP_TITLE = "ผ่อดีดี";

    Fragment mCurrentFragment;
    int mNotificationCount;

    private String[] mMenuTitles;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    DrawerAdapter drawerAdapter;

    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private int drawerPosition;

    NotificationDataSource notificationDataSource;
    private boolean sendScreenViewAnalytic = true;
    private SharedPrefUtil sharedPrefUtil;

    GoogleCloudMessaging gcm;
    String regid;

    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Receiving action " + intent.getAction());
            setNotificationCount();
            refreshDrawerAdapter();
            supportInvalidateOptionsMenu();
            refreshNotificationListAdapter();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // notification receiver from gcm intent service
        registerReceiver(mNotificationReceiver, new IntentFilter(RECEIVE_MESSAGE_ACTION));

        // initialize and create or upgrade db
        notificationDataSource = new NotificationDataSource(this);

        // initialize prefs
        sharedPrefUtil = new SharedPrefUtil((getApplicationContext()));

        mMenuTitles = getResources().getStringArray(R.array.menu_titles);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // Set the adapter for the list view
        setNotificationCount();
        refreshDrawerAdapter();

        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        mDrawerTitle = APP_TITLE;
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                setTitle(drawerPosition == 0 ? APP_TITLE : mMenuTitles[drawerPosition]);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                new ConnectivityChangeReceiver(),
                new IntentFilter(DataSubmitService.ACTION_REPORT_SUBMIT));

        /* return to last position after recreate activity */
        if (savedInstanceState != null) {
            drawerPosition = savedInstanceState.getInt("drawerPosition");
        } else {
            drawerPosition = 0;
        }
        selectItem(drawerPosition);

        onNewIntent(getIntent());
    }

    public void setNotificationCount() {
        mNotificationCount = notificationDataSource.getUnseenCount();
    }

    public void refreshDrawerAdapter() {
        drawerAdapter = new DrawerAdapter(this, R.layout.drawer_list_item, mMenuTitles, mNotificationCount);
        mDrawerList.setAdapter(drawerAdapter);
    }

    public void refreshNotificationListAdapter() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(NotificationListFragment.class.getSimpleName());
        if (fragment != null && fragment == mCurrentFragment) {
            NotificationListFragment notificationFragment = (NotificationListFragment) fragment;
            notificationFragment.refreshAdapter();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("drawerPosition", drawerPosition);
        super.onSaveInstanceState(outState);
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        menu.findItem(R.id.action_settings).setVisible(!drawerOpen);
        menu.findItem(R.id.action_new_event).setVisible(!drawerOpen);
        if (drawerPosition > 0) {
            menu.findItem(R.id.action_new_event).setVisible(false);
        }

        MenuItem item = menu.findItem(R.id.badge);
        item.setVisible(mNotificationCount > 0);

        MenuItemCompat.setActionView(item, R.layout.notif_count);
        Button counter = (Button) MenuItemCompat.getActionView(item);
        counter.setText(String.valueOf(mNotificationCount));

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    /** Swaps fragments in the main content view */
    private void selectItem(int position) {
        drawerPosition = position;
        if (position == 0) {
            mCurrentFragment = new ReportListFragment();
            setTitle(APP_TITLE);

        } else if (position == 1) {
            mCurrentFragment = new NotificationListFragment();
            setTitle(mMenuTitles[position]);
        } else if (position == 2) {
            mCurrentFragment = new DashboardFeedFragment();
            setTitle(mMenuTitles[position]);
        } else if (position == 3) {
            mCurrentFragment = new VisualizationFragment();
            setTitle(mMenuTitles[position]);
        }else {
            mCurrentFragment = PlaceholderFragment.newInstance(position + 1);
            setTitle(null);
        }

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, mCurrentFragment, mCurrentFragment.getClass().getSimpleName())
                .commit();

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);

        mDrawerLayout.closeDrawer(mDrawerList);

    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        StyleUtil.setActionBarTitle(this, mTitle.toString());

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showSetting();
            return true;
        }
        if (id == R.id.action_new_event) {
            newReport();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSetting() {
        Intent intent = new Intent(this, SettingActivity.class);
        startActivityForResult(intent, 0);
    }

    private void newReport() {
        Intent intent = new Intent(this, ReportTypeActivity.class);
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        sendScreenViewAnalytic = false;
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (! sharedPrefUtil.isUserLoggedIn()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else {
            if (sendScreenViewAnalytic) {
                // send screen view analytic
                Tracker tracker = ((PoddApplication) getApplication()).getTracker(PoddApplication.TrackerName.APP_TRACKER);
                tracker.setScreenName("ReportList");
                tracker.send(new HitBuilders.AppViewBuilder().build());
            }
            sendScreenViewAnalytic = true;

            // Check device for Play Services APK. If check succeeds, proceed with
            //  GCM registration.
            if (checkPlayServices()) {
                gcm = GoogleCloudMessaging.getInstance(this);
                regid = getRegistrationId();

                if (regid.isEmpty()) {
                    registerInBackground();
                }
            } else {
                Log.i(TAG, "No valid Google Play Services APK found.");

            }

            setNotificationCount();
            refreshDrawerAdapter();
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // handle intent result from notification
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey("id")) {
                String title = extras.getString("title");
                String content = extras.getString("content");
                long id = extras.getLong("id");
                displayWebViewContent(id, title, content);
            }
        }
    }

    private void displayWebViewContent(long id, String title, String content) {
        Intent intent = new Intent(this, WebContentActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("content", content);
        intent.putExtra("id", id);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        notificationDataSource.close();
        unregisterReceiver(mNotificationReceiver);
    }


    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId() {
        String registrationId = sharedPrefUtil.getGCMRegId();
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = sharedPrefUtil.getGCMVersion();
        int currentVersion = BuildConfig.VERSION_CODE;
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(HomeActivity.this);
                    }
                    regid = gcm.register(BuildConfig.GCM_SERVICE_ID);
                    msg = "Device registered, registration ID=" + regid;

                    new RegisterTask().execute((Void[]) null);

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.e(TAG, msg);
            }
        }.execute(null, null, null);
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param regId registration ID
     */
    private void storeRegistrationId(String regId) {
        int appVersion = BuildConfig.VERSION_CODE;
        sharedPrefUtil.setGCMData(regId, appVersion);
    }

    @Override
    public void refreshNotificationCount() {
        setNotificationCount();
        refreshDrawerAdapter();
        // refresh actionbar menu
        supportInvalidateOptionsMenu();
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    public class DrawerAdapter extends ArrayAdapter<String> {

        Context context;
        int resource;
        int unseenNotificationCount;

        public DrawerAdapter(Context context, int resource, String[] titles, int unseenNotificationCount) {
            super(context, resource, titles);
            this.context = context;
            this.resource = resource;
            this.unseenNotificationCount = unseenNotificationCount;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rootView = LayoutInflater.from(context).inflate(resource, parent, false);
            TextView titleView = (TextView) rootView.findViewById(R.id.title);
            titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));

            TextView counterView = (TextView) rootView.findViewById(R.id.counter);
            counterView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
            counterView.setText(String.valueOf(unseenNotificationCount));

            ImageView iconView = (ImageView) rootView.findViewById(R.id.icon);

            if (position == 0) {
                iconView.setImageResource(R.drawable.ic_action_view_as_list);
                counterView.setVisibility(View.INVISIBLE);

            } else if (position == 1) {
                iconView.setImageResource(R.drawable.ic_action_event);

                if (unseenNotificationCount > 0) {
                    counterView.setVisibility(View.VISIBLE);
                } else {
                    counterView.setVisibility(View.INVISIBLE);
                }
            } else if (position == 3) {
                iconView.setImageResource(R.drawable.ic_menu_list);
                counterView.setVisibility(View.INVISIBLE);

            }else {
                counterView.setVisibility(View.INVISIBLE);
            }
            titleView.setText(getItem(position));

            // Re-draw menu.
            invalidateOptionsMenu();

            return rootView;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_home, container, false);
            return rootView;
        }
    }


    /**
     * Post gcm register id
     */
    public class RegisterTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {
            // authenticate and get access token
            String reqData = regid;
            return RequestDataUtil.registerDeviceId(reqData, sharedPrefUtil.getAccessToken());
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);
            JSONObject obj = resp.getJsonObject();

            if (obj != null) {
                // Persist the regID - no need to register again.
                storeRegistrationId(regid);
                return;
            }

        }
    }
}
