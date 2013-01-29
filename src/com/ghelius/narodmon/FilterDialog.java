package com.ghelius.narodmon;


import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

public class FilterDialog extends DialogFragment implements DialogInterface.OnClickListener {

    final static String LOG_TAG = "narodmon-dialog";
    private OnChangeListener listener;
    private final FilterFlags filterFlags;

    FilterDialog (FilterFlags filterFlags) {
        this.filterFlags = filterFlags;
    }

    interface OnChangeListener {
        void onFilterChange ();
    }

    public void setOnChangeListener (OnChangeListener listener) {
        this.listener = listener;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("Filtering and sorting");
        View v = inflater.inflate(R.layout.filter_dialog, null);

        RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.radiogroupe_All_My);
        if (filterFlags.showingMyOnly)
            radioGroup.check(R.id.radioButtonMyOnly);
        else
            radioGroup.check(R.id.radioButtonAll);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                filterFlags.showingMyOnly = checkedId == R.id.radioButtonMyOnly;

                if (listener!=null)
                    listener.onFilterChange();
            }
        });

        CheckBox cb1 = (CheckBox) v.findViewById(R.id.checkBoxTemperature);
        cb1.setChecked(filterFlags.types[FilterFlags.type_temperature]);
        cb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filterFlags.types[FilterFlags.type_temperature] = isChecked;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });
        CheckBox cb2 = (CheckBox) v.findViewById(R.id.checkBoxPressure);
        cb2.setChecked(filterFlags.types[FilterFlags.type_pressure]);
        cb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filterFlags.types[FilterFlags.type_pressure] = isChecked;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });
        CheckBox cb3 = (CheckBox) v.findViewById(R.id.checkBoxHumidity);
        cb3.setChecked(filterFlags.types[FilterFlags.type_humidity]);
        cb3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filterFlags.types[FilterFlags.type_humidity] = isChecked;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });
        CheckBox cb4 = (CheckBox) v.findViewById(R.id.checkBoxOtherType);
        cb4.setChecked(filterFlags.types[FilterFlags.type_unknown]);
        cb4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filterFlags.types[FilterFlags.type_unknown] = isChecked;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });

        RadioGroup radioGroup1 = (RadioGroup) v.findViewById(R.id.radiogroupe_sort);
        if (filterFlags.sortingDistance)
            radioGroup1.check(R.id.radioButtonSortDistance);
        else
            radioGroup1.check(R.id.radioButtonSortName);
        radioGroup1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                filterFlags.sortingDistance = checkedId == R.id.radioButtonSortDistance;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });

        return v;
    }

    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(LOG_TAG, "Dialog 1: onDismiss");
    }

    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        Log.d(LOG_TAG, "Dialog 1: onCancel");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    }
}
