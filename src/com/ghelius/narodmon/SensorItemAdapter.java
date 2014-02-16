package com.ghelius.narodmon;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SensorItemAdapter extends ArrayAdapter<Sensor> {
    private final List<Sensor> originItems;
    private ArrayList<Sensor> localItems = null;
    private SensorFilter filter = null;
    private static final String TAG = "narodmon-adapter";
    private UiFlags uiFlags;
	private boolean hideValue = false;
	private SensorGroups groups = SensorGroups.All;
    ArrayList<Integer> favorites;

    public SensorItemAdapter(Context context, ArrayList<Sensor> values) {
        super(context, R.layout.sensor_list_item);
        this.originItems = values;
        this.localItems = new ArrayList<Sensor>();
	    uiFlags = new UiFlags();
        updateAlarms();
        updateFavorites();
    }

	enum SensorGroups {All, Watched, My, Alarmed}

    public void setUiFlags(UiFlags uiFlags) {
        this.uiFlags = uiFlags;
    }

	public void hideValue (boolean hide) {
		hideValue = hide;
	}

    public void updateFilter() {
        getFilter().filter("");
        // calc sensor in alarm state
        for (Sensor s: originItems) {
            if (s.alarmed) {
                Float v = s.valueToFloat();
                AlarmSensorTask alarm = DatabaseManager.getInstance().getAlarmById(s.id);
                Log.d(TAG,"in UpdateFilter: found alarm on sensor");
                if (alarm != null && v != null && alarm.isAlarmNow(v))
                    s.alarm_fired = true;
                else
                    s.alarm_fired = false;
                Log.d(TAG, "alarm_fired: " + s.alarm_fired);
            }
        }
    }

    public void updateFavorites () {
		favorites = DatabaseManager.getInstance().getFavorites();
    }

    public void updateAlarms () {
        Log.d(TAG,"> start update alarms");
        int cnt = 0;
        ArrayList<AlarmSensorTask> alarms = DatabaseManager.getInstance().getAlarmTask();
        for (Sensor s : originItems) {
            s.alarmed = false;
            for (AlarmSensorTask a : alarms) {
                if ((s.id == a.id) && a.job != AlarmSensorTask.NOTHING) {
                    s.alarmed = true;
                    cnt++;
                }
            }
        }
        Log.d(TAG,"< stop update alarms: " + cnt + " items");
        notifyDataSetChanged();
    }

	public void setGroups (SensorGroups g) {
		this.groups = g;
		updateFilter();
	}

    static class SensorNameComparator implements Comparator<Sensor> {
        @Override
        public int compare(Sensor o1, Sensor o2) {
            return o1.name.compareToIgnoreCase(o2.name);
        }
    }

	static class SensorDistanceComparator implements Comparator<Sensor> {
		@Override
		public int compare(Sensor o1, Sensor o2) {
			return Float.valueOf(o1.distance).compareTo(o2.distance);
		}
	}

	static class SensorTimeComparator implements Comparator<Sensor> {
		@Override
		public int compare(Sensor o1, Sensor o2) {
			return Long.valueOf(o2.time).compareTo(o1.time);
		}
	}

	static class SensorTypeComparator implements Comparator<Sensor> {
		@Override
		public int compare(Sensor o1, Sensor o2) {
			return Integer.valueOf(o1.type).compareTo(o2.type);
		}
	}

	private class SensorFilter extends Filter {
		// NOTE: this function is *always* called from a background thread, and not the UI thread.
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			Log.d(TAG, "performFiltering");
			FilterResults filteredResult = new FilterResults();
			ArrayList<Sensor> tempFilteredItems = new ArrayList<Sensor>();
//			if (groups ==  SensorGroups.Watched) {
//			}
//            if (groups == SensorGroups.Alarmed) {
//                updateAlarms();
//            }

			for (Sensor originItem : originItems) {
				boolean show_my = !(!originItem.my && (groups == SensorGroups.My)); // if wants 'my' and it's not my - return false, else true.
				boolean show_watched = !(groups == SensorGroups.Watched) || favorites.contains(originItem.id); // if wants 'favorites' and it's not - return false, else true.
                boolean show_alarmed = !(groups == SensorGroups.Alarmed) || originItem.alarmed; // if wants alarm only, and it's not - return false, else true.

				boolean type_match = true;
				for (int i = 0; i < uiFlags.hidenTypes.size(); i++) {
					if (uiFlags.hidenTypes.get(i) == originItem.type) {
						type_match = false;
					}
				}

				if (show_alarmed && show_my && show_watched && type_match && (originItem.distance < uiFlags.radiusKm)) {
					tempFilteredItems.add(originItem);
				}
			}
			if (uiFlags.sortType == UiFlags.SortType.distance) {
				Collections.sort(tempFilteredItems, new SensorDistanceComparator());
			} else if (uiFlags.sortType == UiFlags.SortType.name) {
				Collections.sort(tempFilteredItems, new SensorNameComparator());
			} else if (uiFlags.sortType == UiFlags.SortType.type) {
				Collections.sort(tempFilteredItems, new SensorTypeComparator());
			} else if (uiFlags.sortType == UiFlags.SortType.time) {
				Collections.sort(tempFilteredItems, new SensorTimeComparator());
			}

			filteredResult.values = tempFilteredItems;
			filteredResult.count = tempFilteredItems.size();
			Log.d(TAG, filteredResult.count + " items");
			return filteredResult;
		}


		@Override
		protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
			ArrayList<Sensor> res = (ArrayList<Sensor>) (filterResults.values);
			if (res == null) {
				return;
			}
			localItems.clear();
			for (Sensor re : res) {
				localItems.add(re);
			}
			notifyDataSetChanged();
		}
	}

	@Override
	public Filter getFilter() {
		if (filter == null)
			filter = new SensorFilter();
		return filter;
	}

	@Override
	public int getCount() {
		return localItems.size();
	}

    public int getAllCount() {
        return originItems.size();
    }

	public int getMyCount() {
		int i = 0;
		for (Sensor s: originItems) {
			if (s.my)
				i++;
		}
		return i;
	}

	@Override
	public Sensor getItem(int position) {
		return localItems.get(position);
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {

		if (v == null) {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			v = inflater.inflate(R.layout.sensor_list_item, null);
			ViewHolder holder = new ViewHolder();
			holder.name = (TextView) v.findViewById(R.id.text1);
			holder.location = (TextView) v.findViewById(R.id.text2);
			holder.value = (TextView) v.findViewById(R.id.text3);
			holder.icon = (ImageView) v.findViewById(R.id.img);
			v.setTag(holder);
		}

		ViewHolder holder = (ViewHolder) v.getTag();
		if (position < localItems.size()) {
			Sensor sensor = localItems.get(position);
			holder.name.setText(sensor.name);
			holder.location.setText(sensor.location);
			if (hideValue)
				holder.value.setText("");
			else
				holder.value.setText(sensor.value);

			if (sensor.my)
				holder.name.setTextColor(Color.argb(0xFF, 0x33, 0xb5, 0xe5));
            else
                holder.name.setTextColor(Color.WHITE);

            if (sensor.alarm_fired)
                holder.value.setTextColor(Color.argb(0xFF, 0xFF, 0x00, 0x00));
			else
                holder.value.setTextColor(Color.WHITE);

			holder.icon.setImageDrawable(SensorTypeProvider.getInstance(getContext()).getIcon(sensor.type));

		} else {
			Log.e(TAG, "index out of bound results[]");
		}
		return v;
	}

	static class ViewHolder {
		TextView name;
		TextView location;
		TextView value;
		ImageView icon;
	}
}

