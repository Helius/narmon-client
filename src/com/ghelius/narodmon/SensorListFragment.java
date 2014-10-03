package com.ghelius.narodmon;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class SensorListFragment extends ListFragment {

	private OnSensorListClickListener listener = null;
    private final static String TAG = "narodmon-listfragment";
//    Button more;
    private SensorItemAdapter listAdapter;
    private int iprev;
    private int i2prev;
    private int i3prev;
    private TextView emptyTextView;
    private Button more;

    public void setEmptyMessage(String emptyMessage) {
        emptyTextView.setText(emptyMessage);
    }
//    private TextView msgTextView;

    interface OnSensorListClickListener {
       void onItemClick (ListView l, View v, int position, long id);
       void scrollOverDown();
       void moreButtonPressed();
    }

    public void setOnListItemClickListener (OnSensorListClickListener listener) {
		this.listener = listener;
	}

    private TextView noItems(String text) {
        TextView emptyView = new TextView(getActivity());
        //Make sure you import android.widget.LinearLayout.LayoutParams;
        emptyView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        //Instead of passing resource id here I passed resolved color
        //That is, getResources().getColor((R.color.gray_dark))
//        emptyView.setTextColor(getResources().getColor(R.color.));
        emptyView.setText(text);
        emptyView.setTextSize(14);
        emptyView.setVisibility(View.GONE);
        emptyView.setGravity(Gravity.CENTER_VERTICAL
                | Gravity.CENTER_HORIZONTAL);

        //Add the view to the list view. This might be what you are missing
        ((ViewGroup) getListView().getParent()).addView(emptyView);

        return emptyView;
    }
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup group, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, group, savedInstanceState);
        return v;
	}
    @Override
    public void onStart() {
        super.onStart();
        emptyTextView = noItems("");
        getListView().setEmptyView(emptyTextView);
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
                Log.d(TAG,"more: event " + i + ", " + i2 + "," + i3);
                if (i3 > 0 && i+i2 > (i3-1) && listener != null) {
                    if (i3 != i3prev && i2 != i2prev && i != iprev) {
                        listener.scrollOverDown();
                        i3prev = i3;
                        i2prev = i2;
                        iprev = i;
                    }
                }
            }
        });
        more = new Button (getActivity().getApplicationContext());
        more.setText(getString(R.string.more_button_text));
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listener!= null)
                    listener.moreButtonPressed();
            }
        });
        getListView().addFooterView(more);
    }

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (listener != null)
			listener.onItemClick(l, v, position, id);
		super.onListItemClick(l, v, position, id);
	}

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (menu.findItem(R.id.menu_refresh) == null) {
            inflater.inflate(R.menu.icon_menu, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    public void showMoreButton (boolean show) {
        more.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    /* Called whenever we call invalidateOptionsMenu() */
//    @Override
//    public void onPrepareOptionsMenu(Menu menu) {
//        menu.clear();
//        super.onPrepareOptionsMenu(menu);
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//
//        // Handle action buttons
//        switch (item.getItemId()) {
//            case R.id.menu_refresh:
//                Log.d(TAG, "refresh menu pressed...");
////                getSensorsList(deviceRequestLimit);
//                break;
//            case R.id.menu_filter:
//                Log.d(TAG, "filte menu pressed..");
////                getSensorsList(deviceRequestLimit);
//                break;
//            default:
//        }
//        return super.onOptionsItemSelected(item);
//    }
}
