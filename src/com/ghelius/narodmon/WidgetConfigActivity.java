package com.ghelius.narodmon;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class WidgetConfigActivity extends SherlockFragmentActivity {

	public void onCreate(Bundle savedInstanceState) {
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.widget_config);
		setResult(RESULT_CANCELED);
	}
}
