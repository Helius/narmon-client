package com.ghelius.narodmon;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.*;

import com.androidplot.ui.AnchorPosition;
import com.androidplot.ui.PositionMetrics;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.XLayoutStyle;
import com.androidplot.ui.YLayoutStyle;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYSeries;
import com.androidplot.xy.*;
import com.androidplot.ui.SizeLayoutType;
import android.graphics.*;
import java.text.*;
import java.util.Date;

public class SensorInfoFragment extends Fragment implements MultitouchPlot.ZoomListener {

	private final String TAG = "narodmon-info";
	private int sensorId = -1;
    private Sensor sensor = null;
	private ArrayList<Point> logData = new ArrayList<Point>();
	private Timer updateTimer;
	private int offset = 0;
	private LogPeriod period = LogPeriod.day;
	private SensorLogGetter logGetter;
	private TextView sensorValueUnitText;
    private float currentValue;
    private int type;
    AlarmSensorTask task = null;
    Menu menu = null;
    AlarmsSetupDialog dialog = null;

    TextView seriesInfo;
    private MultitouchPlot plot = null;
    LineAndPointFormatter formatter;

	private SensorConfigChangeListener listener = null;

    private void setMenuIcon (int itemId, int drawableId) {
        if (menu == null) {
            return;
        }
        MenuItem i = menu.findItem(itemId);
        if (i != null)
            i.setIcon(drawableId);
        else
            Log.e(TAG,"menuItem is null");
    }

    public void setSensor(Sensor tmpS) {
        sensor = tmpS;
    }

    @Override
    public void zoommed(Number minX, Number maxX) {
        long diff = maxX.longValue() - minX.longValue();
        Log.d(TAG, "time lenght is: " + String.valueOf(diff));
        if (diff < 3*24*3600) {
            period = LogPeriod.day;
        } else if (diff > 3*24*3600 && diff < 12*24*3600) {
            period = LogPeriod.week;
        } else if (diff > 12*24*3600) {
            period = LogPeriod.month;
        }
    }


    interface SensorConfigChangeListener {
		void favoritesChanged();
		void alarmChanged();
	}

	public void setConfigChangeListener(SensorConfigChangeListener listener) {
		this.listener = listener;
	}

    @Override
    public void onDetach () {
        super.onDetach();
        Log.d(TAG, "onDetach");
        this.listener = null;
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
		super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setHasOptionsMenu(true);
        setRetainInstance(true);
	}


    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.sensor_info_menu, menu);
        this.menu = menu;
        super.onCreateOptionsMenu(menu, inflater);
        updateMenuIcons();
    }



	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView");
		logGetter = new SensorLogGetter();
		View v = inflater.inflate(R.layout.sensorinfo, null);
        plot = (MultitouchPlot) v.findViewById(R.id.plot1);
        plot.setZoomListener(this);
        seriesInfo = (TextView) v.findViewById(R.id.series_info);
        return v;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action buttons
        switch (item.getItemId()) {
            case R.id.menu_alarm:
                Log.d(TAG,"alarm menu pressed...");
                showAlarmSetupDialog();
                break;
            case R.id.menu_favorites:
                Log.d(TAG,"favorites menu pressed...");
                ArrayList<Integer> favorites = DatabaseManager.getInstance().getFavoritesId();
                if (favorites.contains(sensorId))
                    DatabaseManager.getInstance().removeFavorites(sensorId);
                else
                    DatabaseManager.getInstance().addFavorites(sensor.id, sensor.deviceId);
                updateMenuIcons();
                if (listener != null)
                    listener.favoritesChanged();
                break;
            default:

        }
        return super.onOptionsItemSelected(item);
    }

	public void setId(int id) {
        Log.d(TAG,"set id: " + id);
		sensorId = id;
	}

	private void updateGraph() {
		Log.d(TAG,"updating graph...");
        if (getActivity() == null)
            return;
		getActivity().findViewById(R.id.marker_progress).setVisibility(View.VISIBLE);
		logGetter.getLog(sensorId, period, offset);
		if (offset == 0) {

		}
	}

    private String getTitle() {

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
        return title;
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

    private float dpToFloat (int dp) {
        Resources r = getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    }

	private void initChart() {
//        if (plot == null || plot.getGraphWidget() == null) {
//            Log.e(TAG,"plot is null!");
//            return;
//        }
        if (plot.getGraphWidget().getGridBackgroundPaint() != null) {
            plot.getGraphWidget().getGridBackgroundPaint().setColor(Color.BLACK);
        }
        plot.getGraphWidget().getDomainGridLinePaint().setColor(Color.GRAY);
        plot.getGraphWidget().getDomainGridLinePaint().
                setPathEffect(new DashPathEffect(new float[]{1, 1}, 1));
        plot.getGraphWidget().getRangeGridLinePaint().setColor(Color.GRAY);
        plot.getGraphWidget().getRangeGridLinePaint().
                setPathEffect(new DashPathEffect(new float[]{1, 1}, 1));
        plot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.GRAY);
        plot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.GRAY);

        plot.getGraphWidget().getDomainLabelPaint().setTextSize(dpToFloat(10));
        plot.getGraphWidget().getRangeLabelPaint().setTextSize(dpToFloat(10));
        plot.getGraphWidget().getRangeLabelPaint().setTextAlign(Paint.Align.LEFT);
        Paint p = new Paint();
        p.setTextSize(dpToFloat(12));
        p.setColor(Color.WHITE);
        plot.getLegendWidget().setTextPaint(p);

//        plot.getLegendWidget().setHeight(dpToFloat(30));
//        plot.getGraphWidget().setDomainLabelWidth(-dpToFloat(50));

        plot.getGraphWidget().setDrawMarkersEnabled(true);

        // setup our line fill paint to be a slightly transparent gradient:
        Paint lineFill = new Paint();
        lineFill.setAlpha(0);

        formatter = new LineAndPointFormatter(Color.GREEN, Color.GREEN, Color.BLACK, null);
        formatter.setFillPaint(lineFill);


//        plot.getGraphWidget().setSize(new SizeMetrics(
//                dpToFloat(100), SizeLayoutType.FILL,
//                -dpToFloat(0), SizeLayoutType.FILL));
        plot.getGraphWidget().setRangeLabelHorizontalOffset(-dpToFloat(30));
        plot.getGraphWidget().setMarginBottom(dpToFloat(15));
        plot.getGraphWidget().setMarginLeft(dpToFloat(-35));
        plot.getLegendWidget().setMarginBottom(dpToFloat(0));
        plot.getLegendWidget().setHeight(dpToFloat(16));

        /* customisation */
        //http://stackoverflow.com/questions/13761455/androidplot-remove-domain-values-from-graphwidget

//        if (!mBackgroundOn) {
            // remove the background stuff.
            plot.setBackgroundPaint(null);
            plot.getGraphWidget().setBackgroundPaint(null);
            plot.getGraphWidget().setGridBackgroundPaint(null);
//        }
//        if (!mKeyOn)
//            mDynamicPlot.getLayoutManager()
//                    .remove(mDynamicPlot.getLegendWidget());
//        if (!mDomainLabelOn)
//            mDynamicPlot.getLayoutManager().remove(
//                    mDynamicPlot.getDomainLabelWidget());
//        if (!mDomainAxisOn) {
//            mDynamicPlot.getGraphWidget().setDomainLabelPaint(null);
//            mDynamicPlot.getGraphWidget().setDomainOriginLabelPaint(null);
//        }
//        if (!mBoarderOn){
//            //mDynamicPlot.setDrawBorderEnabled(false);
            plot.setBorderPaint(null);
//        }if (!mRangeLabelOn)
//            mDynamicPlot.getLayoutManager().remove(
//                    mDynamicPlot.getRangeLabelWidget());
//        if (!mRangeAxisOn) {
//            mDynamicPlot.getGraphWidget().setRangeLabelPaint(null);
//            mDynamicPlot.getGraphWidget().setRangeOriginLabelPaint(null);
//            //mDynamicPlot.getGraphWidget().setRangeLabelVerticalOffset(rangeLabelVerticalOffset);
//        }
//        if (!mGridOn) {
//            //mDynamicPlot.getGraphWidget().setGridLinePaint(null);
//            mDynamicPlot.getGraphWidget().setDomainOriginLinePaint(null);
//            mDynamicPlot.getGraphWidget().setRangeOriginLinePaint(null);
//        }
//        if (!mTitleOn) {
//            mDynamicPlot.getLayoutManager().remove(mDynamicPlot.getTitleWidget());
//        }
        // by default, AndroidPlot displays developer guides to aid in laying out your plot.
        // To get rid of them call disableAllMarkup():
        plot.setMarkupEnabled(false);


        plot.setDomainValueFormat(new Format() {

            // create a simple date format that draws on the year portion of our timestamp.
            // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
            // for a full description of SimpleDateFormat.
            private SimpleDateFormat daysDateFormat = new SimpleDateFormat("HH:mm");
            private SimpleDateFormat weekDateFormat = new SimpleDateFormat("E");
            private SimpleDateFormat monsDateFormat = new SimpleDateFormat("d,E");

            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {

                // because our timestamps are in seconds and SimpleDateFormat expects milliseconds
                // we multiply our timestamp by 1000:
                long timestamp = ((Number) obj).longValue() * 1000;
                Date date = new Date(timestamp);
                switch (period) {
                    case day:
                        return daysDateFormat.format(date, toAppendTo, pos);
                    case week:
                        return monsDateFormat.format(date, toAppendTo, pos);
                    case month:
                        return monsDateFormat.format(date, toAppendTo, pos);
                }
                return daysDateFormat.format(date, toAppendTo, pos);
            }

            @Override
            public Object parseObject(String source, ParsePosition pos) {
                return null;

            }
        });

        plot.setVisibility(View.VISIBLE);
	}

	private void addSampleData() {
        Log.d(TAG,"addSampleData");
		if ((period == LogPeriod.day) && (offset == 0) && logData.size()>=1) {
            currentValue = logData.get(logData.size() - 1).value;
			sensorValueUnitText.setText(String.valueOf(currentValue) + " " +
                    SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getUnitForType(type));
            updateTime(logData.get(logData.size()-1).time);
		}
		int max_gap = 1000*60;
		if (period == LogPeriod.day) {
			max_gap = 60*60; // hour
		} else if (period == LogPeriod.week) {
			max_gap = 100*60;
		} else if (period == LogPeriod.month) {
			max_gap = 24*60*60; //day
		}

        float summ = 0;
        Point max = new Point(Long.valueOf(0), Float.valueOf(0));
        Point min = new Point(Long.valueOf(0), Float.valueOf(0));
        long prevTime = -1;
        ArrayList<Number> values = new ArrayList<Number>();
        ArrayList<Number> times = new ArrayList<Number>();

        for (Point data : logData) {
            summ += data.value;
            if (max.time == 0 || data.value > max.value) {
                max.value = data.value;
                max.time = data.time;
            }
            if (min.time == 0 || data.value < min.value) {
                min.value = data.value;
                min.time = data.time;
            }
            if ((data.time - prevTime) > max_gap) {
                values.add(null);
                times.add(data.time-1);
            } else {
                values.add(data.value);
                times.add(data.time);
            }

            prevTime = data.time;
        }
        XYSeries series = new SimpleXYSeries(
                times,
                values,
                getTitle());
        plot.clear();


        plot.removeMarkers();
        if (task!=null && task.job != AlarmSensorTask.NOTHING) {
            if (task.job != AlarmSensorTask.LESS_THAN) {
                Paint phi = new Paint();
                phi.setColor(Color.RED);
                YValueMarker mhi = new YValueMarker(task.hi, " ");
                mhi.setLinePaint(phi);
                mhi.setTextPaint(phi);
                plot.addMarker(mhi);
            }
            if (task.job != AlarmSensorTask.MORE_THAN) {
                Paint plo = new Paint();
                plo.setColor(Color.BLUE);
                YValueMarker mlo = new YValueMarker(task.lo, " ");
                mlo.setLinePaint(plo);
                mlo.setTextPaint(plo);
                plot.addMarker(mlo);
            }
        }


//        if (max.value - min.value > 1000)
//            plot.setRangeValueFormat(new DecimalFormat("####"));
        //else
        if (max.value - min.value > 100)
            plot.setRangeValueFormat(new DecimalFormat("###"));
        else if (max.value- min.value > 10)
            plot.setRangeValueFormat(new DecimalFormat("##.#"));

//        plot.getGraphWidget().setDomainCursorPosition(max.time);
//        plot.getGraphWidget().setRangeCursorPosition(max.value-10);
//        plot.setCursorPosition(min.time, min.value);

        plot.clearBoundaryValue();
        plot.addSeries(series, formatter);
        plot.setRangeStep(XYStepMode.SUBDIVIDE, 10);
        plot.redraw();

		getActivity().findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
        if (seriesInfo != null)
            seriesInfo.setText("max: " + max.value + "\navg: " + String.format("%.2f%n", summ/logData.size())+ "min: " + min.value );
	}

	@Override
	public void onResume() {
		Log.d(TAG,"onResume");
		super.onResume();
        plot.setVisibility(View.INVISIBLE);
        loadInfo();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                initChart();
            }
        }, 50);
	}

    private void updateMenuIcons () {
        // init menu icon
        ArrayList<Integer> favorites = DatabaseManager.getInstance().getFavoritesId();
        if (favorites.contains(sensorId)) { // we are favorite!
            setMenuIcon(R.id.menu_favorites, R.drawable.ic_action_important);
        } else {
            setMenuIcon(R.id.menu_favorites, R.drawable.ic_action_not_important);
        }
        if (sensor != null) {
            task = DatabaseManager.getInstance().getAlarmById(sensor.id);
            if (task == null || task.job == AlarmSensorTask.NOTHING) {
                setMenuIcon(R.id.menu_alarm, R.drawable.ic_bell);
            } else {
                setMenuIcon(R.id.menu_alarm, R.drawable.ic_bell_fill);
            }
        }
    }

    public void loadInfo () {
        if (getActivity()==null) {
            Log.e(TAG,"loadInfo: getActivity return null");
            return;
        }
        if (getView()==null) {
            Log.e(TAG,"loadInfo: getView return null");
            return;
        }
        period = LogPeriod.day;
        offset = 0;
//        if (sensor == null)
//        Sensor sensor = null;
        Log.d(TAG, "id is " + sensorId);
        if (sensor == null) {
            ArrayList<Sensor> sList = getSavedList();
            for (Sensor s : sList) {
                if (s.id == sensorId) {
                    sensor = s;
                }
            }
        }
        if (sensor == null) {
            Log.e(TAG, "sensor" + sensorId + "not found");
            return; // TODO: hide fragment or report 'sensor not found'
        } else {
            sensorId = sensor.id;
        }
        Log.d(TAG,"sensor id: " + sensor.id + ", devices id: " + sensor.deviceId);

        type = sensor.type;
        ((TextView) getView().findViewById(R.id.text_name)).setText(sensor.name);
        ((TextView) getView().findViewById(R.id.text_location)).setText(sensor.location);
        ((TextView) getView().findViewById(R.id.text_distance)).setText(String.valueOf(sensor.distance));

        ((TextView) getView().findViewById(R.id.text_type)).setText(SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getNameForType(sensor.type));
        ((ImageView) getView().findViewById(R.id.info_sensor_icon)).setImageDrawable(SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getIcon(sensor.type));
        sensorValueUnitText = (TextView) getView().findViewById(R.id.text_value);
        sensorValueUnitText.setText(String.valueOf(sensor.value) + " " +
                SensorTypeProvider.getInstance(getActivity().getApplicationContext()).getUnitForType(type));

        updateTime(sensor.time);

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


        dialog = new AlarmsSetupDialog();
        dialog.setOnAlarmChangeListener(new AlarmsSetupDialog.AlarmChangeListener() {
            @Override
            public void onAlarmChange(AlarmSensorTask task_) {
                task = task_;
                Log.d(TAG, task.toString());
                task.lastValue = currentValue;
                //task.deviceId = sensor.deviceId;
                if (task.job == AlarmSensorTask.NOTHING)
                    DatabaseManager.getInstance().removeAlarm(task.id);
                else
                    DatabaseManager.getInstance().addAlarmTask(task);
                if (listener!=null)
                    listener.alarmChanged();
                updateMenuIcons();
                addSampleData();
            }
        });

        updateMenuIcons();
        startTimer();
    }

    private void showAlarmSetupDialog () {
        dialog.setCurrentValue(currentValue);

        AlarmSensorTask task = DatabaseManager.getInstance().getAlarmById(sensor.id);
        if (task != null) {
            Log.d(TAG, "Found: SensorTask " + task);
        } else {
            task = new AlarmSensorTask(sensorId, sensor.deviceId, 0, 0f, 0f, currentValue, sensor.name);
            Log.e(TAG, "sensorTask not found, create empty:" + task.toString());
//                    DatabaseManager.getInstance().addAlarmTask(task);
        }
        dialog.setSensorTask(task);
        dialog.show(getActivity().getSupportFragmentManager(), "alarmDialog");
    }

    private void updateTime (long timeStamp) {

        TextView time = (TextView) getView().findViewById(R.id.text_time);
        if (time != null) {
            long dv = timeStamp * 1000;// its need to be in millisecond
            Date df = new java.util.Date(dv);
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getDefault());
            String vv = sdf.format(df);
            time.setText(vv);
        }

        TextView ago = (TextView) getView().findViewById(R.id.text_ago);
        ago.setText(getTimeSince(getActivity().getApplicationContext(), timeStamp));
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
            if (getActivity() != null) {
                Toast.makeText(getActivity().getApplicationContext(), "Server not responds, try later", Toast.LENGTH_SHORT).show();
                getActivity().findViewById(R.id.marker_progress).setVisibility(View.INVISIBLE);
            }
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
