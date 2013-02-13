package com.ghelius.narodmon;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;


public class SensorInfo extends Activity {
    private final String TAG = "narodmon-info";
    private final String apiUrl = "http://narodmon.ru/client.php?json=";
    private ArrayList <Point> logData = new ArrayList<Point>();
    private Timer updateTimer;
    private int offset = 0;
    private LogPeriod period = LogPeriod.day;
    private SensorLogGetter logGetter;
    private int id;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensorinfo);
        final AlarmsSetupDialog dialog = new AlarmsSetupDialog();
        Bundle extras = getIntent().getExtras();
        if (extras == null)
            finish();

        logGetter = new SensorLogGetter();

        final Sensor sensor = (Sensor)extras.getSerializable("Sensor");
        id = sensor.id;
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
        for (int i = 0; i < logData.size(); i++) {
            mCurrentSeries.add(i, logData.get(i).value);
        }
        if (logData.size() > 20) {
            mCurrentRenderer.setPointStyle(PointStyle.POINT);
            mRenderer.setPointSize(1f);
        } else {
            mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
            mRenderer.setPointSize(4f);
        }
        mChart.repaint();
    }

    @Override
    public void onResume() {
        super.onResume();
        LinearLayout layout = (LinearLayout) findViewById(R.id.sensorInfoChart);
        if (mChart == null) {
            initChart();
            mChart = ChartFactory.getLineChartView(this, mDataset, mRenderer);
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }
        startTimer();
    }

    @Override
    public void onPause () {
        super.onPause();
        stopTimer();
    }

    enum LogPeriod {day,week,month,year}
    private class SensorLogGetter implements ServerDataGetter.OnResultListener {
        ServerDataGetter getter;
        void getLog (int id, LogPeriod period, int offset) {
            getter = new ServerDataGetter();
            getter.setOnListChangeListener(this);
            String sPeriod = "";
            switch (period) {
                case day:
                    sPeriod = "day";
                    break;
                case week:
                    sPeriod = "week";
                    break;
                case month:
                    sPeriod = "month";
                    break;
                default:
                    break;
            }
            getter.execute(apiUrl+"{\"cmd\":\"sensorLog\",\"uuid\":\"" + ConfigHolder.getInstance(SensorInfo.this).getUid() +
                    "\",\"id\":\""+id+"\",\"period\":\"" + sPeriod + "\",\"offset\":\""+ offset +"\"}");
        }

        @Override
        public void onResultReceived(String result) {
            logData.clear();
            Log.d(TAG,"sensorLog received: " + result);
            try {
                JSONObject jsonObject = new JSONObject(result);
                JSONArray arr = jsonObject.getJSONArray("data");
                for (int i = 0; i < arr.length(); i++) {
                    logData.add(new Point(Long.valueOf(arr.getJSONObject(i).getString("time")),
                            Float.valueOf(arr.getJSONObject(i).getString("value"))));
                }
            } catch (JSONException e) {
                Log.e(TAG,"SensorLog: Wrong JSON " + e.getMessage());
            }
            // now we have full data, paint graph
            Log.d(TAG,"add log data to graph, items count: " + logData.size());
            addSampleData();
        }

        @Override
        public void onNoResult() {
            Log.e(TAG,"getLog: no data");
        }
    }


    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            logGetter.getLog(id, period, offset);
            return false;
        }
    });

    void startTimer () {
        stopTimer();
        updateTimer = new Timer("updateTimer",true);
        updateTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                h.sendEmptyMessage(0);
            }
        }, 0, 60000*Integer.valueOf(PreferenceManager.
                getDefaultSharedPreferences(this).
                getString(getString(R.string.pref_key_interval),"5")));
    }
    void stopTimer () {
        if (updateTimer != null) {
            updateTimer.cancel();
            updateTimer.purge();
            updateTimer = null;
        }
    }
    class Point {
        public long time;
        public float value;

        public Point(Long time, Float value) {
            this.time = time;
            this.value = value;
        }
    }
}