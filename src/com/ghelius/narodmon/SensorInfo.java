package com.ghelius.narodmon;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;


public class SensorInfo extends FragmentActivity {
    private final String TAG = "narodmon-info";
    private ArrayList <Point> logData = new ArrayList<Point>();
    private Timer updateTimer;
    private int offset = 0;
    private LogPeriod period = LogPeriod.day;
    private SensorLogGetter logGetter;
    private int id;
    private LogPeriod oldPeriod;

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
        distance.setText(String.valueOf(sensor.distance));

        final ImageButton monitor = (ImageButton) findViewById(R.id.addMonitoring);
        final ImageButton alarm = (ImageButton) findViewById(R.id.alarmSetup);
        final ConfigHolder config = ConfigHolder.getInstance(getApplicationContext());

        dialog.setOnAlarmChangeListener(new AlarmsSetupDialog.AlarmChangeListener() {
            @Override
            public void onAlarmChange(int job, Float hi, Float lo) {
                Log.d(TAG, "new alarm setup: " + job + " " + hi + " " + lo);
                ConfigHolder.getInstance(SensorInfo.this).setAlarm(sensor.id, job, hi, lo);
                if (job == 0) {
                    alarm.setImageResource(R.drawable.alarm_gray);
                    Toast.makeText(SensorInfo.this, getString(R.string.text_alarm_disabled), Toast.LENGTH_SHORT).show();
                } else {
                    alarm.setImageResource(R.drawable.alarm_blue);
                    Toast.makeText(SensorInfo.this, getString(R.string.text_alarm_enabled), Toast.LENGTH_SHORT).show();
                }
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
                LinearLayout l = ((LinearLayout)findViewById(R.id.value_layout));
                if (l != null)
                    l.setOrientation(LinearLayout.VERTICAL);
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
                dialog.show(getSupportFragmentManager(), "alarmDialog");
//                dialog.setShowsDialog(true);
                if (config.isSensorWatchJob(sensor.id)) {
                    alarm.setImageResource(R.drawable.alarm_blue);
                } else {
                    alarm.setImageResource(R.drawable.alarm_gray);
                }
            }
        });

        findViewById(R.id.bt_graph_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offset += 1;
                updateGraph();
            }
        });
        findViewById(R.id.bt_graph_day).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                period = LogPeriod.day;
                offset = 0;
                updateGraph();
            }
        });
        findViewById(R.id.bt_graph_week).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                period = LogPeriod.week;
                offset = 0;
                updateGraph();
            }
        });
        findViewById(R.id.bt_graph_month).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                period = LogPeriod.month;
                offset = 0;
                updateGraph();
            }
        });
        findViewById(R.id.bt_graph_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (offset > 0) {
                    offset -= 1;
                    updateGraph();
                }
            }
        });

    }

    private void updateGraph() {
        logGetter.getLog(id, period, offset);
        String title = "";
        switch (period) {
            case day:
                if (offset == 0)
                    title = "today";
                else
                    title = String.valueOf(offset) + " days ago";
                break;
            case week:
                if (offset == 0)
                    title = "this week";
                else
                    title = String.valueOf(offset) + " weeks ago";
                break;
            case month:
                if (offset == 0)
                    title = "this month";
                else
                    title = String.valueOf(offset) + " months ago";
                break;
            case year:
                if (offset == 0)
                    title = "this year";
                else
                    title = String.valueOf(offset) + " years ago";
                break;
        }
        if (offset == 0) {

        }
        mRenderer.setXTitle(title);
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
    private TimeSeries timeSeries;
    private XYSeriesRenderer mCurrentRenderer;

    private void initChart() {
        timeSeries = new TimeSeries ("");
        mDataset.addSeries(timeSeries);
        mCurrentRenderer = new XYSeriesRenderer();
        mRenderer.addSeriesRenderer(mCurrentRenderer);
        mRenderer.setShowLabels(true);
        mRenderer.setShowGrid(true);
        mRenderer.setGridColor(0xFF505050);
        //mRenderer.setXLabels(24);

        mRenderer.setXTitle(getString(R.string.text_today));
        mRenderer.setYLabels(10);
        mRenderer.setPointSize(1f);
        mRenderer.setAxisTitleTextSize(20);
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setLabelsTextSize(15);
        mRenderer.setLegendTextSize(10);
        mRenderer.setYLabelsPadding(-20);
        mRenderer.setXLabelsAlign(Paint.Align.CENTER);
        //mRenderer.setShowCustomTextGrid(true);
        mRenderer.setXLabels(10);

        mCurrentRenderer.setColor(0xFF00FF00);
        mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
//        mCurrentRenderer.setLineWidth(-1);
        mCurrentRenderer.setFillPoints(true);
        mCurrentRenderer.setChartValuesTextSize(15);



    }

    private void addSampleData() {
    if (mChart == null) {
            LinearLayout layout = (LinearLayout) findViewById(R.id.sensorInfoChart);
            initChart();
            mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "H:mm");
            layout.addView(mChart);
            oldPeriod = period;
        }
        if (oldPeriod != period) { // period was change, we need to create new mChart with other date-time format
            LinearLayout layout = (LinearLayout) findViewById(R.id.sensorInfoChart);
            layout.removeAllViews();
            if (period == LogPeriod.day)
                mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "H:mm");
            else if (period == LogPeriod.week)
                mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "E");
            else if (period == LogPeriod.month)
                mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "d");
            else if (period == LogPeriod.year)
                mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer, "M.d");
            oldPeriod = period;
            layout.addView(mChart);
        }
        timeSeries.clear();
        for (Point data : logData) {
            timeSeries.add((data.time * 1000), data.value);
        }
        mRenderer.initAxesRange(1);
        mChart.repaint();
    }

    @Override
    public void onResume() {
        super.onResume();
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
            if (getter != null) {
                getter.cancel(true);
            }
            Log.d(TAG,"Getting log for id:" + id + " period:" + period.name() + " offset:" + offset);
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
            getter.execute(NarodmonApi.apiUrl, ConfigHolder.getInstance(getApplicationContext()).getApiHeader() + "\"cmd\":\"sensorLog\"," +
                    "\"id\":\""+id+"\",\"period\":\"" + sPeriod + "\",\"offset\":\""+ offset +"\"}");
        }

        @Override
        public void onResultReceived(String result) {
            logData.clear();
            //Log.d(TAG,"sensorLog received: " + result);
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
            getter = null;
        }

        @Override
        public void onNoResult() {
            Log.e(TAG, "getLog: no data");
        }
    }


    final Handler h = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            updateGraph();
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