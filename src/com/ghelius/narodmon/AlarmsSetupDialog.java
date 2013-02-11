package com.ghelius.narodmon;

import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class AlarmsSetupDialog extends DialogFragment {

    private Float currentValue;
    private AlarmChangeListener listener;
    private Configuration.SensorTask sensorTask;

    private final static String TAG = "narodmon-info";

    public void setSensorTask(Configuration.SensorTask sensorTask) {
        if (sensorTask != null) {
            this.sensorTask = sensorTask;
        }
    }

    public void setCurrentValue(String value) {
        this.currentValue = Float.valueOf(value);
    }

    interface AlarmChangeListener {
        void onAlarmChange (int job, Float hi, Float lo);
    }

    public void setOnAlarmChangeListener (AlarmChangeListener l) {
        this.listener = l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle("Alarm setup");
        View v = inflater.inflate(R.layout.alarms_setup_dialog, null);
        return v;
    }

    @Override
    public void onActivityCreated (Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        ((TextView) getView().findViewById(R.id.alarmDialogCurrentValue)).setText(String.valueOf(currentValue));
        getView().findViewById(R.id.AcceptButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    Float hi = (float) 0;
                    try {
                        hi = Float.valueOf(((EditText) getView().findViewById(R.id.hiLimit)).getText().toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Can't parce float " + e.getMessage());
                    }
                    Float lo = (float) 0;
                    try {
                        lo = Float.valueOf(((EditText) getView().findViewById(R.id.lowLimit)).getText().toString());
                    } catch (Exception e) {
                        Log.e(TAG, "Can't parce float " + e.getMessage());
                    }
                    Log.d(TAG, "onClick: call listener for save alarm");
                    listener.onAlarmChange(((Spinner)getView().findViewById(R.id.AlarmSpinner)).getSelectedItemPosition(), hi, lo);
                } else
                    Log.e(TAG,"listener is null");
                dismiss();
            }
        });

        if (sensorTask != null) {
            ((EditText) getView().findViewById(R.id.hiLimit)).setText(String.valueOf(sensorTask.hi));
            ((EditText) getView().findViewById(R.id.lowLimit)).setText(String.valueOf(sensorTask.lo));
            ((Spinner)getView().findViewById(R.id.AlarmSpinner)).setSelection(sensorTask.job);
        } else {
            Log.e(TAG,"Sensor task is null");
        }

        ((Spinner)getView().findViewById(R.id.AlarmSpinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText("For enable alarms, select alarm type");
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.INVISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.INVISIBLE);
                        break;
                    case 1:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText("Alarm occurs when the value is above the limit");
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.INVISIBLE);
                        break;
                    case 2:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText("Alarm occurs when the value is below the limit");
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.INVISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText("Alarm occurs when the value out of limits");
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.VISIBLE);
                        break;
                    case 4:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText("Alarm occurs when the value within limits");
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });

    }
}
