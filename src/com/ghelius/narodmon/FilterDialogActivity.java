package com.ghelius.narodmon;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

public class FilterDialogActivity extends Activity {
	UiFlags uiFlags = null;
	ListView typeListView;
	CheckedListItemAdapter typeAdapter;
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		uiFlags = UiFlags.load(this);
		setContentView(R.layout.filter_dialog);
		typeAdapter = new CheckedListItemAdapter (this, SensorTypeProvider.getInstance(this).getTypesList());
		typeListView = (ListView) findViewById(R.id.typeListView);
		typeListView.setAdapter(typeAdapter);
	}
}