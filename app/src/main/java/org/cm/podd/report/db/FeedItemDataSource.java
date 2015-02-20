package org.cm.podd.report.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.ListAdapter;

import org.cm.podd.report.model.FeedItem;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by siriwat on 2/19/15.
 */
public class FeedItemDataSource {

    private static final String TAG = "FeedItemDataSource";

    private ReportDatabaseHelper reportDatabaseHelper;
    private static final String TABLE_NAME = "feed_item";

    private static int COLUMN_ID         = 0;
    private static int COLUMN_ITEM_ID    = 1;
    private static int COLUMN_TYPE       = 2;
    private static int COLUMN_DATE       = 3;
    private static int COLUMN_JSONSTRING = 4;
    private static int COLUMN_CREATED_AT = 5;
    private static int COLUMN_UPDATED_AT = 6;

    public FeedItemDataSource(Context context) {
        reportDatabaseHelper = new ReportDatabaseHelper(context);
    }

    public void save(FeedItem feedItem) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        Cursor existingRow;
        String[] columns = new String[] { "_id" };

        // Fetch the existing first.
        existingRow = db.query(TABLE_NAME, columns, "item_id = ? AND type = ?",
                new String[]{ Long.toString(feedItem.getItemId()), "report" },
                null, null, null);

        ContentValues values = new ContentValues();
        values.put("item_id", feedItem.getItemId());
        values.put("type", feedItem.getType());
        values.put("json_string", feedItem.getJsonString());
        values.put("date", feedItem.getDate().getTime());

        Date now = new Date();

        if (existingRow != null && existingRow.moveToNext()) {
            // Then update instead.
            values.put("updated_at", now.getTime());
            db.update(TABLE_NAME, values, "_id = ?",
                    new String[] { Long.toString(existingRow.getLong(COLUMN_ID)) });
        } else {
            values.put("created_at", now.getTime());
            values.put("updated_at", now.getTime());
            db.insert(TABLE_NAME, null, values);
        }

        if (existingRow != null) {
            existingRow.close();
        }
        db.close();
    }

    public void clear() {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

    public ArrayList<FeedItem> latest() {
        return latest(20);
    }

    public ArrayList<FeedItem> latest(int limit) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ArrayList<FeedItem> feedItems = new ArrayList<FeedItem>();
        FeedItem feedItem;

        if (limit == 0) {
            limit = 20;
        }

        Cursor result = db.query(true,
                TABLE_NAME, null, null, null, null, null, "date DESC", Integer.toString(limit));

        while (result.moveToNext()) {
            feedItem = loadByCursor(result);
            if (feedItem != null) {
                feedItems.add(feedItem);
            }
        }

        return feedItems;
    }

    public FeedItem loadById(long id) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        FeedItem feedItem = null;

        Cursor result = db.query(TABLE_NAME, null, "_id = ?",
                new String[]{ Long.toString(id) }, null, null, null);

        try {
            if (result.moveToNext()) {
                feedItem = loadByCursor(result);
            }
        } finally {
            result.close();
            db.close();
        }

        return feedItem;
    }

    public FeedItem loadByCursor(Cursor cursor) {
        FeedItem feedItem = new FeedItem();

        try {
            JSONObject jsonObject = new JSONObject(cursor.getString(COLUMN_JSONSTRING));

            feedItem.setJsonString(cursor.getString(COLUMN_JSONSTRING));
            feedItem.setId(cursor.getLong(COLUMN_ID));
            feedItem.setItemId(jsonObject.getLong("id"));
            feedItem.setType("report");
            feedItem.setDate(new Date(cursor.getLong(COLUMN_DATE)));
            feedItem.setCreatedAt(new Date(cursor.getLong(COLUMN_CREATED_AT)));
            feedItem.setUpdatedAt(new Date(cursor.getLong(COLUMN_UPDATED_AT)));
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON string", e);
            return null;
        }

        return feedItem;
    }

}
