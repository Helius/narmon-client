package com.ghelius.narodmon;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class WatchedItemAdapter extends ArrayAdapter<Sensor> {
    private final Context context;
    private final String TAG = "narodmon-watched";

    ConfigHolder config;

    public WatchedItemAdapter(Context context, ArrayList<Sensor> values) {
        super(context, R.layout.sensor_list_item);
        this.context = context;
        config = ConfigHolder.getInstance(context);
    }


    @Override
    public View getView(int position, View v, ViewGroup parent) {

        if(v == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.watched_list_item, null);
            ViewHolder holder = new ViewHolder();
            holder.name = (TextView)v.findViewById(R.id.main_text);
            holder.value = (TextView)v.findViewById(R.id.value);
            holder.bottomText = (TextView)v.findViewById(R.id.bottom_text);
            holder.icon = (ImageView)v.findViewById(R.id.icon_view);
            v.setTag(holder);
        }

        ViewHolder holder = (ViewHolder)v.getTag();
        Sensor sensor = getItem(position);
        holder.name.setText(sensor.name);
        holder.value.setText(sensor.value);

        if (!sensor.online) {
            holder.name.setEnabled(false);
            holder.bottomText.setText("offline since ");
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
        return v;
    }

    static class ViewHolder {
        TextView  name;
        TextView  bottomText;
        TextView  value;
        ImageView icon;
    }
}
