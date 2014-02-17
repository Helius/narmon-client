package com.ghelius.narodmon;


import android.graphics.Color;
import android.os.Bundle;
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


    interface MenuClickListener {
        void menuAllClicked();
        void menuWatchedClicked();
        void menuMyClicked();
        void menuAlarmClicked();
    }

    public void setOnMenuClickListener (MenuClickListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.sliding_menu_fragment, null);

        final int menuBackgroundColor = Color.argb(0xff, 0x01, 0x34, 0x6E);
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
					clearMenuSelection();
					Log.d(TAG, "click!" + v.getTag());
					switch ((Integer)v.getTag()) {
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
					v.setBackgroundColor(menuBackgroundColor);
				}
			});
		}
        return v;
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
