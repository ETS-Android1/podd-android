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

package org.cm.podd.report.model.view;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.RequiresApi;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.ForgetPasswordActivity;

import org.cm.podd.report.fragment.ForgetPasswordFormFragment;
import org.cm.podd.report.model.Config;
import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.util.CustomFilterUtil;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class AddressView extends LinearLayout {

    private final Question question;
    private Spinner [] spinnerViews = null;
    private ArrayAdapter<String> adapter;

    private Context context;
    private Config config;

    private int init = 0;

    private SharedPrefUtil sharedPrefUtil;
    private CustomFilterUtil customFilterUtil;

    public AddressView(final Context context, Question q, final boolean readonly) {
        super(context);

        sharedPrefUtil = new SharedPrefUtil(context);
        customFilterUtil = new CustomFilterUtil();

        this.context = context;
        this.question = q;

        new SyncDataTask().execute((Void[]) null);

        final String hintText = context.getString(R.string.edittext_hint);

        setOrientation(VERTICAL);

        final LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        setLayoutParams(params);
        setTag(q.getName());
        setId(q.getId());


        TextView titleView = new TextView(context);
        titleView.setText(question.getTitle());
        titleView.setLayoutParams(params);
        titleView.setTextAppearance(context, R.style.ReportTextLabel);
        titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));

        addView(titleView);


        String system = "fetchData";
        String key = question.getDataUrl();

        config = sharedPrefUtil.getSyncData(system, key);

        final String[] fields = question.getFilterFields().split(",");
        final EditText editView = new EditText(context);

        spinnerViews = new Spinner[fields.length];
        for (int idx = 0; idx < fields.length; idx++) {

            CustomFilterUtil.FilterWord [] filterWords = new CustomFilterUtil.FilterWord[idx];
            for (int jdx = 0; jdx < idx; jdx++) {
                String _key = fields[jdx];
                Object _value = spinnerViews[jdx].getSelectedItem();
                String[] values = _key.split("\\|");
                if (values.length > 1) {
                    _key = values[0].replaceAll(" ", "");
                }

                if (_value != null) {
                    CustomFilterUtil.FilterWord word = new CustomFilterUtil.FilterWord(_key, _value.toString());
                    filterWords[jdx] = word;
                }
            }

            String[] values = fields[idx].split("\\|");

            String header = fields[idx].replaceAll(" ", "");
            String value = fields[idx].replaceAll(" ", "");

            if (values.length > 1) {
                value = values[0].replaceAll(" ", "");
                header = values[1];
            }

            final ArrayList<String> listData;

            if (config.getValue() == null) {
                listData = new ArrayList<String>();
            } else {
                listData = customFilterUtil.getStringByKey(config.getValue(), value, filterWords);
            }

            adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listData);

            spinnerViews[idx] = new Spinner(context);
            spinnerViews[idx].setLayoutParams(params);
            spinnerViews[idx].setPadding(0, 0, 0, 0);
            spinnerViews[idx].setAdapter(adapter);
            spinnerViews[idx].setSelected(true);

            final int finalIdx = idx;
            spinnerViews[idx].setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    if (init == 0) return;

                    String value = "";
                    String [] findValue = new String[fields.length];
                    for (int idx = 0; idx < fields.length; idx++) {
                        Object selected = spinnerViews[idx].getSelectedItem();
                        if(selected != null) {
                            findValue[idx] = value;
                            value += " " + selected.toString();
                        }

                    }
                    value += "[specific:" + editView.getText().toString() + "]";

                    question.setData(value);

                    // refresh
                    for (int idx = finalIdx + 1; idx < fields.length; idx++) {
                        CustomFilterUtil.FilterWord[] filterWords = new CustomFilterUtil.FilterWord[idx];
                        for (int jdx = 0; jdx < idx; jdx++) {
                            String _key = fields[jdx];
                            Object _value = spinnerViews[jdx].getSelectedItem();
                            if (_value != null) {
                                CustomFilterUtil.FilterWord word = new CustomFilterUtil.FilterWord(_key, _value.toString());
                                filterWords[jdx] = word;
                            }
                        }

                        if (config.getValue() == null) {
                            continue;
                        }

                        value = fields[idx].replaceAll(" ", "");

                        final ArrayList<String> listData = customFilterUtil.getStringByKey(config.getValue(), value, filterWords);
                        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listData);
                        spinnerViews[idx].setAdapter(adapter);

                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }

            });

            Object text = question.getValue();
            if (text != null) {
                for (int i = 0; i < listData.size(); i++) {
                    if (text.toString().toLowerCase().contains(adapter.getItem(i).toLowerCase())) {
                        spinnerViews[idx].setSelection(adapter.getPosition(adapter.getItem(i)));
                    }
                }
            }

            if (readonly) {
                spinnerViews[idx].setEnabled(false);
            }

            TextView headerView = new TextView(context);
            headerView.setLayoutParams(params);
            headerView.setPadding(10, 0, 0, 10);
            headerView.setText(header);

            addView(headerView);
            addView(spinnerViews[idx]);

        }

        TextView headerView = new TextView(context);
        headerView.setLayoutParams(params);
        headerView.setPadding(10, 0, 0, 10);
        headerView.setText(context.getString(R.string.specific_address));

        addView(headerView);

        editView.setLayoutParams(params);
        editView.setPadding(0, 0, 0, 20);
        editView.setClickable(true);
        editView.setHint(hintText);
        editView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                editView.setHint("");
                return false;
            }

        });

        Object text = question.getValue();
        if (text != null) {
            Pattern specificPattern = Pattern.compile("\\[specific:(.*?)\\]");
            Matcher match = specificPattern.matcher(text.toString());
            while (match.find()) {
                String value = match.group(1);
                editView.setText(value);
            }

        }
        addView(editView);

        if (readonly) {
            editView.setFocusable(false);
            editView.setClickable(false);
        }

        if (! readonly) {
            editView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    String text = "";

                    Object value = question.getValue();
                    if (value != null) {
                        text += value.toString();
                    }

                    text += "[specific:" + editable.toString() + "]";
                    question.setData(text);
                }
            });
        }

    }

    private SoftKeyActionHandler listener;
    public void setListener(SoftKeyActionHandler listener) {
        this.listener = listener;
    }


    public interface SoftKeyActionHandler {
        public boolean onSoftKeyAction(TextView view, int actionId, KeyEvent event);
    }

    public class SyncDataTask extends AsyncTask<Void, Void, RequestDataUtil.ResponseObject> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected RequestDataUtil.ResponseObject doInBackground(Void... params) {

            String system = "fetchData";
            String url = question.getDataUrl();

            String accessToken = sharedPrefUtil.getAccessToken();

            return RequestDataUtil.get(url, null, accessToken);
        }

        @Override
        protected void onPostExecute(RequestDataUtil.ResponseObject resp) {
            super.onPostExecute(resp);

            if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
                String system = "fetchData";
                String key = question.getDataUrl();

                JSONObject response = null;
                try {
                    response = new JSONObject(resp.getRawData());
                    sharedPrefUtil.setSyncData(system, key, response.getString("results"));

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (config.getValue() == null) {

                    config = sharedPrefUtil.getSyncData(system, key);

                    if (config.getValue() == null) return;

                    final String[] fields = question.getFilterFields().split(",");
                    for (int idx = 0; idx < fields.length; idx++) {

                        CustomFilterUtil.FilterWord[] filterWords = new CustomFilterUtil.FilterWord[idx];
                        for (int jdx = 0; jdx < idx; jdx++) {
                            String _key = fields[jdx];
                            Object _value = spinnerViews[jdx].getSelectedItem();

                            if (_value != null) {
                                CustomFilterUtil.FilterWord word = new CustomFilterUtil.FilterWord(_key, _value.toString());
                                filterWords[jdx] = word;
                            }
                        }
                        String value = fields[idx].replaceAll(" ", "");
                        final ArrayList<String> listData = customFilterUtil.getStringByKey(config.getValue(), value, filterWords);
                        adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, listData);
                        spinnerViews[idx].setAdapter(adapter);

                        Object text = question.getValue();
                        if (text != null) {
                            for (int i = 0; i < listData.size(); i++) {
                                if (text.toString().toLowerCase().contains(adapter.getItem(i).toLowerCase())) {
                                    spinnerViews[idx].setSelection(adapter.getPosition(adapter.getItem(i)));
                                }
                            }
                        }
                    }
                }

                init = 1;

            } else {
                // show error
            }
        }
    }

}
