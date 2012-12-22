package com.ghelius.narodmon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import static android.util.Log.d;
import static android.util.Log.e;

public class SensorItemAdapter extends ArrayAdapter<Sensor> {
    private final Context context;
    private final List<Sensor> items;

    public SensorItemAdapter(Context context, List<Sensor> values) {
        super(context, R.layout.list, values);
        this.context = context;
        this.items = values;
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        d("eltex","getView for " + String.valueOf(position));

        if(v == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.list, null);
            ViewHolder holder = new ViewHolder();
            holder.name = (TextView)v.findViewById(R.id.text1);
            holder.location = (TextView)v.findViewById(R.id.text2);
            holder.value = (TextView)v.findViewById(R.id.text3);
            holder.icon = (ImageView)v.findViewById(R.id.img);
            v.setTag(holder);
        }

        ViewHolder holder = (ViewHolder)v.getTag();
        if (position < items.size()) {
            Sensor sensor = items.get(position);
            holder.name.setText(sensor.getName());
            holder.location.setText(sensor.getLocation());
            holder.value.setText(sensor.getValue());
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
           e("PlaylistAdapter", "index out of bound items[]");
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
