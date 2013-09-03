package com.ghelius.narodmon;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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

import static android.util.Log.e;

public class SensorItemAdapter extends ArrayAdapter<Sensor> {
    private final Context context;
    private final List<Sensor> originItems;
    private ArrayList<Sensor> localItems = null;
    private SensorFilter filter = null;
    private static final String TAG = "narodmon-adapter";
    ConfigHolder config;
    private UiFlags uiFlags;

    public SensorItemAdapter(Context context, ArrayList<Sensor> values) {
        super(context, R.layout.sensor_list_item);
        this.context = context;
        this.originItems = values;
        this.localItems = new ArrayList<Sensor>();
        config = ConfigHolder.getInstance(context);
	    uiFlags.sortType = UiFlags.SortType.distance;
	    uiFlags.radiusKm = 10000;
	    uiFlags.showingMyOnly = false;
	    uiFlags.hidenTypes = new ArrayList<Integer>();
	    uiFlags.uiMode = UiFlags.UiMode.list;
    }

    public void setUiFlags(UiFlags uiFlags) {
        this.uiFlags = uiFlags;
    }

    public void update() {
        getFilter().filter("");
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

			for (Sensor originItem : originItems) {
				boolean my_match = uiFlags.showingMyOnly && originItem.my || !uiFlags.showingMyOnly;

				boolean type_match = true;
				for (int i = 0; i < uiFlags.hidenTypes.size(); i++) {
					if (uiFlags.hidenTypes.get(i) == originItem.type) {
						type_match = false;
					}
				}

				if (my_match && type_match && (originItem.distance < uiFlags.radiusKm)) {
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

	@Override
	public Sensor getItem(int position) {
		return localItems.get(position);
	}

	@Override
	public View getView(int position, View v, ViewGroup parent) {

		if (v == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
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
			holder.value.setText(sensor.value);

			if (config.isSensorWatched(sensor.id)) {
				holder.value.setTypeface(null, Typeface.BOLD);
			} else {
				holder.value.setTypeface(null, Typeface.NORMAL);
			}

			if (sensor.my) {
				holder.name.setTextColor(Color.argb(0xFF, 0x33, 0xb5, 0xe5));
			} else {
				holder.name.setTextColor(Color.WHITE);
			}

			if (ConfigHolder.getInstance(context).isSensorWatched(sensor.id)) {
				holder.value.setTextColor(Color.argb(0xFF, 0x00, 0xFF, 0x00));
			} else {
				holder.value.setTextColor(Color.WHITE);
			}
			holder.icon.setImageDrawable(SensorTypeProvider.getInstance(context).getIcon(sensor.type));

		} else {
			e("PlaylistAdapter", "index out of bound results[]");
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

