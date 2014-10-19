package com.ghelius.narodmon;


import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class SlidingMenuFragment extends Fragment {

    private final static String TAG="narodmon-menuFragment";
    private ArrayList<View> menuItems = new ArrayList<View>();
    private MenuClickListener listener;
    private Integer lastSelectedItemPosition;
    final int menuBackgroundColor = Color.argb(0xff, 0x01, 0x34, 0x6E);


    interface MenuClickListener {
        void menuAllClicked();
        void menuWatchedClicked();
        void menuMyClicked();
        void menuAlarmClicked();
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG,"onAttach");
        super.onAttach(activity);
        try {
            listener = (MenuClickListener) activity;
        } catch (Exception e) {
            Log.e(TAG, "Activity needs to implement MenuClickListener");
        }
    }

    @Override
    public void onDetach() {
        Log.d(TAG,"onDetach");
        super.onDetach();
        listener = null;
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        Log.d(TAG,"onCreate");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
        View v = inflater.inflate(R.layout.sliding_menu_fragment, null);
        menuItems.clear();

        // collect menu item views
		menuItems.add(v.findViewById(R.id.menu_item0));
		menuItems.add(v.findViewById(R.id.menu_item1));
		menuItems.add(v.findViewById(R.id.menu_item2));
		menuItems.add(v.findViewById(R.id.menu_item3));
		menuItems.add(v.findViewById(R.id.menu_item5));
        menuItems.get(0).setBackgroundColor(menuBackgroundColor);

		int i = 0;
		for (View view : menuItems) {
			view.setTag(i++);
			view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
                    lastSelectedItemPosition = (Integer)v.getTag();
					clearMenuSelection();
					Log.d(TAG, "click! " + lastSelectedItemPosition);
					v.setBackgroundColor(menuBackgroundColor);
                    callListenerMethod(lastSelectedItemPosition);
				}
			});
		}
        return v;
    }

    private void callListenerMethod (Integer position) {
        if (listener == null)
            return;
        Log.d(TAG,"callListenerMethod " + position);
        lastSelectedItemPosition = position;
        switch (position) {
            case 0: // all
                listener.menuAllClicked();
                break;
            case 1: // watched
                listener.menuWatchedClicked();
                break;
            case 2: // my
                listener.menuMyClicked();
                break;
            case 3: // alarm
                listener.menuAlarmClicked();
                break;
//						case 4: // graph
//							menuGraphClicked();
//							setTitle("Graphs");
//							break;
            default:
                Log.d(TAG, "unknown tag");
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        lastSelectedItemPosition = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getInt("lastMenuItem",0);
        Log.d(TAG,"onResume, last selection is " + lastSelectedItemPosition);
        clearMenuSelection();
        for (View v : menuItems) {
            if (v.getTag() == lastSelectedItemPosition) {
                v.setBackgroundColor(menuBackgroundColor);
                callListenerMethod(lastSelectedItemPosition);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG,"onPause: save selection " + lastSelectedItemPosition);
        PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit().
                putInt("lastMenuItem",lastSelectedItemPosition).commit();
    }

    private void clearMenuSelection () {
        for (View item: menuItems ) {
            item.setBackgroundColor(Color.BLACK);
        }
    }

    public void setMenuAllCount (int count) {
        Log.d(TAG,"updateSensorCount setMenuAllCount: " + count);
        if (menuItems.size()!=0)
            ((TextView)menuItems.get(0).findViewById(R.id.cnt)).setText(String.valueOf(count));
    }
    public void setMenuWatchCount (int count) {
        if (menuItems.size()>1)
            ((TextView)menuItems.get(1).findViewById(R.id.cnt)).setText(String.valueOf(count));
    }
    public void setMenuMyCount (int count) {
        if (menuItems.size()>2)
            ((TextView)menuItems.get(2).findViewById(R.id.cnt)).setText(String.valueOf(count));
    }

    public void setMenuAlarmCount(int count) {
        if (menuItems.size()>3)
            ((TextView)menuItems.get(3).findViewById(R.id.cnt)).setText(String.valueOf(count));
    }
}
