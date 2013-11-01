package com.ghelius.narodmon;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class AlarmsListFragment extends ListFragment {

	private AdapterView.OnItemClickListener listener = null;

	public void setOnListItemClickListener (AdapterView.OnItemClickListener listener) {
		this.listener = listener;
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, group, savedInstanceState);
		return v;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (listener != null)
			listener.onItemClick(l, v, position, id);
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onResume () {
		DatabaseHandler dbh = new DatabaseHandler(getActivity().getApplicationContext());
		ArrayList<AlarmSensorTask> tasks = dbh.getAlarmTask();
		super.onResume();
	}
}
