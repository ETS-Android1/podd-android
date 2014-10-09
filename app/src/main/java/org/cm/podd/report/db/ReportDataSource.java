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

package org.cm.podd.report.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by pphetra on 10/8/14 AD.
 */
public class ReportDataSource {

    private ReportDatabaseHelper reportDatabaseHelper;
    private ReportImageDatabaseHelper reportImageDatabaseHelper;

    public ReportDataSource(Context context) {
        reportDatabaseHelper = new ReportDatabaseHelper(context);
        reportImageDatabaseHelper = new ReportImageDatabaseHelper(context);
    }

    /**
     * create draft report
     * @return row id of new report
     */
    public long createDraftReport() {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        return db.insert("report", "form_data", new ContentValues());
    }

    public Cursor getAll() {
        return reportDatabaseHelper.getReadableDatabase().rawQuery("SELECT * FROM report", null);
    }

}
