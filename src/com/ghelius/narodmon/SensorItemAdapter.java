package com.ghelius.narodmon;

import android.content.Context;
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
    private final String TAG = "narodmon-adapter";
    ConfigHolder config;
    private FilterFlags filterFlags;

    public SensorItemAdapter(Context context, ArrayList<Sensor> values) {
        super(context, R.layout.sensor_list_item);
        this.context = context;
        this.originItems = values;
        this.localItems = new ArrayList<Sensor>();
        config = ConfigHolder.getInstance(context);
    }

    public void setFilterFlags (FilterFlags filterFlags) {
        this.filterFlags = filterFlags;
    }

    public void update() {
        getFilter().filter("");
    }

    class SensorNameComparator implements Comparator<Sensor> {
        @Override
        public int compare(Sensor o1, Sensor o2) {
            return o1.name.compareToIgnoreCase(o2.name);
        }
    }

    class SensorDistanceComparator implements Comparator<Sensor> {
        @Override
        public int compare(Sensor o1, Sensor o2) {
            return o1.distance.compareTo(o2.distance);
        }
    }

    private class SensorFilter extends Filter {
        private boolean prevSortByDistance = false;

        // NOTE: this function is *always* called from a background thread, and not the UI thread.
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            Log.d(TAG,"performFiltering");
            FilterResults filteredResult = new FilterResults();
            ArrayList<Sensor> tempFilteredItems= new ArrayList<Sensor>();

            for (Sensor originItem : originItems) {
                boolean my_match = filterFlags.showingMyOnly && originItem.my || !filterFlags.showingMyOnly;
                boolean type_match =
                        filterFlags.types[FilterFlags.type_temperature] && (originItem.type == Sensor.TYPE_TEMPERATURE) ||
                        filterFlags.types[FilterFlags.type_humidity] && (originItem.type == Sensor.TYPE_HUMIDITY) ||
                        filterFlags.types[FilterFlags.type_pressure] && (originItem.type == Sensor.TYPE_PRESSURE) ||
                        filterFlags.types[FilterFlags.type_unknown] && (originItem.type == Sensor.TYPE_UNKNOWN);

                if (my_match && type_match) {
                    tempFilteredItems.add(originItem);
                }
            }
            // avoid superfluous sorting
            if (prevSortByDistance != filterFlags.sortingDistance) {
                if (filterFlags.sortingDistance) {
                    Collections.sort(tempFilteredItems, new SensorNameComparator());
                } else {
                    Collections.sort(tempFilteredItems, new SensorDistanceComparator());
                }
                prevSortByDistance = filterFlags.sortingDistance;
            }
            filteredResult.values = tempFilteredItems;
            filteredResult.count = tempFilteredItems.size();
            return filteredResult;
        }


        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            ArrayList <Sensor> res = (ArrayList<Sensor>)(filterResults.values);
            if (res == null) {
                return;
            }
            localItems.clear();
            for (int i = 0; i < res.size(); i++) {
                localItems.add (res.get(i));
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

        if(v == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.sensor_list_item, null);
            ViewHolder holder = new ViewHolder();
            holder.name = (TextView)v.findViewById(R.id.text1);
            holder.location = (TextView)v.findViewById(R.id.text2);
            holder.value = (TextView)v.findViewById(R.id.text3);
            holder.icon = (ImageView)v.findViewById(R.id.img);
            v.setTag(holder);
        }

        ViewHolder holder = (ViewHolder)v.getTag();
        if (position < localItems.size()) {
            Sensor sensor = localItems.get(position);
            holder.name.setText(sensor.name);
            holder.location.setText(sensor.location);
            holder.value.setText(sensor.value);
            if (config.isSensorWatched(sensor.id)) {
                holder.value.setTypeface(null, Typeface.BOLD);
            } else {
                holder.value.setTypeface(null, Typeface.NORMAL);
                //holder.value.setVisibility(View.INVISIBLE);
            }
            switch (sensor.type) {
                case 1:
                    holder.icon.setImageResource(R.drawable.termo_icon);
                    break;
                case 2:
                    holder.icon.setImageResource(R.drawable.pressure_icon);
                    break;
                case 3:
                    holder.icon.setImageResource(R.drawable.humid_icon);
                    break;
                default:
                    holder.icon.setImageResource(R.drawable.unknown_icon);
            }
        } else {
           e("PlaylistAdapter", "index out of bound results[]");
        }
        return v;
    }
    static class ViewHolder {
        TextView  name;
        TextView  location;
        TextView  value;
        ImageView icon;
    }
}

