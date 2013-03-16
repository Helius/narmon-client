package com.ghelius.narodmon;


import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class FilterDialog extends android.support.v4.app.DialogFragment implements DialogInterface.OnClickListener {

    final static String LOG_TAG = "narodmon-dialog";
    private OnChangeListener listener;
    private static UiFlags uiFlags = null;
    private TextView radiusValue;

    public FilterDialog () {
    }

    public static void setUiFlags(UiFlags filterF) {
        uiFlags = filterF;
    }

    interface OnChangeListener {
        void onFilterChange ();
        void onDialogClose();
    }

    public void setOnChangeListener (OnChangeListener listener) {
        this.listener = listener;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(this.getString(R.string.text_filter_dialog_title));
        final View v = inflater.inflate(R.layout.filter_dialog, null);

        RadioGroup radioGroup = (RadioGroup) v.findViewById(R.id.radiogroupe_All_My);
        if (uiFlags.showingMyOnly)
            radioGroup.check(R.id.radioButtonMyOnly);
        else
            radioGroup.check(R.id.radioButtonAll);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                uiFlags.showingMyOnly = checkedId == R.id.radioButtonMyOnly;

                if (listener!=null)
                    listener.onFilterChange();
            }
        });

        CheckBox cb1 = (CheckBox) v.findViewById(R.id.checkBoxTemperature);
        cb1.setChecked(uiFlags.types[UiFlags.type_temperature]);
        cb1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                uiFlags.types[UiFlags.type_temperature] = isChecked;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });
        CheckBox cb2 = (CheckBox) v.findViewById(R.id.checkBoxPressure);
        cb2.setChecked(uiFlags.types[UiFlags.type_pressure]);
        cb2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                uiFlags.types[UiFlags.type_pressure] = isChecked;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });
        CheckBox cb3 = (CheckBox) v.findViewById(R.id.checkBoxHumidity);
        cb3.setChecked(uiFlags.types[UiFlags.type_humidity]);
        cb3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                uiFlags.types[UiFlags.type_humidity] = isChecked;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });
        CheckBox cb4 = (CheckBox) v.findViewById(R.id.checkBoxOtherType);
        cb4.setChecked(uiFlags.types[UiFlags.type_unknown]);
        cb4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                uiFlags.types[UiFlags.type_unknown] = isChecked;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });

        RadioGroup radioGroup1 = (RadioGroup) v.findViewById(R.id.radiogroupe_sort);
        if (uiFlags.sortingDistance)
            radioGroup1.check(R.id.radioButtonSortDistance);
        else
            radioGroup1.check(R.id.radioButtonSortName);
        radioGroup1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                uiFlags.sortingDistance = checkedId == R.id.radioButtonSortDistance;
                if (listener!=null)
                    listener.onFilterChange();
            }
        });

        SeekBar radius = (SeekBar) v.findViewById(R.id.radius_seekerbar);
        radius.setMax(15);
        radius.setProgress((int) (Math.log(uiFlags.radiusKm)/Math.log(2)));
        radiusValue = (TextView) v.findViewById(R.id.radius_value);
        radiusValue.setText(String.valueOf(uiFlags.radiusKm));
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int distance = (int) Math.pow(2,progress);
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
                if (listener!=null) {
                    listener.onFilterChange();
                }
            }
        });

        return v;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Log.d(LOG_TAG, "Dialog 1: onDismiss");
        if (listener != null)
            listener.onDialogClose();
    }
    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        Log.d(LOG_TAG, "Dialog 1: onCancel");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
    }
}
