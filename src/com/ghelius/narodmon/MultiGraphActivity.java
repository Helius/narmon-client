package com.ghelius.narodmon;

import android.os.Bundle;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class MultiGraphActivity extends SherlockFragmentActivity {
	public void onCreate(Bundle savedInstanceState) {
		setTheme(com.actionbarsherlock.R.style.Theme_Sherlock);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.multi_graph_activity);
		// create big chart with multiple series
	}
}