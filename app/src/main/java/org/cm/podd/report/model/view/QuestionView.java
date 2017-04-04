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

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.HomeActivity;
import org.cm.podd.report.db.ConfigurationDataSource;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.model.Config;
import org.cm.podd.report.model.DataType;
import org.cm.podd.report.model.Question;
import org.cm.podd.report.model.ReportType;
import org.cm.podd.report.service.CommentService;
import org.cm.podd.report.service.ConfigService;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by pphetra on 9/30/14 AD.
 */
public class QuestionView extends LinearLayout {

    private final Question question;
    private EditText editView = null;
    private AutoCompleteTextView autoCompleteTextView = null;
    private DatePicker calendarView = null;
    private Spinner [] spinnerViews = null;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public QuestionView(final Context context, Question q, final boolean readonly) {
        super(context);
        this.question = q;
        final String hintText = context.getString(R.string.edittext_hint);

        setOrientation(VERTICAL);

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 24);
        setLayoutParams(params);
        setTag(q.getName());
        setId(q.getId());


        TextView titleView = new TextView(context);
        titleView.setText(question.getTitle());
        titleView.setLayoutParams(params);
        titleView.setTextAppearance(context, R.style.ReportTextLabel);
        titleView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
        titleView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (! readonly) {
                    editView.requestFocus();
                    (new android.os.Handler()).postDelayed(new Runnable() {

                        public void run() {
                            editView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                            editView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
                        }
                    }, 200);

                }
            }
        });
        addView(titleView);

        if (question.getDataType() == DataType.DATE) {
            calendarView = new DatePicker(context);
            calendarView.setCalendarViewShown(true);
            calendarView.setSpinnersShown(false);
            calendarView.setLayoutParams(params);
            calendarView.setPadding(0, 0, 0, 0);

            Date value = (Date) question.getValue();
            if (value == null) {
                value = new Date();
            }

            Calendar c = Calendar.getInstance();
            c.setTime(value);
            calendarView.init(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE), new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker datePicker, int year, int month, int day) {
                    Calendar c = Calendar.getInstance();
                    c.set(year, month, day, 0, 0, 0);
                    question.setData(c.getTime());
                }
            });

            addView(calendarView);

        } else if (question.getDataType() == DataType.ADDRESS) {

            String system = "fetchData";
            String key = question.getDataUrl();
            startSyncConfigService(context, system, key);

            ConfigurationDataSource dbSource = new ConfigurationDataSource(context);
            Config config = dbSource.getConfigValue(system, key);

            String[] fields = question.getFilterFields().split(",");
            ArrayList[] listData = new ArrayList[fields.length];
            for (int idx = 0; idx < fields.length; idx++) {
                listData[idx] = new ArrayList<String>();
            }

            if (config != null) {
                try {
                    JSONArray items = new JSONArray(config.getValue());
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        for (int j = 0; j < fields.length; j++) {
                            String value = item.getString(fields[j].replaceAll(" ", ""));
                            listData[j].add(value);
                        }
                    }
                } catch (JSONException e) {

                }
            }

            spinnerViews = new Spinner[fields.length];

            for (int idx = 0; idx < listData.length; idx++) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, listData[idx]);

                spinnerViews[idx] = new Spinner(context);
                spinnerViews[idx].setLayoutParams(params);
                spinnerViews[idx].setPadding(0, 0, 0, 0);
                spinnerViews[idx].setAdapter(adapter);

                addView(spinnerViews[idx]);

            }

        } else if (question.getDataType() == DataType.AUTOCOMPLETE) {

            String system = "fetchData";
            String key = question.getDataUrl();

            startSyncConfigService(context, system, key);

            ConfigurationDataSource dbSource = new ConfigurationDataSource(context);
            Config config = dbSource.getConfigValue(system, key);

            ArrayList<String> listData = new ArrayList<String>();
            if (config != null) {
                try {
                    JSONArray items = new JSONArray(config.getValue());
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.getJSONObject(i);
                        String name = item.getString("name");
                        listData.add(name);
                    }

                } catch (JSONException e) {

                }
            }

            AutocompleteAdapter adapter = new AutocompleteAdapter(context, android.R.layout.simple_dropdown_item_1line, listData);
            adapter.getFilter();

            autoCompleteTextView = new AutoCompleteTextView(context);
            autoCompleteTextView.setLayoutParams(params);

            autoCompleteTextView.setAdapter(adapter);
            addView(autoCompleteTextView);
        } else {
            editView = new EditText(context);
            editView.setLayoutParams(params);
            editView.setPadding(0, 0, 0, 0);
            editView.setClickable(true);
            editView.setHint(hintText);
            editView.setOnTouchListener(new OnTouchListener() {

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    editView.setHint("");
                    return false;
                }

            });

            editView.setOnFocusChangeListener(new OnFocusChangeListener() {

                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        editView.setHint(hintText);
                    }
                }
            });
            editView.setTextAppearance(context, R.style.EditTextFlat);
            editView.setTypeface(StyleUtil.getDefaultTypeface(context.getAssets(), Typeface.NORMAL));
            if (readonly) {
                editView.setKeyListener(null);
                editView.setEnabled(false);
            }
            int type = 0;
            if (question.getDataType() == DataType.INTEGER) {
                type = InputType.TYPE_CLASS_NUMBER;
            }
            if (question.getDataType() == DataType.DOUBLE) {
                type = InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL;
            }
            if (question.getDataType() == DataType.STRING) {
                type = InputType.TYPE_CLASS_TEXT;
            }
            editView.setInputType(type);
            Object value = question.getValue();
            if (value != null) {
                if (question.getDataType() == DataType.DOUBLE) {
                    editView.setText(String.format( "%.2f", value ));
                } else {
                    editView.setText(value.toString());
                }
            }
            addView(editView);

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
                        question.setData(question.getDataType().parseFromString(editable.toString()));
                    }
                });

                editView.setOnFocusChangeListener(new OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        if (! hasFocus) {
                            question.setData(question.getDataType().parseFromString(editView.getText().toString()));
                        }
                    }
                });

                editView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (listener != null) {
                            return listener.onSoftKeyAction(v, actionId, event);
                        }
                        return false;
                    }
                });
            }
        }


    }

    private void startSyncConfigService(Context context, String system, String key) {
        Intent intent = new Intent(context, ConfigService.class);
        intent.putExtra("system", system);
        intent.putExtra("key", key);
        intent.putExtra("url", key);
        context.startService(intent);
    }

    private SoftKeyActionHandler listener;

    public void setListener(SoftKeyActionHandler listener) {
        this.listener = listener;
    }

    public interface SoftKeyActionHandler {
        public boolean onSoftKeyAction(TextView view, int actionId, KeyEvent event);
    }

    public void askForFocus() {
        if (editView != null) {
            (new Handler()).postDelayed(new Runnable() {
                public void run() {
                    editView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                    editView.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP , 0, 0, 0));
                }
            }, 100);
        }
    }


    class StringContainFilter extends Filter {

        AutocompleteAdapter adapter;
        ArrayList<String> originalList;
        ArrayList<String> filteredList;

        public StringContainFilter(AutocompleteAdapter adapter, ArrayList<String> originalList) {
            super();
            this.adapter = adapter;
            this.originalList = (ArrayList<String>) originalList.clone();
            this.filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(originalList);
            } else {
                final String filterPattern = constraint.toString().toLowerCase().trim();

                for (final String text : originalList) {
                    if (text.contains(filterPattern)) {
                        filteredList.add(text);
                    }
                }
            }
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            adapter.filteredString.clear();
            adapter.filteredString.addAll((ArrayList) results.values);
            adapter.notifyDataSetChanged();
        }
    }

    public class AutocompleteAdapter extends ArrayAdapter<String> implements Filterable {

        Context context;
        ArrayList<String> filteredString = new ArrayList<String>();

        public AutocompleteAdapter(Context context, int resource, ArrayList<String> filteredString) {
            super(context, resource, filteredString);
            this.context = context;
            this.filteredString = filteredString;
        }

        @Override
        public int getCount() {
            return filteredString.size();
        }

        @Override
        public Filter getFilter() {
            return new StringContainFilter(this, filteredString);
        }


    }

}
