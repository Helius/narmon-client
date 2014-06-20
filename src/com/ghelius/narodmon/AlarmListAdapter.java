package com.ghelius.narodmon;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class AlarmListAdapter extends ArrayAdapter<AlarmSensorTask> {
    private static final String TAG = "narodmon-alarmAdapter";
    private ArrayList<AlarmSensorTask> tasks;
    private DatabaseHelper dbh;
    public AlarmListAdapter(Context context, int resource) {
        super(context, resource);

        tasks = DatabaseManager.getInstance().getAlarmTasks();
    }


    public void update () {
        tasks.clear();
        tasks.addAll(DatabaseManager.getInstance().getAlarmTasks());
    }

    @Override
    public int getCount () {
        return tasks.size();
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            v = inflater.inflate(R.layout.alarm_item, null);
            ViewHolder holder = new ViewHolder();
            holder.name = (TextView) v.findViewById(R.id.alarm_name);
            v.setTag(holder);
        }
        ViewHolder holder = (ViewHolder) v.getTag();
        holder.name.setText (tasks.get(position).name +", "+ tasks.get(position).lastValue);
        Log.d(TAG, "name: " + tasks.get(position).name + ", " + tasks.get(position).lastValue );
        //TODO: init view value
        return v;
    }

    static class ViewHolder {
        TextView name;
        ImageView icon;
    }
}
