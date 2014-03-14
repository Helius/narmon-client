package com.ghelius.narodmon;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

public class SensorListFragment extends ListFragment {

	private OnSensorListClickListener listener = null;
    private final static String TAG = "narodmon-listfragment";
    Button more;
    private SensorItemAdapter listAdapter;

    interface OnSensorListClickListener {
       void onItemClick (ListView l, View v, int position, long id);
       void onFooterClick ();
    }

    public void setOnListItemClickListener (OnSensorListClickListener listener) {
		this.listener = listener;
	}

	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, group, savedInstanceState);
        more = new Button(getActivity().getApplicationContext());
        more.setText(getActivity().getString(R.string.more_button_text));
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener != null)
                    listener.onFooterClick();
            }
        });
		return v;
	}

    @Override
    public void onActivityCreated (Bundle savedInstance) {
        super.onActivityCreated(savedInstance);
        getListView().addFooterView(more);
        if (listAdapter != null)
            setListAdapter(listAdapter);
    }


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (listener != null)
			listener.onItemClick(l, v, position, id);
		super.onListItemClick(l, v, position, id);
	}

    public void setAdapter(SensorItemAdapter listAdapter) {
        this.listAdapter = listAdapter;
    }
}
