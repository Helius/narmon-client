package com.ghelius.narodmon;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler extends SQLiteOpenHelper {
	// All Static variables
	// Database Version
	private static final int DATABASE_VERSION = 1;
	// Database Name
	private static final String DATABASE_NAME = "miscDataBase";
	// Widgets table name
	private static final String TABLE_WIDGETS = "widget";

	// Widgets Table Columns names
	private static final String KEY_WIDGET_ID = "widget_id";
	private static final String KEY_SENSOR_ID = "sensor_id";
	private static final String KEY_NAME = "name";

	public DatabaseHandler(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	// Creating Tables
	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_WIDGETS + "("
				+ KEY_WIDGET_ID + " INTEGER," + KEY_SENSOR_ID + " INTEGER,"
				+ KEY_NAME + " TEXT" + ")";
		db.execSQL(CREATE_CONTACTS_TABLE);
	}

	// Upgrading database
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older table if existed
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIDGETS);

		// Create tables again
		onCreate(db);
	}

	/**
	 * All CRUD(Create, Read, Update, Delete) Operations
	 */

	// Adding new contact
	void addWidget(Widget widget) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(KEY_WIDGET_ID, widget.widgetId);
		values.put(KEY_SENSOR_ID, widget.sensorId);
		values.put(KEY_NAME, widget.widgetId);

		// Inserting Row
		db.insert(TABLE_WIDGETS, null, values);
		db.close(); // Closing database connection
	}

	// Getting single contact
	Widget getWidget(int id) {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(TABLE_WIDGETS, new String[] { KEY_WIDGET_ID,
				KEY_SENSOR_ID, KEY_NAME }, KEY_WIDGET_ID + "=?",
				new String[] { String.valueOf(id) }, null, null, null, null);
		if (cursor != null)
			cursor.moveToFirst();

		Widget widget = new Widget(Integer.parseInt(cursor.getString(0)),
				Integer.parseInt(cursor.getString(1)), cursor.getString(2));
		// return contact
		db.close();
		return widget;
	}

	// Getting All Widget
	public List<Widget> getAllWidgets() {
		List<Widget> widgetList = new ArrayList<Widget>();
		// Select All Query
		String selectQuery = "SELECT  * FROM " + TABLE_WIDGETS;

		SQLiteDatabase db = this.getWritableDatabase();
		Cursor cursor = db.rawQuery(selectQuery, null);

		// looping through all rows and adding to list
		if (cursor.moveToFirst()) {
			do {
				// Adding widgets to list
				widgetList.add(new Widget(Integer.parseInt(cursor.getString(0)), Integer.parseInt(cursor.getString(1)), cursor.getString(2)));
			} while (cursor.moveToNext());
		}
		// return contact list
		cursor.close();
		db.close();
		return widgetList;
	}

//	public int updateWidget(Contact contact) {
//		SQLiteDatabase db = this.getWritableDatabase();
//
//		ContentValues values = new ContentValues();
//		values.put(KEY_NAME, contact.getName());
//		values.put(KEY_PH_NO, contact.getPhoneNumber());
//
//		// updating row
//		return db.update(TABLE_WIDGETS, values, KEY_ID + " = ?",
//				new String[] { String.valueOf(contact.getID()) });
//	}

	// Deleting single contact
	public void deleteWidget(Widget widget) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_WIDGETS, KEY_WIDGET_ID + " = ?",
				new String[] { String.valueOf(widget.widgetId) });
		db.close();
	}

	public void deleteWidget(int widgetId) {
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(TABLE_WIDGETS, KEY_WIDGET_ID + " = ?",
				new String[] { String.valueOf(widgetId) });
		db.close();
	}


	// Getting widgetCount
	public int getWidgetCount() {
		String countQuery = "SELECT  * FROM " + TABLE_WIDGETS;
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor cursor = db.rawQuery(countQuery, null);
		int count = cursor.getCount();
		cursor.close();
		db.close();
		return count;
	}
}
