package com.ghelius.narodmon;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

public class FilterFragment extends Fragment {
    final private static String TAG="narodmon-filterFragment";
    private OnFilterChangeListener mListener;
    private UiFlags uiFlags;

    public interface OnFilterChangeListener {
        void filterChange();
        UiFlags returnUiFlags();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFilterChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFilterChangeListener");
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.filter_dialog, null);
//        uiFlags = mListener.returnUiFlags();
        final CheckedListItemAdapter typeAdapter = new CheckedListItemAdapter(getActivity().getApplicationContext(), SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getTypesList());
        typeAdapter.setItemChangeListener( new CheckedListItemAdapter.ItemChangeInterface() {
            @Override
            public boolean isItemChecked(int position) {
                for (int i = 0; i < uiFlags.hidenTypes.size(); i++) {
                    if (typeAdapter.getItem(position).code == uiFlags.hidenTypes.get(i)) {
                        return false;
                    }
                }
                return true;
            }
        });

        typeAdapter.notifyDataSetChanged();
        ListView typeListView = (ListView) view.findViewById(R.id.typeListView);
        typeListView.setAdapter(typeAdapter);
        typeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (uiFlags.hidenTypes.contains(typeAdapter.getItem(position).code)) {
                    uiFlags.hidenTypes.remove((Integer) typeAdapter.getItem(position).code);
                } else {
                    uiFlags.hidenTypes.add((Integer) typeAdapter.getItem(position).code);
                }
                typeAdapter.notifyDataSetChanged();
                mListener.filterChange();
            }
        });

        ((Button) view.findViewById(R.id.filter_select_all)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uiFlags.hidenTypes.clear();
                typeAdapter.notifyDataSetChanged();
                mListener.filterChange();
            }
        });
        ((Button) view.findViewById(R.id.filter_select_none)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getTypesList().size(); i++) {
                    uiFlags.hidenTypes.add(SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getTypesList().get(i).code);
                }
                typeAdapter.notifyDataSetChanged();
                mListener.filterChange();
            }
        });

        RadioButton btSortDistance = (RadioButton) view.findViewById(R.id.radioButtonSortDistance);
        btSortDistance.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked)
                    return;
                Log.d(TAG, "check sort");
                uiFlags.sortType = UiFlags.SortType.distance;
                ((RadioButton) view.findViewById(R.id.radioButtonSortName)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioButtonSortType)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioButtonSortTime)).setChecked(false);
                mListener.filterChange();
            }
        });
        RadioButton btSortName = (RadioButton) view.findViewById(R.id.radioButtonSortName);
        btSortName.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked)
                    return;
                Log.d(TAG, "check name");
                uiFlags.sortType = UiFlags.SortType.name;
                ((RadioButton) view.findViewById(R.id.radioButtonSortDistance)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioButtonSortType)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioButtonSortTime)).setChecked(false);
                mListener.filterChange();
            }
        });
        RadioButton btSortType = (RadioButton) view.findViewById(R.id.radioButtonSortType);
        btSortType.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked)
                    return;
                Log.d(TAG, "check type");
                uiFlags.sortType = UiFlags.SortType.type;
                ((RadioButton) view.findViewById(R.id.radioButtonSortName)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioButtonSortDistance)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioButtonSortTime)).setChecked(false);
                mListener.filterChange();
            }
        });
        RadioButton btSortTime = (RadioButton) view.findViewById(R.id.radioButtonSortTime);
        btSortTime.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked)
                    return;
                Log.d(TAG, "check time");
                uiFlags.sortType = UiFlags.SortType.time;
                ((RadioButton) view.findViewById(R.id.radioButtonSortName)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioButtonSortType)).setChecked(false);
                ((RadioButton) view.findViewById(R.id.radioButtonSortDistance)).setChecked(false);
                mListener.filterChange();
            }
        });


        SeekBar radius = (SeekBar) view.findViewById(R.id.radius_seekerbar);
        final TextView radiusValue = (TextView) view.findViewById(R.id.radius_value);
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int distance = (int) Math.pow(2, progress);
                radiusValue.setText(String.valueOf(distance));
                if (distance != 0)
                    uiFlags.radiusKm = distance;
                else
                    uiFlags.radiusKm = 1;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mListener.filterChange();
            }
        });


        return view;
	}

    @Override
    public void onResume () {
        super.onResume();
        Log.d(TAG,"onResume");
        uiFlags = mListener.returnUiFlags();

        SeekBar radius = (SeekBar) getView().findViewById(R.id.radius_seekerbar);
        radius.setProgress((int) (Math.log(uiFlags.radiusKm) / Math.log(2)));
        radius.setMax(15);
        final TextView radiusValue = (TextView) getView().findViewById(R.id.radius_value);
        radiusValue.setText(String.valueOf(uiFlags.radiusKm));

        RadioGroup radioGroup1 = (RadioGroup) getView().findViewById(R.id.radiogroupe_sort);
        if (uiFlags.sortType == UiFlags.SortType.distance)
            radioGroup1.check(R.id.radioButtonSortDistance);
        else if (uiFlags.sortType == UiFlags.SortType.name)
            radioGroup1.check(R.id.radioButtonSortName);
        else if (uiFlags.sortType == UiFlags.SortType.time)
            radioGroup1.check(R.id.radioButtonSortTime);
        else if (uiFlags.sortType == UiFlags.SortType.type)
            radioGroup1.check(R.id.radioButtonSortType);

    }
}
