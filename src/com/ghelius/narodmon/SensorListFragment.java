package com.ghelius.narodmon;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;

public class SensorListFragment extends ListFragment {

	private OnSensorListClickListener listener = null;
    private final static String TAG = "narodmon-listfragment";
//    Button more;
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
//        more = new Button(getActivity().getApplicationContext());
//        more.setText(getActivity().getString(R.string.more_button_text));
//        more.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (listener != null)
//                    listener.onFooterClick();
//            }
//        });
		return v;
	}

    @Override
    public void onActivityCreated (Bundle savedInstance) {
        super.onActivityCreated(savedInstance);
//        if (listAdapter != null)
//            setListAdapter(listAdapter);
        getListView().setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i2, int i3) {
                if (i3 > 0 && i+i2 == i3 && listener != null) {
                    Log.d(TAG,"more: event");
                    listener.onFooterClick();
                }
            }
        });
    }


	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (listener != null)
			listener.onItemClick(l, v, position, id);
		super.onListItemClick(l, v, position, id);
	}

}
