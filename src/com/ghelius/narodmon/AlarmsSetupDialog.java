package com.ghelius.narodmon;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class AlarmsSetupDialog extends android.support.v4.app.DialogFragment {

    private Float currentValue;
    private AlarmChangeListener listener;
    private AlarmSensorTask sensorTask=null;

    private final static String TAG = "narodmon-info";

    public void setSensorTask(AlarmSensorTask sensorTask) {
        this.sensorTask = sensorTask;
    }

    public void setCurrentValue(String value) {
	    try {
        this.currentValue = Float.valueOf(value);
	    } catch (Exception e) {
		    Log.e(TAG,"current value error: " + e.getMessage());
		    this.currentValue = 0f;
	    }
    }


	interface AlarmChangeListener {
        void onAlarmChange (AlarmSensorTask task);
    }

    public void setOnAlarmChangeListener (AlarmChangeListener l) {
        this.listener = l;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(this.getString(R.string.text_alarm_setup_titel));
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
//                    listener.onAlarmChange(((Spinner)getView().findViewById(R.id.AlarmSpinner)).getSelectedItemPosition(), hi, lo);
	            if (listener != null)
		            listener.onAlarmChange (new AlarmSensorTask(
				            sensorTask.id,
				            ((Spinner)getView().findViewById(R.id.AlarmSpinner)).getSelectedItemPosition(),
				            hi,
				            lo,
				            sensorTask.lastValue));
	            dismiss();
            }
        });

	    EditText textHi = (EditText)getView().findViewById(R.id.hiLimit);
	    EditText textLo = (EditText)getView().findViewById(R.id.lowLimit);
	    Spinner spinner = (Spinner)getView().findViewById(R.id.AlarmSpinner);
        if (sensorTask != null) {
            textHi.setText(String.valueOf(sensorTask.hi));
            textLo.setText(String.valueOf(sensorTask.lo));
            spinner.setSelection(sensorTask.job);
            if (sensorTask.job == 0) {
                textHi.setText(String.valueOf(currentValue + 10));
                textLo.setText(String.valueOf(currentValue - 10));
            }
        }


        ((Spinner)getView().findViewById(R.id.AlarmSpinner)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText(AlarmsSetupDialog.this.getString(R.string.alarm_info_text_1));
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.INVISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.INVISIBLE);
                        break;
                    case 1:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText(AlarmsSetupDialog.this.getString(R.string.alarm_info_text_2));
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.INVISIBLE);
                        break;
                    case 2:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText(AlarmsSetupDialog.this.getString(R.string.alarm_info_text_3));
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.INVISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.VISIBLE);
                        break;
                    case 3:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText(AlarmsSetupDialog.this.getString(R.string.alarm_info_text_4));
                        getView().findViewById(R.id.row_highlimit).setVisibility(View.VISIBLE);
                        getView().findViewById(R.id.row_lowlimit).setVisibility(View.VISIBLE);
                        break;
                    case 4:
                        ((TextView)getView().findViewById(R.id.alarmHelp)).setText(AlarmsSetupDialog.this.getString(R.string.alarm_info_text_5));
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
