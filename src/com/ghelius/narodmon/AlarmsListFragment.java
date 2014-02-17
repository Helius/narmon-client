package com.ghelius.narodmon;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

public class AlarmsListFragment extends ListFragment {

	private AdapterView.OnItemClickListener listener = null;
    private AlarmListAdapter adapter;
    private final String TAG = "narodmon-listFragment";

	public void setOnListItemClickListener (AdapterView.OnItemClickListener listener) {
		this.listener = listener;
	}

	@Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        adapter = new AlarmListAdapter(getActivity().getApplicationContext(), R.layout.alarm_item);
        setListAdapter(adapter);
    }

    @Override
	public View onCreateView (LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, group, savedInstanceState);
		return v;
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
        Log.d(TAG, "sensor click: " + position);
		if (listener != null)
			listener.onItemClick(l, v, position, id);
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void onResume () {
		super.onResume();
	}
}
