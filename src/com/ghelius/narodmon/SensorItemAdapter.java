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
import java.util.List;

import static android.util.Log.e;

public class SensorItemAdapter extends ArrayAdapter<Sensor> {
    private final Context context;
    private final List<Sensor> originItems;
    private List<Sensor> sensorItems = null;
    private SensorFilter filter = null;
    private final String TAG = "narodmon-adapter";
    ConfigHolder config;

    public SensorItemAdapter(Context context, ArrayList<Sensor> values) {
        super(context, R.layout.general_list_item);
        this.context = context;
        this.originItems = values;
        this.sensorItems = new ArrayList<Sensor>();
        config = ConfigHolder.getInstance(context);
   }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new SensorFilter();
        return filter;
    }

    private class SensorFilter extends Filter {
        // NOTE: this function is *always* called from a background thread, and not the UI thread.
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            Log.d(TAG,"performFiltering with: " + constraint);
            FilterResults filteredResult = new FilterResults();
            ArrayList<Sensor> filteredItems= new ArrayList<Sensor>();
            if (String.valueOf(constraint).equals("watch")) {
                Log.d(TAG,"make watch list");
                for (int i = 0; i < originItems.size(); i++) {
                    if (ConfigHolder.getInstance(context).isSensorWatched(originItems.get(i).getId())) {
                        filteredItems.add(originItems.get(i));
                    }
                }
                filteredResult.values = filteredItems;
                filteredResult.count = filteredItems.size();
            } else if (String.valueOf(constraint).equals("temperature")) {
                for (Sensor originItem : originItems) {
                    if (originItem.type == Sensor.TYPE_TEMPERATURE) {
                        filteredItems.add(originItem);
                    }
                }
                filteredResult.values = filteredItems;
                filteredResult.count = filteredItems.size();
            } else if (String.valueOf(constraint).equals("pressure")) {
                for (Sensor originItem : originItems) {
                    if (originItem.type == Sensor.TYPE_PRESSURE) {
                        filteredItems.add(originItem);
                    }
                }
                filteredResult.values = filteredItems;
                filteredResult.count = filteredItems.size();
            } else if (String.valueOf(constraint).equals("humidity")) {
                for (Sensor originItem : originItems) {
                    if (originItem.type == Sensor.TYPE_HUMIDITY) {
                        filteredItems.add(originItem);
                    }
                }
                filteredResult.values = filteredItems;
                filteredResult.count = filteredItems.size();
            } else if (String.valueOf(constraint).equals("unknown")) {
                for (Sensor originItem : originItems) {
                    if (originItem.type == Sensor.TYPE_UNKNOWN) {
                        filteredItems.add(originItem);
                    }
                }
                filteredResult.values = filteredItems;
                filteredResult.count = filteredItems.size();
            } else {
                Log.d(TAG,"return originIems.size = " + originItems.size());
                filteredResult.values = originItems;
                filteredResult.count = originItems.size();
            }
            return filteredResult;
        }


        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            Log.d(TAG, "publishResults: filterResults.size = " + ((List<Sensor>) filterResults.values).size());
            sensorItems = (ArrayList<Sensor>)filterResults.values;
            notifyDataSetChanged();
            clear();

            for (int i = 0; i < sensorItems.size(); i++)
                add(sensorItems.get(i));
            notifyDataSetInvalidated();
        }


    }

    @Override
    public int getCount() {
        return sensorItems.size();
    }

    @Override
    public Sensor getItem(int position) {
        return sensorItems.get(position);
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {

        if(v == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.general_list_item, null);
            ViewHolder holder = new ViewHolder();
            holder.name = (TextView)v.findViewById(R.id.text1);
            holder.location = (TextView)v.findViewById(R.id.text2);
            holder.value = (TextView)v.findViewById(R.id.text3);
            holder.icon = (ImageView)v.findViewById(R.id.img);
            v.setTag(holder);
        }

        ViewHolder holder = (ViewHolder)v.getTag();
        if (position < sensorItems.size()) {
            Sensor sensor = sensorItems.get(position);
            holder.name.setText(sensor.getName());
            holder.location.setText(sensor.getLocation());
            holder.value.setText(sensor.getValue());
            if (config.isSensorWatched(sensor.getId())) {
                holder.value.setTypeface(null, Typeface.BOLD);
            } else {
                holder.value.setTypeface(null, Typeface.NORMAL);
                //holder.value.setVisibility(View.INVISIBLE);
            }
            switch (sensor.getType()) {
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
