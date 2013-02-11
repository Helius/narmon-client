package com.ghelius.narodmon;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public class SensorInfo extends Activity {
    private final String TAG = "narodmon-info";
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensorinfo);
        final AlarmsSetupDialog dialog = new AlarmsSetupDialog();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final Sensor sensor = (Sensor)extras.getSerializable("Sensor");
            TextView name = (TextView) findViewById(R.id.text_name);
            TextView location = (TextView) findViewById(R.id.text_location);
            TextView distance = (TextView) findViewById(R.id.text_distance);
            TextView time = (TextView) findViewById(R.id.text_time);
            TextView value = (TextView) findViewById(R.id.text_value);
            TextView units = (TextView) findViewById(R.id.value_units);
            TextView type = (TextView) findViewById(R.id.text_type);
            TextView id = (TextView) findViewById(R.id.text_id);
            TextView ago = (TextView) findViewById(R.id.text_ago);
            ImageView icon = (ImageView) findViewById(R.id.info_sensor_icon);

            name.setText(sensor.name);
            location.setText(sensor.location);
            distance.setText(sensor.distance.toString());

            dialog.setOnAlarmChangeListener(new AlarmsSetupDialog.AlarmChangeListener() {
                @Override
                public void onAlarmChange(int job, Float hi, Float lo) {
                    Log.d(TAG, "new alarm setup: " + job + " " + hi + " " + lo);
                    ConfigHolder.getInstance(SensorInfo.this).setAlarm(sensor.id, job, hi, lo);

                    Toast.makeText(SensorInfo.this, "Set alarm", Toast.LENGTH_SHORT).show();
                }
            });

            String types;
            switch (sensor.type) {
                case 1:
                    types = getString(R.string.type_termometr);
                    icon.setImageResource(R.drawable.termo_icon);
                    break;
                case 2:
                    types = getString(R.string.type_pressure);
                    icon.setImageResource(R.drawable.pressure_icon);
                    break;
                case 3:
                    types = getString(R.string.type_humidity);
                    icon.setImageResource(R.drawable.humid_icon);
                    break;
                default:
                    types = getString(R.string.type_unknown);
                    icon.setImageResource(R.drawable.unknown_icon);
                    break;
            }
            type.setText(types);
            setTitle(types);
            String suffix = "";
            switch (sensor.type) {
                case 1:
                    suffix = "°C";
                    break;
                case 2:
                    suffix = "mmHg";
                    ((LinearLayout)findViewById(R.id.value_layout)).setOrientation(LinearLayout.VERTICAL);
                    break;
                case 3:
                    suffix = "%";
                    break;
                default:
                    break;
            }
            value.setText(sensor.value);
            units.setText(suffix);

            id.setText(String.valueOf(sensor.id));

            long dv = Long.valueOf(sensor.time)*1000;// its need to be in milisecond
            Date df = new java.util.Date(dv);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getDefault());
            String vv = sdf.format(df);
            time.setText(vv);

            ago.setText(getTimeSince(this,sensor.time));

            final ImageButton monitor = (ImageButton) findViewById(R.id.addMonitoring);
            final ImageButton alarm = (ImageButton) findViewById(R.id.alarmSetup);
            final ConfigHolder config = ConfigHolder.getInstance(getApplicationContext());

            if (config.isSensorWatched(sensor.id)) {
                monitor.setImageResource(R.drawable.yey_blue);
                alarm.setVisibility(View.VISIBLE);
                if (config.isSensorWatchJob(sensor.id)) {
                    alarm.setImageResource(R.drawable.alarm_blue);
                } else {
                    alarm.setImageResource(R.drawable.alarm_gray);
                }
            } else {
                monitor.setImageResource(R.drawable.yey_gray);
                alarm.setVisibility(View.INVISIBLE);
            }

            monitor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG,"monitoring onClick");
                    if (config.isSensorWatched(sensor.id)) {
                        config.setSensorWatched(sensor,false);
                        alarm.setVisibility(View.INVISIBLE);
                    } else {
                        config.setSensorWatched(sensor,true);
                        alarm.setVisibility(View.VISIBLE);
                    }
                    if (config.isSensorWatched(sensor.id)) {
                        monitor.setImageResource(R.drawable.yey_blue);
                    } else {
                        monitor.setImageResource(R.drawable.yey_gray);
                    }
                    if (config.isSensorWatchJob(sensor.id)) {
                        alarm.setImageResource(R.drawable.alarm_blue);
                    } else {
                        alarm.setImageResource(R.drawable.alarm_gray);
                    }
                }
            });
            alarm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.setCurrentValue(sensor.value);
                    Configuration.SensorTask task = ConfigHolder.getInstance(SensorInfo.this).getSensorTask(sensor.id);
                    Log.d(TAG,"find SensorTask");
                    if (task != null) {
                        Log.d(TAG, "SensorTask with job " + task.job);
                    } else {
                        Log.e(TAG, "Cant find sensorTask");
                    }
                    dialog.setSensorTask(task);
                    dialog.show(getFragmentManager(), "alarmDialog");
                    if (config.isSensorWatchJob(sensor.id)) {
                        alarm.setImageResource(R.drawable.alarm_blue);
                    } else {
                        alarm.setImageResource(R.drawable.alarm_gray);
                    }
                }
            });
        } else {
            finish();
        }

    }

    public static String getTimeSince (Context context, Long time) {
        long difftime = (System.currentTimeMillis() - time*1000)/1000;
        String agoText;
        if (difftime < 60) {
            agoText = String.valueOf(difftime) + context.getString(R.string.text_sec);
        } else if (difftime/60 < 60) {
            agoText = String.valueOf(difftime/60) + context.getString(R.string.text_min);
        } else if (difftime/3600 < 24) {
            agoText = String.valueOf(difftime/3600) + context.getString(R.string.text_hr);
        } else {
            agoText = String.valueOf(difftime/(3600*24)) + context.getString(R.string.text_days);
        }
        return agoText;
    }

    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries mCurrentSeries;
    private XYSeriesRenderer mCurrentRenderer;

    private void initChart() {
        mCurrentSeries = new XYSeries("");
        mDataset.addSeries(mCurrentSeries);
        mCurrentRenderer = new XYSeriesRenderer();
        mRenderer.addSeriesRenderer(mCurrentRenderer);
        mRenderer.setShowLabels(true);
        mRenderer.setShowGrid(true);
        mRenderer.setGridColor(0xFF505050);
        mRenderer.setXLabels(20);
        mRenderer.setYLabels(10);
        mRenderer.setPointSize(4f);
        mRenderer.setLabelsTextSize(15);
        mCurrentRenderer.setColor(0xFF00FF00);
        mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
        mCurrentRenderer.setLineWidth(2);
        mCurrentRenderer.setFillPoints(true);
        mCurrentRenderer.setChartValuesTextSize(15);

    }

    private void addSampleData() {
        mCurrentSeries.add(1, 2);
        mCurrentSeries.add(2, 3);
        mCurrentSeries.add(3, 2);
        mCurrentSeries.add(4, 5);
        mCurrentSeries.add(5, 4);
        mCurrentSeries.add(10, 3);
        mCurrentSeries.add(11, 1);
        mCurrentSeries.add(12, -1);
        mCurrentSeries.add(13, -3);
        mCurrentSeries.add(14, -2);
        mCurrentSeries.add(15, 0);
        mCurrentSeries.add(16, 5);
        mCurrentSeries.add(17, 10);
        mCurrentSeries.add(18, 20);
        mCurrentSeries.add(19, 18);
        mCurrentSeries.add(20, 15);
        mCurrentSeries.add(25, 10);
    }

    @Override
    public void onResume() {
        super.onResume();
        LinearLayout layout = (LinearLayout) findViewById(R.id.sensorInfoChart);
        if (mChart == null) {
            initChart();
            addSampleData();
            //mChart = ChartFactory.getCubeLineChartView(this, mDataset, mRenderer, 0.2f);
            mChart = ChartFactory.getLineChartView(this, mDataset, mRenderer);
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }
    }
}