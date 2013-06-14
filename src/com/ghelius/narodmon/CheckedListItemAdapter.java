package com.ghelius.narodmon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.util.ArrayList;

public class CheckedListItemAdapter extends ArrayAdapter<SensorType> {
	private final Context context;
	private ItemChangeInterface listener = null;

	interface ItemChangeInterface {
		boolean isItemChecked (int position);
		boolean itemChecked (int position, boolean checked);
	}

	public void setItemChangeInterface (ItemChangeInterface listener) {
		this.listener = listener;
	}

	static class ViewHolder {
		public TextView text;
		public ImageView image;
		public CheckBox checkBox;
	}

	public CheckedListItemAdapter(Context context, ArrayList<SensorType> types) {
		super(context, R.layout.checked_list_item_adapter,  types);
		this.context = context;
	}

	public void itemChecked (int index, boolean checked) {
		if (listener != null)
			listener.itemChecked(index,checked);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = LayoutInflater.from(context);
			rowView = inflater.inflate(R.layout.checked_list_item_adapter, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.text = (TextView) rowView.findViewById(R.id.checkeditem_text);
			viewHolder.image = (ImageView) rowView.findViewById(R.id.checkeditem_image);
			viewHolder.checkBox = (CheckBox) rowView.findViewById(R.id.checkeditem_checkbox);
			viewHolder.checkBox.setTag(position);
			viewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					itemChecked((Integer) buttonView.getTag(), isChecked);
				}
			});
			rowView.setTag(viewHolder);
		}
		ViewHolder holder = (ViewHolder) rowView.getTag();
		if (listener != null) {
			holder.checkBox.setChecked(listener.isItemChecked(position));
		}
		holder.text.setText(getItem(position).name);
//		String s = names[position];
//		holder.text.setText(s);
//		if (s.startsWith("Windows7") || s.startsWith("iPhone")
//				|| s.startsWith("Solaris")) {
//			holder.image.setImageResource(R.drawable.no);
//		} else {
//			holder.image.setImageResource(R.drawable.ok);
//		}
		return rowView;
	}
}
