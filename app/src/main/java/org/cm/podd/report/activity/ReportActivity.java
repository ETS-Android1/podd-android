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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

import org.cm.podd.report.R;
import org.cm.podd.report.db.ReportDataSource;
import org.cm.podd.report.db.ReportTypeDataSource;
import org.cm.podd.report.fragment.ReportConfirmFragment;
import org.cm.podd.report.fragment.ReportImageFragment;
import org.cm.podd.report.fragment.ReportNavigationInterface;
import org.cm.podd.report.model.Form;
import org.cm.podd.report.model.FormIterator;
import org.cm.podd.report.model.Page;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.Report;
import org.cm.podd.report.model.validation.ValidationResult;
import org.cm.podd.report.model.view.PageView;
import org.cm.podd.report.service.LocationBackgroundService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.keyboardsurfer.android.widget.crouton.Configuration;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class ReportActivity extends ActionBarActivity implements ReportNavigationInterface {

    private static final String TAG = "ReportActivity";
    private Button prevBtn;
    private Button nextBtn;

    private String currentFragment;
    private ReportDataSource reportDataSource;
    private ReportTypeDataSource reportTypeDataSource;
    private long reportId;
    private long reportType;
    private FormIterator formIterator;

    protected double currentLatitude = 0.00;
    protected double currentLongitude = 0.00;
    protected String currentLocationProvider;

    protected BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentLatitude = intent.getDoubleExtra("Latitude", 0.00);
            currentLongitude = intent.getDoubleExtra("Longitude", 0.00);
            currentLocationProvider = intent.getStringExtra("Provider");

            Log.d(TAG, "current location = " + currentLatitude + "," + currentLongitude);
            reportDataSource.updateLocation(reportId, currentLatitude, currentLongitude);
            stopLocationService();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_report);
        prevBtn = (Button) findViewById(R.id.prevBtn);
        nextBtn = (Button) findViewById(R.id.nextBtn);
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextScreen();
            }
        });
        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        reportDataSource = new ReportDataSource(this);
        reportTypeDataSource = new ReportTypeDataSource(this);


        if (savedInstanceState != null) {
            currentFragment = savedInstanceState.getString("currentFragment");
            reportId = savedInstanceState.getLong("reportId");
            reportType = savedInstanceState.getLong("reportType");
            formIterator = (FormIterator) savedInstanceState.getSerializable("formIterator");

            currentLatitude = savedInstanceState.getDouble("currentLatitude");
            currentLongitude = savedInstanceState.getDouble("currentLongitude");
            if (currentLongitude == 0.00 && currentLatitude == 0.00) {
                startLocationService();
            }

        } else {
            Intent intent = getIntent();
            reportType = intent.getLongExtra("reportType", 0);
            reportId = intent.getLongExtra("reportId", -99);
            formIterator = new FormIterator(reportTypeDataSource.getForm(reportType));

            if (reportId == -99) {
                reportId = reportDataSource.createDraftReport(reportType);
                startLocationService();
            } else {
                loadFormData();
            }

            nextScreen();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("currentFragment", currentFragment);
        outState.putLong("reportId", reportId);
        outState.putLong("reportType", reportType);
        outState.putSerializable("formIterator", formIterator);
        outState.putDouble("currentLatitude", currentLatitude);
        outState.putDouble("currentLongitude", currentLongitude);
        super.onSaveInstanceState(outState);
    }

    private void loadFormData() {

        Form form = formIterator.getForm();

        Report report = reportDataSource.getById(reportId);
        String formDataStr = report.getFormData();
        Log.d(TAG, "form data = " + formDataStr);
        if (formDataStr != null) {
            try {
                JSONObject jsonObject = new JSONObject(formDataStr);
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String[] ary = key.split("@@@");
                    int qid = Integer.parseInt(ary[0]);
                    String name = ary[1];

                    Question question = form.getQuestion(qid);
                    if (question != null) {
                        String value = jsonObject.getString(key);
                        if (value != null) {
                            question.setData(name, question.getDataType().parseFromString(value));
                        }
                    } else {
                        Log.d(TAG, "Question not found. key= " + key);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, "error parsing form_data", e);
            }
        }

        if (report.getLatitude() == 0.00 && report.getLongitude() == 0.00) {
            startLocationService();
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "from fragment = " + currentFragment);

        if (currentFragment != null) {
//            if (currentFragment.equals(ReportLocationFragment.class.getName())) {
//                currentFragment = ReportImageFragment.class.getName();
//                setPrevVisible(false);
//            } else
            if (currentFragment.equals(ReportImageFragment.class.getName())) {
                currentFragment = null;
            } else if (currentFragment.equals(ReportConfirmFragment.class.getName())) {
                currentFragment = "dynamicForm";
                setNextVisible(true);
            } else if (currentFragment.equals("dynamicForm")) {
                if (! formIterator.previousPage()) {
                    //currentFragment = ReportLocationFragment.class.getName();
                    currentFragment = ReportImageFragment.class.getName();
                }
            }
        }
        Log.d(TAG, "back to fragment = " + currentFragment);
    }

    private void nextScreen() {
        Fragment fragment = null;
        boolean isDynamicForm = false;

        hideKeyboard();

        if (currentFragment == null) { /* first screen */
            Log.d(TAG, "first screen");
            fragment = ReportImageFragment.newInstance(reportId);
        } else {
//            if (currentFragment.equals(ReportImageFragment.class.getName())) {
//                fragment = ReportLocationFragment.newInstance(reportId);
//
//            } else
//
            if (currentFragment.equals(ReportConfirmFragment.class.getName())) {
                /* do nothing */

            } else {
                isDynamicForm = true;

                setNextVisible(true);
                setPrevVisible(true);
                setNextEnable(true);
                setPrevEnable(true);

                // case I
                // just come into this dynamic form
                // serving fragment(currentPage)
                // case II
                // we are not in first page
                // and not in last page
                // so we proceed to nextPage
                // case III
                // we are at last page
                // so we skip to ReportConfirmFragment
                //if (currentFragment.equals(ReportLocationFragment.class.getName())) {
                if (currentFragment.equals(ReportImageFragment.class.getName())) {
                    // no-op
                    fragment = getPageFramgment(formIterator.getCurrentPage());
                } else if (formIterator.isAtLastPage()) {
                    fragment = ReportConfirmFragment.newInstance(reportId);
                    isDynamicForm = false;
                } else {
                    if (! formIterator.nextPage()) {

                        // validation case
                        List<ValidationResult> validateResults = formIterator.getCurrentPage().validate();
                        if (validateResults.size() > 0) {
                            StringBuffer buff = new StringBuffer();
                            for (ValidationResult vr : validateResults) {
                                buff.append(vr.getMessage()).append("\n");
                            }
                            final Crouton crouton = Crouton.makeText(this, buff.toString(), Style.ALERT);
                            crouton.setConfiguration(new Configuration.Builder().setDuration(1000).build());
                            crouton.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Crouton.hide(crouton);
                                }
                            });
                            crouton.show();

                        } else {
                            // end if no valid transition and no validation results
                            fragment = ReportConfirmFragment.newInstance(reportId);
                            isDynamicForm = false;
                        }

                    } else {

                        fragment = getPageFramgment(formIterator.getCurrentPage());

                    }
                }

            }
        }

        if (fragment != null) {

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (currentFragment == null) {
                Log.d(TAG, "add fragment");
                transaction.add(R.id.container, fragment);
            } else {
                Log.d(TAG, "replace fragment");
                transaction.replace(R.id.container, fragment);
                transaction.addToBackStack(fragment.getClass().getName());
            }
            transaction.commit();

            if (isDynamicForm) {
                currentFragment = "dynamicForm";
            } else {
                currentFragment = fragment.getClass().getName();
            }

        }

        Log.d("----", "current fragment = " + currentFragment);
    }

    private Fragment getPageFramgment(Page page) {
        FormPageFragment fragment = new FormPageFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("page", formIterator.getCurrentPage());
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.report, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setNextEnable(boolean flag) {
        nextBtn.setEnabled(flag);
    }

    @Override
    public void setPrevEnable(boolean flag) {
        prevBtn.setEnabled(flag);
    }

    @Override
    public void setNextVisible(boolean flag) {
        if (flag) {
            nextBtn.setVisibility(View.VISIBLE);
        } else {
            nextBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPrevVisible(boolean flag) {
        if (flag) {
            prevBtn.setVisibility(View.VISIBLE);
        } else {
            prevBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void finishReport() {
        saveForm();

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationService();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private void saveForm() {
        Map<String, Object> data = formIterator.getData(false);
        String jsonData = new JSONObject(data).toString();
        Log.d(TAG, jsonData);
        reportDataSource.updateData(reportId, jsonData, 0 /* draft = 0*/);
    }

    public void startLocationService() {
        Log.i(TAG, "startLocationService");
        startService(new Intent(this, LocationBackgroundService.class));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(LocationBackgroundService.BROADCAST_ACTION));
    }

    public void stopLocationService() {
        Log.i(TAG, "stopLocationService");
        stopService(new Intent(this, LocationBackgroundService.class));
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class FormPageFragment extends Fragment {

        private Page page;

        public FormPageFragment() {
            super();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Bundle arguments = getArguments();
            Page page = (Page) arguments.get("page");
            PageView pageView = new PageView(getActivity(), page);
            return pageView;
        }
    }

    public void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
    }
}
