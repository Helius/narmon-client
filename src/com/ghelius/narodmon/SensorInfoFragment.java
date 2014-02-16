package com.ghelius.narodmon;


import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.achartengine.util.MathHelper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class SensorInfoFragment extends Fragment {

	private final String TAG = "narodmon-info";
	private int sensorId = -1;
	private ArrayList<Point> logData = new ArrayList<Point>();
	private Timer updateTimer;
	private int offset = 0;
	private LogPeriod period = LogPeriod.day;
	private SensorLogGetter logGetter;
	private LogPeriod oldPeriod;
	private TextView value;
    private int type;
    AlarmSensorTask task = null;

	private GraphicalView mChart;
	private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
	private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
	private TimeSeries timeSeries;
    private TimeSeries mHiLevel;
    private TimeSeries mLowLevel;
	private XYSeriesRenderer mCurrentRenderer;
    private XYSeriesRenderer mCurrentRendereHiLevel ;
    private XYSeriesRenderer mCurrentRendereLowLevel ;

	private SensorConfigChangeListener listener = null;


	interface SensorConfigChangeListener {
		void favoritesChanged();
		void alarmChanged();
	}

	public void setFavoritesChangeListener (SensorConfigChangeListener listener) {
		this.listener = listener;
	}




	ArrayList<Sensor> getSavedList () {
		final String fileName = "sensorList.obj";
		ArrayList<Sensor> sensorList = new ArrayList<Sensor>();
//		Log.d(TAG, "------restore list start-------");
		FileInputStream fis;
		try {
			fis = getActivity().getApplicationContext().openFileInput(fileName);
			ObjectInputStream is = new ObjectInputStream(fis);
			sensorList.addAll((ArrayList<Sensor>) is.readObject());
			is.close();
			fis.close();
			for (Sensor aSensorList : sensorList) aSensorList.value = "--";
//			Log.d(TAG,"------restored list end------- " + sensorList.size());
		} catch (Exception e) {
			Log.e(TAG, "Can't read sensorList: " + e.getMessage());
		}
		return sensorList;
	}

	@Override
	public void onCreate (Bundle savedInstanceState) {
		super.onCreate (savedInstanceState);
		initChart();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		logGetter = new SensorLogGetter();
		return inflater.inflate(R.layout.sensorinfo, null);
	}

	public void setId(int id) {
        Log.d(TAG,"set id: " + id);
		sensorId = id;
	}

	private void updateGraph() {
		Log.d(TAG,"updating graph...");
		getActivity().findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
		logGetter.getLog(sensorId, period, offset);
		String title = "";
		switch (period) {
			case day:
				if (offset == 0)
					title = getString(R.string.text_today);
				else
					title = String.valueOf(offset) + " " + getString(R.string.text_days_ago);
				break;
			case week:
				if (offset == 0)
					title = getString(R.string.text_this_week);
				else
					title = String.valueOf(offset) + " " + getString(R.string.text_weeks_ago);
				break;
			case month:
				if (offset == 0)
					title = getString(R.string.text_this_month);
				else
					title = String.valueOf(offset) + " " + getString(R.string.text_month_ago);
				break;
			case year:
				if (offset == 0)
					title = getString(R.string.text_this_year);
				else
					title = String.valueOf(offset) + " " + getString(R.string.text_yars_ago);
				break;
		}
		if (offset == 0) {

		}
		mRenderer.setXTitle(title);
	}

	public static String getTimeSince (Context context, Long time) {
		long diffTime = (System.currentTimeMillis() - time*1000)/1000;
		String agoText;
		if (diffTime < 60) {
			agoText = String.valueOf(diffTime) +" "+ context.getString(R.string.text_sec);
		} else if (diffTime/60 < 60) {
			agoText = String.valueOf(diffTime/60) +" "+ context.getString(R.string.text_min);
		} else if (diffTime/3600 < 24) {
			agoText = String.valueOf(diffTime/3600) +" "+ context.getString(R.string.text_hr);
		} else {
			agoText = String.valueOf(diffTime/(3600*24)) +" "+ context.getString(R.string.text_days);
		}
		return agoText;
	}


	private void initChart() {
		Log.d(TAG,"init chart");
        mHiLevel = new TimeSeries("hi level");
        mLowLevel = new TimeSeries("low level");
		timeSeries = new TimeSeries("value");
		while (mDataset.getSeriesCount() != 0) {
            Log.d(TAG,"removeSeries");
			mDataset.removeSeries(mDataset.getSeriesCount()-1);
        }
		mDataset.addSeries(timeSeries);
        mDataset.addSeries(mHiLevel);
        mDataset.addSeries(mLowLevel);
		mCurrentRenderer = new XYSeriesRenderer();
        mCurrentRendereHiLevel = new XYSeriesRenderer();
        mCurrentRendereLowLevel = new XYSeriesRenderer();

		mRenderer.removeAllRenderers();
		mRenderer.addSeriesRenderer(mCurrentRenderer);
        mRenderer.addSeriesRenderer(mCurrentRendereHiLevel);
        mRenderer.addSeriesRenderer(mCurrentRendereLowLevel);

		mRenderer.setShowLabels(true);
		mRenderer.setShowGrid(true);
		mRenderer.setGridColor(0xFF505050);

		mRenderer.setXTitle(getString(R.string.text_today));
		mRenderer.setYLabels(10);
		mRenderer.setPointSize(2f);
		mRenderer.setAxisTitleTextSize(20);
		mRenderer.setChartTitleTextSize(20);
		mRenderer.setLabelsTextSize(12);
		mRenderer.setLegendTextSize(12);
		mRenderer.setYLabelsPadding(-20);
		mRenderer.setXLabelsAlign(Paint.Align.CENTER);
		mRenderer.setXLabels(12);

        mRenderer.setLegendHeight(10);
//        mRenderer.setShowLabels(false);
        mRenderer.setShowLegend(false);


		mCurrentRenderer.setColor(0xFF00FF00);
		mCurrentRenderer.setPointStyle(PointStyle.CIRCLE);
		mCurrentRenderer.setFillPoints(true);
		mCurrentRenderer.setChartValuesTextSize(15);

        mCurrentRendereHiLevel.setColor(0xFFFF4040);
        mCurrentRendereHiLevel.setFillPoints(true);
        mCurrentRendereHiLevel.setChartValuesTextSize(18);
        mCurrentRendereHiLevel.setLineWidth(2);

        mCurrentRendereLowLevel.setColor(0xFF4040FF);
        mCurrentRendereLowLevel.setFillPoints(true);
        mCurrentRendereLowLevel.setChartValuesTextSize(18);
        mCurrentRendereLowLevel.setLineWidth(2);
	}

	private void addSampleData() {
        Log.d(TAG,"addSampleData");
		if ((period == LogPeriod.day) && (offset == 0) && logData.size()>1) {
			value.setText(String.valueOf(logData.get(logData.size()-1).value) + " " +
                    SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getUnitForType(type));
		}
		int max_gap = 1000*60;
		if (period == LogPeriod.day) {
			max_gap = 60*60; // hour
		} else if (period == LogPeriod.week) {
			max_gap = 100*60;
		} else if (period == LogPeriod.month) {
			max_gap = 24*60*60; //day
		}

		if (oldPeriod != period) { // period was change, we need to create new mChart with other date-time format
			Log.d(TAG,"recreate chart");
			LinearLayout layout = (LinearLayout) getActivity().findViewById(R.id.sensorInfoChart);
			layout.removeAllViews();
			if (period == LogPeriod.day) {
				mChart = ChartFactory.getTimeChartView(getActivity().getApplicationContext(), mDataset, mRenderer, "H:mm");
			} else if (period == LogPeriod.week) {
				mChart = ChartFactory.getTimeChartView(getActivity().getApplicationContext(), mDataset, mRenderer, "E");
			} else if (period == LogPeriod.month) {
				mChart = ChartFactory.getTimeChartView(getActivity().getApplicationContext(), mDataset, mRenderer, "d");
			} else if (period == LogPeriod.year) {
				mChart = ChartFactory.getTimeChartView(getActivity().getApplicationContext(), mDataset, mRenderer, "M.d");
			}
			oldPeriod = period;
			layout.addView(mChart);
		}
		timeSeries.clear();
        mHiLevel.clear();
        mLowLevel.clear();
		if (!logData.isEmpty()) {
            Log.d(TAG,"logData isn't empty: " + logData.size());
			long prevTime = logData.get(0).time;
			float max = logData.get(0).value;
			float min = logData.get(0).value;
            float summ = 0;
            if (task != null && logData.size()>1) {
                Log.d(TAG,"task not null");
                if (task.job != AlarmSensorTask.NOTHING) {
                    Log.d(TAG,"add hi level");
                    mHiLevel.add((logData.get(0).time-1)*1000, task.hi);
                    mHiLevel.add((logData.get(logData.size()-1).time-1)*1000, task.hi);
                }
                if (task.job == AlarmSensorTask.OUT_OF || task.job == AlarmSensorTask.WITHIN_OF) {
                    Log.d(TAG,"add low level");
                    mLowLevel.add((logData.get(0).time-1)*1000, task.lo);
                    mLowLevel.add((logData.get(logData.size()-1).time-1)*1000, task.lo);
                }
            }
			for (Point data : logData) {
                summ +=data.value;
				if (data.value > max) max = data.value;
				if (data.value < min) min = data.value;
				timeSeries.add((data.time * 1000), data.value);
//                Log.d(TAG,"cur:"+data.time + " prev:" + prevTime + " diff:" + (data.time-prevTime));
				if ((data.time - prevTime) > max_gap) {
					timeSeries.add(((data.time - 1) * 1000), MathHelper.NULL_VALUE);
				}
				prevTime = data.time;
			}
			mRenderer.initAxesRange(1);
            TextView seriesInfo = (TextView) getView().findViewById(R.id.series_info);
            if (seriesInfo != null) {
                seriesInfo.setText("max: " + max + "\navg: " + String.format("%.2f%n", summ/logData.size())+ "min: " + min );
            }
            if (task!=null && task.job != AlarmSensorTask.NOTHING) {
                min = task.lo < min ? task.lo : min;
                max = task.hi > max ? task.hi : max;
            }
			mRenderer.setYAxisMin(min-(max-min)/10);
			mRenderer.setYAxisMax(max+(max-min)/10);

		} else {
            Log.e(TAG,"logData is empty: " + logData.size());
        }
        Log.d(TAG,"repaint");
		mChart.repaint();
		getActivity().findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
	}

	@Override
	public void onResume() {
		Log.d(TAG,"onResume");
		super.onResume();
        loadInfo();
	}

    public void loadInfo () {
        if (getActivity()==null)
            return;
        if (getView()==null)
            return;
        period = LogPeriod.day;
        offset = 0;

        Sensor sensor = null;
        Log.d(TAG, "id is " + sensorId);
        ArrayList<Sensor> sList = getSavedList();
        for (Sensor s : sList) {
            if (s.id == sensorId) {
                sensor = s;
            }
        }
        if (sensor == null) {
            Log.e(TAG,"sensor" + sensorId + "not found");
            return; // TODO: hide fragment or report 'sensor not found'
        }

        type = sensor.type;
        ((TextView) getView().findViewById(R.id.text_name)).setText(sensor.name);
        ((TextView) getView().findViewById(R.id.text_location)).setText(sensor.location);
        ((TextView) getView().findViewById(R.id.text_distance)).setText(String.valueOf(sensor.distance));

        ((TextView) getView().findViewById(R.id.text_type)).setText(SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getNameForType(sensor.type));
//		((TextView) getView().findViewById(R.id.text_id)).setText(sensor.id);
        ((ImageView) getView().findViewById(R.id.info_sensor_icon)).setImageDrawable(SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getIcon(sensor.type));
        value = (TextView) getView().findViewById(R.id.text_value);

        TextView time = (TextView) getView().findViewById(R.id.text_time);

        long dv = sensor.time *1000;// its need to be in millisecond
        Date df = new java.util.Date(dv);
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getDefault());
        String vv = sdf.format(df);
        time.setText(vv);

        TextView ago = (TextView) getView().findViewById(R.id.text_ago);
        ago.setText(getTimeSince(getActivity().getApplicationContext(), sensor.time));


        getActivity().findViewById(R.id.bt_graph_prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                offset += 1;
                updateGraph();
            }
        });
        getActivity().findViewById(R.id.bt_graph_day).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                period = LogPeriod.day;
                offset = 0;
                updateGraph();
            }
        });
        getActivity().findViewById(R.id.bt_graph_week).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                period = LogPeriod.week;
                offset = 0;
                updateGraph();
            }
        });
        getActivity().findViewById(R.id.bt_graph_month).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                period = LogPeriod.month;
                offset = 0;
                updateGraph();
            }
        });
        getActivity().findViewById(R.id.bt_graph_next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (offset > 0) {
                    offset -= 1;
                    updateGraph();
                }
            }
        });


        final ImageButton monitor = (ImageButton) getActivity().findViewById(R.id.addMonitoring);


        ArrayList<Integer> favorites = DatabaseManager.getInstance().getFavorites();
        if (favorites.contains(sensorId)) { // we are favorite!
            monitor.setImageResource(R.drawable.btn_star_big_on);
        } else {
            monitor.setImageResource(R.drawable.btn_star_big_off);
        }
        monitor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG,"monitoring onClick");
                ArrayList<Integer> favorites = DatabaseManager.getInstance().getFavorites();
                if (favorites.contains(sensorId)) { // we are favorite!
                    // remove us
                    DatabaseManager.getInstance().removeFavorites(sensorId);
                    monitor.setImageResource(R.drawable.btn_star_big_off);
                } else {
                    // add us
                    DatabaseManager.getInstance().addFavorites(sensorId);
                    monitor.setImageResource(R.drawable.btn_star_big_on);
                }
                if (listener != null)
                    listener.favoritesChanged();
            }
        });

        final ImageButton alarm = (ImageButton) getActivity().findViewById(R.id.alarmSetup);
        final AlarmsSetupDialog dialog = new AlarmsSetupDialog();
        dialog.setOnAlarmChangeListener(new AlarmsSetupDialog.AlarmChangeListener() {
            @Override
            public void onAlarmChange(AlarmSensorTask task_) {
                task = task_;
                Log.d(TAG, task.toString());
                if (task.job == AlarmSensorTask.NOTHING) {
                    ((ImageButton) getActivity().findViewById(R.id.alarmSetup)).setImageResource(R.drawable.alarm_gray);
                } else {
                    ((ImageButton) getActivity().findViewById(R.id.alarmSetup)).setImageResource(R.drawable.alarm_blue);
                }
                try {
                    task.lastValue = Float.valueOf(value.getText().toString());
                } catch (Exception e) {
                    task.lastValue = -999;
                }
                if (task.job == AlarmSensorTask.NOTHING)
                    DatabaseManager.getInstance().removeAlarm(task.id);
                else
                    DatabaseManager.getInstance().addAlarmTask(task);
                if (listener!=null)
                    listener.alarmChanged();

                addSampleData();
            }
        });
        final Sensor s = sensor;

        task = DatabaseManager.getInstance().getAlarmById(s.id);
        if (task == null || task.job == AlarmSensorTask.NOTHING) {
            ((ImageButton) getActivity().findViewById(R.id.alarmSetup)).setImageResource(R.drawable.alarm_gray);
        } else {
            ((ImageButton) getActivity().findViewById(R.id.alarmSetup)).setImageResource(R.drawable.alarm_blue);
        }
        alarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.setCurrentValue(String.valueOf(value.getText()));

                AlarmSensorTask task = DatabaseManager.getInstance().getAlarmById(s.id);
                if (task != null) {
                    Log.d(TAG, "Found: SensorTask with job " + task.job);
                } else {
                    Log.e(TAG, "sensorTask not found, create empty");
                    Float val;
                    try {
                        val = Float.valueOf(String.valueOf(value.getText()));
                    } catch (Exception e) {
                        Log.e(TAG,"sensor value not valid");
                        val = 0f;
                    }

                    task = new AlarmSensorTask(sensorId, 0, 0f, 0f, val, s.name);
                }
                dialog.setSensorTask(task);
                dialog.show(getActivity().getSupportFragmentManager(), "alarmDialog");
            }
        });

        oldPeriod = LogPeriod.year; // for create chart (in updateFilter)
        startTimer();
    }



	@Override
	public void onPause () {
		Log.d(TAG,"onPause");
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
			getter.execute(getActivity().getApplicationContext().getString(R.string.api_url), PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).getString("apiHeader", "") + "\"cmd\":\"sensorLog\"," +
					"\"id\":\""+id+"\",\"period\":\"" + sPeriod + "\",\"offset\":\""+ offset +"\"}");
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
            if (getView()!=null)
			    addSampleData();
			getter = null;
		}

		@Override
		public void onNoResult() {
			Log.e(TAG, "getLog: no data");
            Toast.makeText(getActivity().getApplicationContext(), "Server not responds, try later", Toast.LENGTH_SHORT).show();
            getActivity().findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
        }
	}


	final Handler timerHandler = new Handler(new Handler.Callback() {
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
				timerHandler.sendEmptyMessage(0);
			}
		}, 0, 60000 * Integer.valueOf(PreferenceManager.
				getDefaultSharedPreferences(getActivity().getApplicationContext()).getString(getString(R.string.pref_key_interval), "5")));
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
