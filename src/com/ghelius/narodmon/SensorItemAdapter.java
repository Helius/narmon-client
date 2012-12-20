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
            holder.name.setText(items.get(position).getName());
            holder.location.setText(items.get(position).getLocation());
            holder.value.setText(items.get(position).getValue());
            holder.icon.setImageResource(R.drawable.ic_launcher);
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
