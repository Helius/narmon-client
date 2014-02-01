package com.ghelius.narodmon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class CheckedListItemAdapter extends ArrayAdapter<SensorType> {
	static final private String TAG = "narodmon-checkedAdapter";
	private ItemChangeInterface listener = null;

	interface ItemChangeInterface {
		boolean isItemChecked (int position);
	}

	public void setItemChangeListener(ItemChangeInterface listener) {
		this.listener = listener;
	}

	static class ViewHolder {
		public TextView text;
		public ImageView image;
		public CheckBox checkBox;
	}

	public CheckedListItemAdapter(Context context, ArrayList<SensorType> types) {
		super(context, R.layout.checked_list_item_adapter,  types);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = LayoutInflater.from(getContext());
			rowView = inflater.inflate(R.layout.checked_list_item_adapter, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.text = (TextView) rowView.findViewById(R.id.checkeditem_text);
			viewHolder.image = (ImageView) rowView.findViewById(R.id.checkeditem_image);
			viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.checkeditem_checkbox);
			rowView.setTag(viewHolder);
		}
		ViewHolder holder = (ViewHolder) rowView.getTag();
		if (listener != null) {
			holder.checkBox.setChecked(listener.isItemChecked(position));
		}
		holder.text.setText(getItem(position).name);
		holder.image.setImageDrawable(SensorTypeProvider.getInstance(getContext()).getIcon(getItem(position).code));
		return rowView;
	}
}
