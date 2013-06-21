package com.ghelius.narodmon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MultiGraphListAdapter extends ArrayAdapter<MultiGraph> {

	static final private String TAG = "narodmon-mglAdapter";
	private final Context context;


	static class ViewHolder {
		public TextView name;
		public TextView sensorNmb;
	}

	public MultiGraphListAdapter(Context context, ArrayList<MultiGraph> graphs) {
		super(context, R.layout.multi_graph_list_adapter,  graphs);
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
			rowView = inflater.inflate(R.layout.multi_graph_list_adapter, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.name = (TextView) rowView.findViewById(R.id.mg_name);
			viewHolder.sensorNmb = (TextView) rowView.findViewById(R.id.mg_nmb);
			rowView.setTag(viewHolder);
		}
		ViewHolder holder = (ViewHolder) rowView.getTag();
		holder.name.setText (getItem(position).name);
		holder.sensorNmb.setText(String.valueOf(getItem(position).ids.size()));
		return rowView;
	}
}
